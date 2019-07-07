package nl.tsakiris.hoohaha;

import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jdbi.v3.core.Jdbi;

public class FingerPrinter implements Runnable {

  @Getter
  @Setter
  private boolean running;

  @Getter
  private long processedBytes;

  @Getter
  private long processedFiles;

  private final OldScanner scanner;
  private final FingerPrintDao fingerPrintDao;

  public FingerPrinter(Configuration configuration, Path path) {
    this.scanner = new OldScanner(path);
    Jdbi jdbi = Jdbi.create(configuration.getJdbcUri());
    this.fingerPrintDao = jdbi.onDemand(FingerPrintDao.class);
  }

  @SneakyThrows
  @Override
  public void run() {
    Thread scannerThread = new Thread(scanner);
    scanner.setRunning(true);
    scannerThread.start();
    do {
      while (scanner.hasFiles()) {
        Path path = scanner.nextFile();
        System.out.println(path.getFileName());
        long size = Files.size(path);
        Fingerprint fingerPrint = Fingerprint.builder()
            .parentId(null)
            .path(path.getFileName().toString())
            .type(Files.isRegularFile(path) ? 'f' : 'd')
            .size(Files.size(path))
            .hash("hehehe")
            .build();
        processedBytes += size;
        processedFiles++;
      }
    } while (scanner.isRunning() || scanner.hasFiles());
    scannerThread.join();
    running = false;
  }

  public long totalBytes() {
    return scanner.getTotalBytes();
  }

  public long totalFiles() {
    return scanner.getTotalFiles();
  }

  public long totalDirectories() {
    return scanner.getTotalDirectories();
  }

}
