package util;

import DataModel.WebServerInfo;

import java.util.List;

/**
 * Information describing the config file
 */
public class JSONServerConfigModel {
  String server_type;
  String primaryIp;
  String primaryPort;
  int port;
  boolean test;
  List<WebServerInfo> webservers;

  public boolean isTest() {
    return test;
  }

  public String getServer_type() {
    return server_type;
  }

  public List<WebServerInfo> getWebservers() {
    return webservers;
  }

  public int getPort() {
    return port;
  }

  public String getPrimaryIp() {
    return primaryIp;
  }

  public String getPrimaryPort() {
    return primaryPort;
  }
}
