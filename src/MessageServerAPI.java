import java.net.Socket;

/**
 * API provided by Message Server
 */
public class MessageServerAPI {

  //Lists of channels and their postings
  private MessageChannelList messageChannelList;

  //Queue of runnables executed by Threads
  private WorkQueue workQueue;

  public MessageServerAPI(){
    messageChannelList = new MessageChannelList();
    workQueue = new WorkQueue();
  }

  /**
   * Asynchronously parses the incoming connection
   * */
  public void parseAPIRequest(Socket socketConnection){
    workQueue.execute(new MessageServerAPIHelper(socketConnection, messageChannelList));
  }
}
