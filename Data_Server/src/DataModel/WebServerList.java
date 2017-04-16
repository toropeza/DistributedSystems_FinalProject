package DataModel;

import util.ReadWriteLock;

import java.util.ArrayList;
import java.util.List;

/**
 * A Thread-Safe collection wrapping Data Server Info
 */
public class WebServerList {

  ReadWriteLock readWriteLock = new ReadWriteLock();
  List<WebServerInfo> webServerInfo;

  public WebServerList(){
    webServerInfo = new ArrayList<>();
  }

  /**
   * Sets the given web servers
   * @param webServerIps
   */
  public void setWebServers(List<WebServerInfo> webServerIps){
    readWriteLock.lockWrite();
    this.webServerInfo = webServerIps;
    readWriteLock.unlockWrite();
  }

  /**
   * Adds the given web servers to the lsit
   * @param info
   */
  public void addWebServer(WebServerInfo info){
    readWriteLock.lockWrite();
    if (!containsWebServer(info)){
      webServerInfo.add(info);
    }
    readWriteLock.unlockWrite();
  }

  /**
   * @return A Thread-Safe copy of the Web Server Info List
   */
  public List<WebServerInfo> getWebServerInfo(){
    List<WebServerInfo> copy = new ArrayList<>();
    readWriteLock.lockRead();
    for (WebServerInfo info: webServerInfo){
      WebServerInfo infoCopy = new WebServerInfo();
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
  public boolean containsWebServer(WebServerInfo webServer){
    String ip = webServer.getIp();
    int port = webServer.getPort();
    boolean containsIp = false;
    for (WebServerInfo info: webServerInfo){
      if (info.getIp().equals(ip) && info.getPort() == port){
        containsIp = true;
      }
    }
    return containsIp;
  }
}
