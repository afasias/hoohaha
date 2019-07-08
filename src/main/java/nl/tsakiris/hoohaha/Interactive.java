package nl.tsakiris.hoohaha;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

public class Interactive {

  private Long currentPathId = null;
  private String currentPath = null;
  private Scan.Strategy currentStrategy = Scan.Strategy.CONTENT;

  private final Scanner scanner = new Scanner(System.in);
  private final FingerPrintDao fingerPrintDao;

  private final Map<Long, Fingerprint> aliases = new HashMap<>();

  public Interactive(FingerPrintDao fingerPrintDao) {
    this.fingerPrintDao = fingerPrintDao;
  }

  @SneakyThrows
  public static void main(String[] args) {
    Class.forName("com.mysql.cj.jdbc.Driver");
    Configuration configuration = new ArgsConfiguration(args);
    Jdbi jdbi = Jdbi.create(configuration.getJdbcUri(), configuration.getJdbProperties());
    jdbi.installPlugin(new SqlObjectPlugin());
    jdbi.registerRowMapper(new FingerprintMapper());
    FingerPrintDao fingerPrintDao = jdbi.onDemand(FingerPrintDao.class);
    Interactive interactive = new Interactive(fingerPrintDao);
    interactive.run();
  }

  public void run() {

    main_loop:
    while (true) {
      System.out.print(Optional.ofNullable(currentPath).orElse("#") + "> ");
      System.out.flush();
      if (!scanner.hasNextLine()) {
        break;
      }
      String nextLine = scanner.nextLine();
      String[] args = nextLine.trim().split("\\s+");

      if (args[0].length() < 1) {
        continue;
      }

      boolean aliasesUsed = false;
      for (int i = 0; i < args.length; i++) {
        if (args[i].charAt(0) == '@') {
          long alias = Long.parseLong(args[i].substring(1));
          Fingerprint fingerprint = aliases.get(alias);
          if (fingerprint == null) {
            System.err.println("No such alias: @" + alias);
            continue main_loop;
          }
          args[i] = String.valueOf(fingerprint.getId());
          aliasesUsed = true;
        }
      }
      if (aliasesUsed) {
        System.out.print("Rewritten as:");
        for (String arg : args) {
          System.out.print(" " + arg);
        }
        System.out.println();
      }

      if (args[0].equals("ls")) {
        list();

      } else if (args[0].equals("alias")) {
        listAliases();

      } else if (args[0].equals("scan") && args.length > 1) {
        Scan scan = new Scan(fingerPrintDao, currentStrategy);
        scan.run(Paths.get(args[1]));

      } else if (args[0].equals("truncate")) {
        fingerPrintDao.truncate();

      } else if (args[0].equals("cd")) {
        changeDir(args.length > 1 ? args[1] : null);

      } else if (args[0].matches("^[0-9]+$")) {
        changeDir(args[0]);

      } else if (args[0].equals("rm") && args.length > 1) {
        for (int i = 1; i < args.length; i++) {
          delete(Long.parseLong(args[i]));
        }

      } else if (args[0].equals("top")) {
        topDuplicates(args.length > 1 ? Long.parseLong(args[1]) : 5);

      } else if (args[0].equals("setup_db")) {
        fingerPrintDao.dropTable();
        fingerPrintDao.createTable();

      } else if (args[0].equals("strategy")) {
        doStrategy(args);

      } else if (args[0].equals("exit") || args[0].equals("q")) {
        break;

      } else {
        System.err.println("Unknown command");
      }
    }
  }

  private void doStrategy(String[] args) {
    if (args.length > 1) {
      currentStrategy = Scan.Strategy.valueOf(args[1].toUpperCase());
    }
    System.out.println("Current strategy: " + currentStrategy);
  }

  private void listAliases() {
    List<Fingerprint> fingerPrints = currentPathId != null ?
        fingerPrintDao.getByParentId(currentPathId) :
        fingerPrintDao.getTopLevel();
    for (long alias : aliases.keySet()) {
      Fingerprint fingerprint = aliases.get(alias);
      System.out.format("%6d %4s %c %s %7s %s\n",
          fingerprint.getId(),
          "@" + alias,
          fingerprint.getType() == 'd' ? 'd' : '-',
          fingerprint.getHash(),
          humanReadableSize(fingerprint.getSize()),
          fingerprint.getPath());
    }
  }

