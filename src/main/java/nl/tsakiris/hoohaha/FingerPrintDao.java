package nl.tsakiris.hoohaha;

import java.util.List;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface FingerPrintDao {

  @SqlUpdate("DROP TABLE IF EXISTS FingerPrint")
  void dropTable();

  @SqlUpdate("CREATE TABLE IF NOT EXISTS FingerPrint ("
      + "  id INT UNSIGNED NOT NULL AUTO_INCREMENT,"
      + "  type CHAR(1) NOT NULL,"
      + "  path TEXT NOT NULL,"
      + "  parentId INT UNSIGNED DEFAULT NULL,"
      + "  size BIGINT unsigned NOT NULL,"
      + "  hash VARCHAR(64) NOT NULL,"
      + "  PRIMARY KEY (id),"
      + "  KEY hash (hash),"
      + "  KEY parentId (parentId),"
      + "  FOREIGN KEY (parentId) REFERENCES FingerPrint (id) ON DELETE CASCADE"
      + ") ENGINE=InnoDB CHARACTER SET=utf8")
  void createTable();

  @SqlUpdate("TRUNCATE TABLE FingerPrint")
  void truncate();

  @GetGeneratedKeys
  @SqlUpdate("INSERT INTO FingerPrint"
      + " (id, type, path, parentId, size, hash)"
      + " VALUES (:id, :type, :path, :parentId, :size, :hash)")
  long insert(@BindBean Fingerprint fingerPrint);

  @SqlUpdate("UPDATE FingerPrint SET hash = :hash, size = :size WHERE id = :id")
  void update(@BindBean Fingerprint fingerPrint);

  @SqlQuery("SELECT * FROM FingerPrint WHERE parentId IS NULL")
  List<Fingerprint> getTopLevel();

  @SqlQuery("SELECT * FROM FingerPrint WHERE parentId = :parentId")
  List<Fingerprint> getByParentId(@Bind("parentId") long parentId);

  @SqlQuery("SELECT * FROM FingerPrint WHERE id = :id")
  Fingerprint getById(@Bind("id") long id);

  @SqlUpdate("DELETE FROM FingerPrint WHERE id = :id")
  void delete(@BindBean Fingerprint fingerprint);

  @SqlQuery("SELECT hash FROM FingerPrint "
      + "GROUP BY hash HAVING count(*) > 1 ORDER BY size DESC LIMIT :limit")
  List<String> topDuplicates(@Bind("limit") long limit);

  @SqlQuery("SELECT * FROM FingerPrint WHERE hash = :hash")
  List<Fingerprint> getByHash(@Bind("hash") String hash);

}
