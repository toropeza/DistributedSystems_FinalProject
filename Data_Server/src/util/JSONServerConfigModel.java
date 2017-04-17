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
  int serverNumber;
  int primaryServerNumber;
  int port;
  boolean test;
  List<WebServerInfo> webservers;

  public int getPrimaryServerNumber() {
    return primaryServerNumber;
  }

  public void setPrimaryServerNumber(int primaryServerNumber) {
    this.primaryServerNumber = primaryServerNumber;
  }

  public boolean isTest() {
    return test;
  }

  public int getServerNumber() {
    return serverNumber;
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
