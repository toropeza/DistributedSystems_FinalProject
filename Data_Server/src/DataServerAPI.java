import DataModel.ChannelPosting;
import DataModel.DataServerInfo;
import DataModel.DataServerList;
import DataModel.MessageChannelList;
import DataModel.WebServerInfo;
import DataModel.WebServerList;
import GsonModels.ResponseModels.NewSecondaryResponse;
import GsonModels.ResponseModels.SuccessResponse;
import com.google.gson.Gson;
import util.WorkQueue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLDecoder;
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

  //List of Web Servers
  private WebServerList webServers;

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
    webServers = new WebServerList();
    dataServers = new DataServerList();
    workQueue = new WorkQueue();
    cachedRequests = new ArrayList<>();
    gson = new Gson();
    timer.schedule(new HeartBeatSender(dataServers, webServers, messageChannelList), 0, HeartBeatSender.TIME_TO_SEND);
  }

  /**
   * Sets the Web Server group membership retrieved from primary or config file
   * @param webServerInfo The web server info list
   */
  public void setWebServers(List<WebServerInfo> webServerInfo){
    webServers.setWebServers(webServerInfo);
  }

  /**
   * Sets the Data Server group membership retrieved from primary or config file
   * @param dataServerInfo The data server info list
   */
  public void setDataServers(List<DataServerInfo> dataServerInfo){
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
    workQueue.execute(new DataServerAPIHelper(socketConnection, messageChannelList, webServers, dataServers));

  }

  /**
   * Queries the primary Data Server for information about group membership and the retrieve the current database
   * @param secondaryPort The port for this data server instance
   * @param primaryIp The primary's Ip Address
   * @param primaryPort The primary's Port
   */
  public void queryPrimaryForData(int secondaryPort, String primaryIp, int primaryPort, boolean test){
    //Contact primary for group membership list and database
    String primaryRequest = "http://" + primaryIp + ":" + primaryPort + "/api/config.newSecondary?port=" + secondaryPort;
    logger.info("Sending Secondary Data Server Request to Primary Data Server");
    Runnable newSecondaryTask = new Runnable() {
      @Override
      public void run() {
        String response = new HTTPHelper().performHttpGet(primaryRequest);
        NewSecondaryResponse newSecondaryResponse = new Gson().fromJson(response, NewSecondaryResponse.class);
        List<WebServerInfo> webServerInfo = newSecondaryResponse.getWebServers();
        List<DataServerInfo> dataServerInfo = newSecondaryResponse.getDataServers();
        DataServerInfo primaryInfo = new DataServerInfo();
        primaryInfo.setIp(primaryIp);
        primaryInfo.setPort(primaryPort);
        dataServerInfo.add(primaryInfo);
        Map<String, List<ChannelPosting>> db = newSecondaryResponse.getDatabase();
        setWebServers(webServerInfo);
        setDataServers(dataServerInfo);
        messageChannelList.setDatabase(db);
        messageChannelList.setVersionNumber(newSecondaryResponse.getVersionNumber());
        logger.info("Primary Group Membership Received");
        logger.info("Web Servers: " + webServerInfo);
        logger.info("Data Servers: " + dataServerInfo);
        logger.info("Database: " + db.size() + " channels");
        for (String channel: db.keySet()){
          List<ChannelPosting> postings = db.get(channel);
          logger.info("Channel: " + channel);
          for (ChannelPosting posting: postings){
            logger.info("    " + posting.getText());
          }
        }
        logger.info("Secondary Data Server successfully set up");
        if (test){
          try {
            logger.info("Waiting, send requests for caching!");
            Object objectWaitLock = new Object();
            synchronized (objectWaitLock){
              objectWaitLock.wait(40000);
            }
            logger.info("No longer waiting!");
          }catch (InterruptedException e){
            e.printStackTrace();
          }
        }
      }
    };
    workQueue.execute(newSecondaryTask);
  }
}
