import DataModel.MessageCache;
import util.WorkQueue;

import java.net.Socket;

/**
 * API provided by Message Server
 */
public class WebServerAPI {

  //cache of starred Messages in a channel
  private MessageCache starredMessageCache;

  //Data Server configuration
  private static String dataServerPort;
  private static String dataServerIP;

  //Queue of runnables executed by Threads
  private WorkQueue workQueue;

  public WebServerAPI(String dataServerPort, String dataServerIP){
    this.dataServerPort = dataServerPort;
    this.dataServerIP = dataServerIP;
    workQueue = new WorkQueue();
    starredMessageCache = new MessageCache();
  }

  /**
   * Asynchronously parses the incoming connection
   * */
  public void parseAPIRequest(Socket socketConnection){
    workQueue.execute(new WebServerAPIHelper(socketConnection, dataServerPort, dataServerIP, starredMessageCache));
  }

  /**
   * Sets the Primary Data Server Info
   * @param ip The Primary's IP
   * @param port The Primary's Port
   */
  public static synchronized void setPrimaryData(String ip, int port){
    dataServerIP = ip;
    dataServerPort = String.valueOf(port);
  }
}
