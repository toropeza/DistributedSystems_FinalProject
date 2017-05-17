package util;

import DataModel.ServerInfo;

import java.util.List;

/**
 * Information describing the config file
 */
public class JSONServerConfigModel {
  int port;
  ServerInfo webserver;
  List<ServerInfo> dataServers;
  int startSequenceNum;
  int test;

  public int getStartSequenceNum() {
    return startSequenceNum;
  }

  public void setStartSequenceNum(int startSequenceNum) {
    this.startSequenceNum = startSequenceNum;
  }

  public int getTest() {
    return test;
  }

  public void setTest(int test) {
    this.test = test;
  }

  public List<ServerInfo> getDataServers() {
    return dataServers;
  }

  public void setDataServers(List<ServerInfo> dataServers) {
    this.dataServers = dataServers;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public ServerInfo getWebserver() {
    return webserver;
  }

  public void setWebserver(ServerInfo webserver) {
    this.webserver = webserver;
  }
}
