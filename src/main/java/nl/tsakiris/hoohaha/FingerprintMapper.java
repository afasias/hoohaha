package nl.tsakiris.hoohaha;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class FingerprintMapper implements RowMapper<Fingerprint> {

  @Override
  public Fingerprint map(ResultSet rs, StatementContext ctx) throws SQLException {
    Long parentId = rs.getLong("parentId");
    if (rs.wasNull()) {
      parentId = null;
    }
    return Fingerprint.builder()
        .id(rs.getLong("id"))
        .hash(rs.getString("hash"))
        .hash2(rs.getString("hash2"))
        .parentId(parentId)
        .path(rs.getString("path"))
        .size(rs.getLong("size"))
        .type(rs.getString("type").charAt(0))
        .build();
  }

}
