package nl.tsakiris.hoohaha;

import java.util.Properties;

public class TestConfiguration implements Configuration {

  @Override
  public String getJdbcUri() {
    return "jdbc:mysql://127.0.0.1/giannis";
  }

  @Override
  public Properties getJdbProperties() {
    Properties properties = new Properties();
    properties.setProperty("user", "root");
    properties.setProperty("password", "1234");
    return properties;
  }


}
