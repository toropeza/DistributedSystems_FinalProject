import DataModel.MessageChannelList;
import util.WorkQueue;

import java.net.Socket;

/**
 * API provided by Message Server
 */
public class DataServerAPI {

  //Lists of channels and their postings
  private MessageChannelList messageChannelList;

  //Queue of runnables executed by Threads
  private WorkQueue workQueue;

  public DataServerAPI(){
    messageChannelList = new MessageChannelList();
    workQueue = new WorkQueue();
  }

  /**
   * Asynchronously parses the incoming connection
   * */
  public void parseAPIRequest(Socket socketConnection){
    workQueue.execute(new DataServerAPIHelper(socketConnection, messageChannelList));
  }
}
