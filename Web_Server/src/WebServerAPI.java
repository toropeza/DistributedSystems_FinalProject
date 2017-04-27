import DataModel.DataServerList;
import DataModel.MessageCache;
import DataModel.ServerInfo;
import util.WorkQueue;

import java.net.Socket;
import java.util.List;
import java.util.Random;

/**
 * API provided by Message Server
 */
public class WebServerAPI {

  //cache of starred Messages in a channel
  private MessageCache starredMessageCache;

  //list of data servers
  public static DataServerList dataServerList;

  //Queue of runnables executed by Threads
  private WorkQueue workQueue;

  public WebServerAPI(List<ServerInfo> dataServers){
    workQueue = new WorkQueue();
    starredMessageCache = new MessageCache();
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
    workQueue.execute(new WebServerAPIHelper(socketConnection, dataServerPort, dataServerIP, starredMessageCache));
  }
}
