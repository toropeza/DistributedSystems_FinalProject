import DataModel.ChannelPosting;
import DataModel.ServerInfo;
import DataModel.DataServerList;
import DataModel.MessageChannelList;
import DataModel.WebServerList;
import GsonModels.ResponseModels.NewSecondaryResponse;
import com.google.gson.Gson;
import util.WorkQueue;

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

  private HTTPHelper httpHelper = new HTTPHelper();

  //Gson object for JSON parsing
  private Gson gson;

  //Collection for caching requests
  private ArrayList<String> cachedRequests;

  //Port used to run this instance
  public static int port;

  //Timer used to send heartbeat messages
  Timer timer = new Timer();

  /**
   * Creates a new Data Server
   */
  public DataServerAPI(int port){
    this.port = port;
    messageChannelList = new MessageChannelList();
    dataServers = new DataServerList();
    workQueue = new WorkQueue();
    cachedRequests = new ArrayList<>();
    gson = new Gson();
    timer.schedule(new HeartBeatSender(dataServers, messageChannelList), 0, HeartBeatSender.TIME_TO_SEND);
  }
  /**
   * Sets the Data Server group membership retrieved from primary or config file
   * @param dataServerInfo The data server info list
   */
  public void setDataServers(List<ServerInfo> dataServerInfo){
    dataServers.setDataServers(dataServerInfo);
  }

  /**
   * Sets the database retrieved from the primary when booting up the secondary data server
   * @param db The full database
   */
  public void addDatabase(Map<String, List<ChannelPosting>> db){
    messageChannelList.addDatabase(db);
  }

  /**
   * Asynchronously parses the incoming connection
   * */
  public void parseAPIRequest(Socket socketConnection){
    workQueue.execute(new DataServerAPIHelper(socketConnection, messageChannelList, dataServers));

  }
}