  private void list() {
    List<Fingerprint> fingerPrints = currentPathId != null ?
        fingerPrintDao.getByParentId(currentPathId) :
        fingerPrintDao.getTopLevel();
    aliases.clear();
    for (Fingerprint fingerprint : fingerPrints) {
      long nextAlias = aliases.size() + 1;
      aliases.put(nextAlias, fingerprint);
      System.out.format("%6d %4s %c %s %7s %s\n",
          fingerprint.getId(),
          "@" + nextAlias,
          fingerprint.getType() == 'd' ? 'd' : '-',
          fingerprint.getHash(),
          humanReadableSize(fingerprint.getSize()),
          currentPath != null ?
              Paths.get(fingerprint.getPath()).getFileName() :
              fingerprint.getPath());
    }
  }

  private void changeDir(String destination) {
    if (destination != null) {
      if (destination.equals("..")) {
        if (currentPathId != null) {
          Fingerprint fingerprint = fingerPrintDao.getById(currentPathId);
          currentPathId = fingerprint.getParentId();
          if (currentPathId != null) {
            Fingerprint parentFingerprint = fingerPrintDao.getById(currentPathId);
            currentPath = parentFingerprint.getPath();
          } else {
            currentPath = null;
          }
        }
      } else {
        long id = Long.parseLong(destination);
        Fingerprint fingerprint = fingerPrintDao.getById(id);
        if (fingerprint == null || fingerprint.getType() != 'd') {
          System.err.println("No such directory");
        } else {
          currentPathId = fingerprint.getId();
          currentPath = fingerprint.getPath();
        }
      }
    } else {
      currentPathId = null;
      currentPath = null;
    }
  }

  private String humanReadableSize(long size) {
    if (size < 1024) {
      return Long.toString(size);
    } else if (size < 1024 * 1024) {
      return String.format("%1.1fK", (double) size / 1024);
    } else if (size < 1024 * 1024 * 1024) {
      return String.format("%1.1fM", (double) size / 1024 / 1024);
    } else {
      return String.format("%1.1fG", (double) size / 1024 / 1024 / 1024);
    }
  }

  @SneakyThrows
  private void delete(long id) {
    Fingerprint fingerprint = fingerPrintDao.getById(id);
    if (fingerprint == null) {
      System.err.println("No such file or directory");
    }

    System.out.print("Really delete " + fingerprint.getPath() + "? [y/n] ");
    System.out.flush();
    String confirmation = scanner.nextLine().trim();
    if (!confirmation.equals("y")) {
      return;
    }

    System.out.print("Deleting: " + fingerprint.getPath() + " ...");
    System.out.flush();
    File file = new File(fingerprint.getPath());
    if (file.isDirectory()) {
      FileUtils.deleteDirectory(file);
    } else {
      file.delete();
    }
    fingerPrintDao.delete(fingerprint);
    System.out.println();
    Long parentId = fingerprint.getParentId();
    if (parentId != null) {
      updateParent(parentId);
    }
  }

  private void updateParent(long id) {
    Fingerprint parentFingerprint = fingerPrintDao.getById(id);
    System.out.println("Updating " + parentFingerprint.getPath());
    long totalSize = 0;
    List<String> hashes = new ArrayList<>();
    List<String> hashes2 = new ArrayList<>();
    List<Fingerprint> fingerprints = fingerPrintDao.getByParentId(id);
    for (Fingerprint fingerprint : fingerprints) {
      totalSize += fingerprint.getSize();
      hashes.add(fingerprint.getHash());
    }
    String hash = DigestUtils.sha1Hex(hashes.stream().sorted().collect(Collectors.joining()));
    String hash2 = DigestUtils.sha1Hex(hashes2.stream().sorted().collect(Collectors.joining()));
    parentFingerprint.setSize(totalSize);
    parentFingerprint.setHash(hash);
    fingerPrintDao.update(parentFingerprint);
    if (parentFingerprint.getParentId() != null) {
      updateParent(parentFingerprint.getParentId());
    }
  }

  private void topDuplicates(long limit) {
    List<String> hashes = fingerPrintDao.topDuplicates(limit);
    aliases.clear();
    for (String hash : hashes) {
      System.out.println(hash + ":");
      List<Fingerprint> fingerprints = fingerPrintDao.getByHash(hash);
      for (Fingerprint fingerprint : fingerprints) {
        long nextAlias = aliases.size() + 1;
        aliases.put(nextAlias, fingerprint);
        System.out.format("  %6d %4s %c %7s %s\n",
            fingerprint.getId(),
            "@" + nextAlias,
            fingerprint.getType() == 'd' ? 'd' : '-',
            humanReadableSize(fingerprint.getSize()),
            fingerprint.getPath());
      }
    }
  }

}
