package nl.tsakiris.hoohaha;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;

@Data
@Builder
@ToString
@RegisterBeanMapper(FingerprintMapper.class)
public class Fingerprint {

  private long id;
  private char type;
  private String path;
  private Long parentId;
  private long size;
  private String hash;
  private String hash2;

}
