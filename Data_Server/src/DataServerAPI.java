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

  //specifies whether the Data Server is currently caching or not
  boolean caching = false;

  //Gson object for JSON parsing
  private Gson gson;

  //static reference for access in threads without reference to instance
  private static boolean isPrimary;

  //static reference for access in threads without reference to instance
  public static DataServerInfo primaryInfo;

  //represent whether the data server back-end is currently electing or not
  public static boolean isElecting = false;

  //Collection for caching requests
  private ArrayList<String> cachedRequests;

  //Port used to run this instance
  public static int port;

  //Timer used to send heartbeat messages
  Timer timer = new Timer();

  //primary data server's ip
  public static String primaryIp;

  //primary data server's port
  public static int primaryPort;

  //whether this instance is testing or not
  public static boolean testing;

  /**
   * Creates a new Data Server
   */
  public DataServerAPI(int port, boolean testing){
    this.port = port;
    this.testing = testing;
    messageChannelList = new MessageChannelList();
    webServers = new WebServerList();
    dataServers = new DataServerList();
    workQueue = new WorkQueue();
    cachedRequests = new ArrayList<>();
    gson = new Gson();
    isElecting = false;
    timer.schedule(new HeartBeatSender(dataServers, webServers, messageChannelList), 0, HeartBeatSender.TIME_TO_SEND);
  }

  /**
   * @return whether this is the primary data server at the moment
   */
  public static boolean isPrimary() {
    return isPrimary;
  }

  /**
   * Sets this server as primary
   */
  public static void setPrimary() {
    DataServerAPI.isPrimary = true;
  }

  /**
   * Sets this node as a secondary
   * @param primaryIp The primary's ip
   * @param primaryPort The primary's port
   */
  public void setSecondary(String primaryIp, int primaryPort){
    this.primaryIp = primaryIp;
    this.primaryPort = primaryPort;
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
    if (isCaching()){
      cacheRequest(socketConnection);
    } else {
      workQueue.execute(new DataServerAPIHelper(socketConnection, messageChannelList, webServers, dataServers));
    }
  }

  /**
   * Sets the Data Server to currently be electing
   */
  public static synchronized void setElecting(){
    isElecting = true;
  }

  /**
   * Sets the Data Server to change to "not electing"
   */
  public static synchronized void setNotElecting(){
    isElecting = false;
  }

  /**
   * Caches the request line to apply later and replies with a success message
   * @param socket
   */
  public void cacheRequest(Socket socket){
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

      String line =  in.readLine();
      if (line != null){
        line = URLDecoder.decode(line, "UTF-8");
        if (httpHelper.isHTTPGET(line)){
          String request = httpHelper.getResource(line);
          cachedRequests.add(request);
          logger.info("Cached request: " + request);
          SuccessResponse successResponse = new SuccessResponse();
          successResponse.setSuccess(true);
          String response = gson.toJson(successResponse);
          out.write(httpHelper.buildHTTPResponse(response));
        }
        out.flush();
        socket.close();
      }
    } catch (IOException e){
      e.printStackTrace();
    }
  }

  /**
   * Applies all of the requests currently cached to the database.
   * Should be called after the database is updated
   */
  public void applyCachedRequests(){
    for (int i=0; i < cachedRequests.size(); i++){
      String request = cachedRequests.get(i);
      new DataServerAPIHelper(null, messageChannelList, webServers, dataServers).parseAPIRequest(request);
      logger.info("Applying cached request " + request);
    }
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
        setCaching(true);
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
        applyCachedRequests();
        setCaching(false);
      }
    };
    workQueue.execute(newSecondaryTask);
  }

  /**
   * @return whether the Data Server is currently caching
   */
  public synchronized boolean isCaching() {
    return caching;
  }

  /**
   * Sets the Data Server to start/stop caching requests
   */
  public synchronized void setCaching(boolean caching) {
    this.caching = caching;
  }
}
