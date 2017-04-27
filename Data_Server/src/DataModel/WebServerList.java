package DataModel;

import util.ReadWriteLock;

import java.util.ArrayList;
import java.util.List;

/**
 * A Thread-Safe collection wrapping Data Server Info
 */
public class WebServerList {

  ReadWriteLock readWriteLock = new ReadWriteLock();
  List<ServerInfo> webServerInfo;

  public WebServerList(){
    webServerInfo = new ArrayList<>();
  }

  /**
   * Sets the given web servers
   * @param webServerIps
   */
  public void setWebServers(List<ServerInfo> webServerIps){
    readWriteLock.lockWrite();
    this.webServerInfo = webServerIps;
    readWriteLock.unlockWrite();
  }

  /**
   * Adds the given web servers to the lsit
   * @param info
   */
  public void addWebServer(ServerInfo info){
    readWriteLock.lockWrite();
    if (!containsWebServer(info)){
      webServerInfo.add(info);
    }
    readWriteLock.unlockWrite();
  }

  /**
   * @return A Thread-Safe copy of the Web Server Info List
   */
  public List<ServerInfo> getWebServerInfo(){
    List<ServerInfo> copy = new ArrayList<>();
    readWriteLock.lockRead();
    for (ServerInfo info: webServerInfo){
      ServerInfo infoCopy = new ServerInfo();
      infoCopy.setIp(info.getIp());
      infoCopy.setPort(info.getPort());
      copy.add(infoCopy);
    }
    readWriteLock.unlockRead();
    return copy;
  }

  /**
   * @return Whether the Web Server Info List contains the given info
   */
  public boolean containsWebServer(ServerInfo webServer){
    String ip = webServer.getIp();
    int port = webServer.getPort();
    boolean containsIp = false;
    for (ServerInfo info: webServerInfo){
      if (info.getIp().equals(ip) && info.getPort() == port){
        containsIp = true;
      }
    }
    return containsIp;
  }
}
