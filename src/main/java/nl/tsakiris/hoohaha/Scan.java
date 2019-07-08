package nl.tsakiris.hoohaha;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

public class Scan {

  private final FingerPrintDao fingerPrintDao;

  @SneakyThrows
  public Scan(FingerPrintDao fingerPrintDao) {
    this.fingerPrintDao = fingerPrintDao;
  }

  @SneakyThrows
  public static void main(String[] args) {
    Class.forName("com.mysql.cj.jdbc.Driver");
    Configuration configuration = new ArgsConfiguration(args);
    Jdbi jdbi = Jdbi.create(configuration.getJdbcUri(), configuration.getJdbProperties());
    jdbi.installPlugin(new SqlObjectPlugin());
    FingerPrintDao fingerPrintDao = jdbi.onDemand(FingerPrintDao.class);
    fingerPrintDao.truncate();
    Scan scan = new Scan(fingerPrintDao);
    scan.run(Paths.get(args[3]));
  }

  public void run(Path path) {
    if (!Files.exists(path)) {
      System.err.println("No such file or directory");
      return;
    } else if (Files.isRegularFile(path)) {
      scanFile(null, path);
    } else {
      scanDirectory(null, path);
    }
  }

  @SneakyThrows
  private Fingerprint scanDirectory(Long parentId, Path path) {
    System.out.print(path);
    System.out.flush();
    Fingerprint fingerPrint = Fingerprint.builder()
        .type('d')
        .path(path.toString())
        .parentId(parentId)
        .hash("")
        .build();
    long id = fingerPrintDao.insert(fingerPrint);
    List<String> hashes = new ArrayList<>();
    List<String> hashes2 = new ArrayList<>();
    long totalSize = 0L;
    List<Path> list = Files.list(path).collect(Collectors.toList());
    for (Path childPath : list) {
      Fingerprint childFingerPrint =
          Files.isRegularFile(childPath) ? scanFile(id, childPath) : scanDirectory(id, childPath);
      hashes.add(childFingerPrint.getHash());
      hashes2.add(childFingerPrint.getHash2());
      totalSize += childFingerPrint.getSize();
    }
    String hash = DigestUtils.sha1Hex(hashes.stream().sorted().collect(Collectors.joining()));
    String hash2 = DigestUtils.sha1Hex(hashes2.stream().sorted().collect(Collectors.joining()));
    fingerPrint.setId(id);
    fingerPrint.setHash(hash);
    fingerPrint.setHash2(hash2);
    fingerPrint.setSize(totalSize);
    fingerPrintDao.update(fingerPrint);
    System.out.println();
    return fingerPrint;
  }

  @SneakyThrows
  private Fingerprint scanFile(Long parentId, Path path) {
    System.out.print(path);
    System.out.flush();
    String hash = DigestUtils.sha1Hex(new FileInputStream(path.toFile()));
    String hash2 = DigestUtils.sha1Hex(path.getFileName().toString());
    Fingerprint fingerPrint = Fingerprint.builder()
        .parentId(parentId)
        .path(path.toString())
        .type('f')
        .size(Files.size(path))
        .hash(hash)
        .hash2(hash2)
        .build();
    fingerPrintDao.insert(fingerPrint);
    System.out.println();
    return fingerPrint;
  }

}
