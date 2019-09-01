package nl.tsakiris.hoohaha;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

public class Scan {

  public static enum Strategy {
    FAST,
    CONTENT,
    FILENAME,
  }


  private final FingerPrintDao fingerPrintDao;
  private final Strategy strategy;

  @SneakyThrows
  public Scan(FingerPrintDao fingerPrintDao, Strategy strategy) {
    this.fingerPrintDao = fingerPrintDao;
    this.strategy = strategy;
  }

  @SneakyThrows
  public static void main(String[] args) {
    Class.forName("com.mysql.cj.jdbc.Driver");
    Configuration configuration = new ArgsConfiguration(args);
    Jdbi jdbi = Jdbi.create(configuration.getJdbcUri(), configuration.getJdbProperties());
    jdbi.installPlugin(new SqlObjectPlugin());
    FingerPrintDao fingerPrintDao = jdbi.onDemand(FingerPrintDao.class);
    fingerPrintDao.truncate();
    Scan scan = new Scan(fingerPrintDao, Strategy.FAST);
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
      totalSize += childFingerPrint.getSize();
    }
    String hash = DigestUtils.sha1Hex(hashes.stream().sorted().collect(Collectors.joining()));
    fingerPrint.setId(id);
    fingerPrint.setHash(hash);
    fingerPrint.setSize(totalSize);
    fingerPrintDao.update(fingerPrint);
    System.out.println();
    return fingerPrint;
  }

  @SneakyThrows
  private Fingerprint scanFile(Long parentId, Path path) {
    System.out.print(path);
    System.out.flush();
    String hash = strategy == Strategy.CONTENT
        ? DigestUtils.sha1Hex(new FileInputStream(path.toFile()))
        : DigestUtils.sha1Hex(path.getFileName().toString());
    Fingerprint fingerPrint = Fingerprint.builder()
        .parentId(parentId)
        .path(path.toString())
        .type('f')
        .size(Files.size(path))
        .hash(calculateHash(path))
        .build();
    fingerPrintDao.insert(fingerPrint);
    System.out.println();
    return fingerPrint;
  }

  @SneakyThrows
  private String calculateHash(Path path) {
    switch (strategy) {
      case CONTENT:
        return DigestUtils.sha1Hex(new FileInputStream(path.toFile()));
      case FILENAME:
        return DigestUtils.sha1Hex(path.getFileName().toString());
      default:
        return fastHash(path);
    }
  }

  @SneakyThrows
  private String fastHash(Path path) {
    long size = Files.size(path);
    if (size < 8_388_608) {
      return DigestUtils.sha1Hex(new FileInputStream(path.toFile()));
    }
    long skipBytes = (size - 8_388_608) / 127;
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    FileInputStream is = new FileInputStream((path.toFile()));
    byte[] chunk = new byte[65_536];
    for (int i = 0; i < 128; i++) {
      is.read(chunk);
      md.update(chunk);
      is.skip(skipBytes);
    }
    return Hex.encodeHexString(md.digest());
  }

}
