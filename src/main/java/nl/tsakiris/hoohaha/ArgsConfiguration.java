package nl.tsakiris.hoohaha;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ArgsConfiguration implements Configuration {

  private final String jdbcUri;
  private final Properties jdbcProperties;

  public ArgsConfiguration(String[] args) {
    this.jdbcUri = args[0];
    this.jdbcProperties = new Properties();
    this.jdbcProperties.setProperty("user", args[1]);
    this.jdbcProperties.setProperty("password", args[2]);
  }

  @Override
  public String getJdbcUri() {
    return jdbcUri;
  }

  @Override
  public Properties getJdbProperties() {
    return jdbcProperties;
  }

}
