import DataModel.ChannelPosting;
import DataModel.ServerInfo;
import DataModel.DataServerList;
import DataModel.MessageChannelList;
import Paxos.PaxosManager;
import com.google.gson.Gson;
import util.*;
import util.HTTPHelper;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.logging.Logger;

/**
 * API provided by Message Server
 */
public class DataServerAPI {

  static final Logger logger = Logger.getLogger(DataServer.class.getName());

  //Lists of channels and their postings
  private MessageChannelList messageChannelList;

  //List of Data Servers
  private DataServerList dataServers;

  //Queue of runnables executed by Threads
  private WorkQueue workQueue;

  private util.HTTPHelper httpHelper = new HTTPHelper();

  //Gson object for JSON parsing
  private Gson gson;

  //Port used to run this instance
  public static int port;

  public static int START_SEQUENCE_NUM = 0;

  private PaxosManager paxosManager;
  /**
   * Creates a new Data Server
   */
  public DataServerAPI(int port, List<ServerInfo> otherDataServers, int killround, int startSequenceNum){
    this.port = port;
    messageChannelList = new MessageChannelList();
    dataServers = new DataServerList();
    dataServers.setDataServers(otherDataServers);
    workQueue = new WorkQueue();
    gson = new Gson();
    paxosManager = new PaxosManager(dataServers, killround, startSequenceNum);
  }

  /**
   * Asynchronously parses the incoming connection
   * */
  public void parseAPIRequest(Socket socketConnection){
    workQueue.execute(new DataServerAPIHelper(socketConnection, messageChannelList, dataServers, paxosManager));
  }
}
