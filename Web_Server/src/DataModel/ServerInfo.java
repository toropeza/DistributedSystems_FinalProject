package DataModel;

/**
 * Information describing a Data Server
 */
public class ServerInfo {
  private String ip;
  private int port;

  public ServerInfo(){

  }

  public ServerInfo(String ip, int port){
    this.ip = ip;
    this.port = port;
  }

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
