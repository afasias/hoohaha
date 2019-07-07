package nl.tsakiris.hoohaha;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

public class OldScanner implements Runnable {

  @Getter
  @Setter
  private boolean running;

  @Getter
  private long totalBytes;

  @Getter
  private long totalFiles;

  @Getter
  private long totalDirectories;

  private Path path;
  private ConcurrentLinkedQueue<Path> pendingFiles = new ConcurrentLinkedQueue<>();

  public OldScanner(Path path) {
    this.path = path;
  }

  @Override
  public void run() {
    scanDirectory(path);
    running = false;
  }

  @SneakyThrows
  private void scanDirectory(Path path) {
    totalDirectories++;
    Stream<Path> list = Files.list(path);
    list.forEach(x -> {
      if (Files.isRegularFile(x)) {
        scanFile(x);
      } else if (Files.isDirectory(x)) {
        scanDirectory(x);
      }
    });
    list.close();
  }

  @SneakyThrows
  private void scanFile(Path path) {
    totalFiles++;
    totalBytes += Files.size(path);
    pendingFiles.add(path);
  }

  public boolean hasFiles() {
    return !pendingFiles.isEmpty();
  }

  public Path nextFile() {
    return pendingFiles.poll();
  }

}
