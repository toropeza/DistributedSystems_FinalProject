package util;

import DataModel.WebServerInfo;

import java.util.List;

/**
 * Information describing the config file
 */
public class JSONServerConfigModel {
  int port;
  WebServerInfo webserver;

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public WebServerInfo getWebserver() {
    return webserver;
  }

  public void setWebserver(WebServerInfo webserver) {
    this.webserver = webserver;
  }
}
