package util;

import DataModel.ServerInfo;

/**
 * Information describing the config file
 */
public class JSONServerConfigModel {
  int port;
  ServerInfo webserver;

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
