import DataModel.DataServerList;
import DataModel.ServerInfo;
import util.WorkQueue;

import java.net.Socket;
import java.util.List;
import java.util.Random;

/**
 * API provided by Message Server
 */
public class WebServerAPI {

  //list of data servers
  public static DataServerList dataServerList;

  //Queue of runnables executed by Threads
  private WorkQueue workQueue;

  public WebServerAPI(List<ServerInfo> dataServers){
    workQueue = new WorkQueue();
    dataServerList = new DataServerList();
    dataServerList.setDataServers(dataServers);
  }

  /**
   * Asynchronously parses the incoming connection
   * */
  public void parseAPIRequest(Socket socketConnection){
    int randPick = new Random().nextInt(dataServerList.size());
    ServerInfo pickedDataServer = dataServerList.getDataServer(randPick);
    String dataServerPort = String.valueOf(pickedDataServer.getPort());
    String dataServerIP = pickedDataServer.getIp();
    workQueue.execute(new WebServerAPIHelper(socketConnection, dataServerPort, dataServerIP));
  }
}
