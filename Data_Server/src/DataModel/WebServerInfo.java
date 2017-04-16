package DataModel;

/**
 * Information describing a Web Server
 */
public class WebServerInfo {
  private String ip;
  private int port;

  public String getIp() {
    return ip;
  }

  public int getPort() {
    return port;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public void setPort(int port) {
    this.port = port;
  }
}
