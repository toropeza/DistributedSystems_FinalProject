package DataModel;

/**
 * Information describing a Data Server
 */
public class ServerInfo {
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

  @Override
  public String toString() {
    return ip + ":" + port;
  }
}
