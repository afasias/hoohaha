package nl.tsakiris.hoohaha;

import java.util.Properties;

public interface Configuration {

  String getJdbcUri();

  Properties getJdbProperties();

}
