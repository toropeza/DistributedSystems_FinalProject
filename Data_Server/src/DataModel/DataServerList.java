package DataModel;

import util.ReadWriteLock;

import java.util.ArrayList;
import java.util.List;

/**
 * A Thread-Safe collection wrapping Data Server Info
 */
public class DataServerList {

  ReadWriteLock readWriteLock = new ReadWriteLock();
  List<DataServerInfo> dataServerInfo;

  public DataServerList(){
    dataServerInfo = new ArrayList<>();
  }

  /**
   * Sets the Data Server Information
   * @param dataServerIps
   */
  public void setDataServers(List<DataServerInfo> dataServerIps){
    readWriteLock.lockWrite();
    this.dataServerInfo = dataServerIps;
    readWriteLock.unlockWrite();
  }

  /**
   * Adds the Data Server Information
   * @param info
   */
  public void addDataServer(DataServerInfo info){
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
  public List<DataServerInfo> getDataServerInfo(){
    List<DataServerInfo> copy = new ArrayList<>();
    readWriteLock.lockRead();
    for (DataServerInfo info: dataServerInfo){
      DataServerInfo infoCopy = new DataServerInfo();
      infoCopy.setIp(info.getIp());
      infoCopy.setPort(info.getPort());
      copy.add(infoCopy);
    }
    readWriteLock.unlockRead();
    return copy;
  }

  /**
   * @return whether the Data Server List contains the given info
   */
  private boolean containsDataServer(DataServerInfo dataServerInfo){
    String ip = dataServerInfo.getIp();
    int port = dataServerInfo.getPort();
    boolean containsDataServer = false;
    for (DataServerInfo info: this.dataServerInfo){
      if (info.getIp().equals(ip) && info.getPort() == port){
        containsDataServer = true;
      }
    }
    return containsDataServer;
  }

  /**
   * Removes the given data server info
   */
  public void removeDataServer(DataServerInfo dataServer){
    String ip = dataServer.getIp();
    int port = dataServer.getPort();

    readWriteLock.lockWrite();
    for (int i=0; i < dataServerInfo.size(); i++){
      DataServerInfo info = dataServerInfo.get(i);
      if (info.getIp().equals(ip) && info.getPort() == port){
        dataServerInfo.remove(info);
      }
    }
    readWriteLock.unlockWrite();
  }
}
