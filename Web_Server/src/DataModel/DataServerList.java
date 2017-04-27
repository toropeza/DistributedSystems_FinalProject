package DataModel;

import util.ReadWriteLock;

import java.util.ArrayList;
import java.util.List;

/**
 * A Thread-Safe collection wrapping Data Server Info
 */
public class DataServerList {

  ReadWriteLock readWriteLock = new ReadWriteLock();
  List<ServerInfo> dataServerInfo;

  public DataServerList(){
    dataServerInfo = new ArrayList<>();
  }

  /**
   * Sets the Data Server Information
   * @param dataServerIps
   */
  public void setDataServers(List<ServerInfo> dataServerIps){
    readWriteLock.lockWrite();
    this.dataServerInfo = dataServerIps;
    readWriteLock.unlockWrite();
  }

  public int size(){
    int size = 0;
    readWriteLock.lockRead();
    size = dataServerInfo.size();
    readWriteLock.unlockRead();
    return size;
  }

  /**
   * Adds the Data Server Information
   * @param info
   */
  public void addDataServer(ServerInfo info){
    readWriteLock.lockWrite();
    if (!containsDataServer(info)){
      dataServerInfo.add(info);
    }
    readWriteLock.unlockWrite();
  }

  /**
   * Returns a Thread-Safe copy of the DataServerInfo List
   * @return
   */
  public List<ServerInfo> getDataServerInfo(){
    List<ServerInfo> copy = new ArrayList<>();
    readWriteLock.lockRead();
    for (ServerInfo info: dataServerInfo){
      ServerInfo infoCopy = new ServerInfo();
      infoCopy.setIp(info.getIp());
      infoCopy.setPort(info.getPort());
      copy.add(infoCopy);
    }
    readWriteLock.unlockRead();
    return copy;
  }

  /**
   * Returns the Data Server at the given index
   */
  public ServerInfo getDataServer(int index){
    ServerInfo dataServer = new ServerInfo();
    readWriteLock.lockRead();
    ServerInfo info = dataServerInfo.get(index);
    dataServer.setIp(info.getIp());
    dataServer.setPort(info.getPort());
    readWriteLock.unlockRead();
    return dataServer;
  }

  /**
   * @return whether the Data Server List contains the given info
   */
  private boolean containsDataServer(ServerInfo dataServerInfo){
    String ip = dataServerInfo.getIp();
    int port = dataServerInfo.getPort();
    boolean containsDataServer = false;
    for (ServerInfo info: this.dataServerInfo){
      if (info.getIp().equals(ip) && info.getPort() == port){
        containsDataServer = true;
      }
    }
    return containsDataServer;
  }

  /**
   * Removes the given data server info
   */
  public void removeDataServer(ServerInfo dataServer){
    String ip = dataServer.getIp();
    int port = dataServer.getPort();

    readWriteLock.lockWrite();
    for (int i=0; i < dataServerInfo.size(); i++){
      ServerInfo info = dataServerInfo.get(i);
      if (info.getIp().equals(ip) && info.getPort() == port){
        dataServerInfo.remove(info);
      }
    }
    readWriteLock.unlockWrite();
  }
}
