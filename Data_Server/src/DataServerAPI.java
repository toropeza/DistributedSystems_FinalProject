import DataModel.ChannelPosting;
import DataModel.DataServerInfo;
import DataModel.DataServerList;
import DataModel.MessageChannelList;
import DataModel.WebServerInfo;
import DataModel.WebServerList;
import GsonModels.ResponseModels.NewSecondaryResponse;
import GsonModels.ResponseModels.SuccessResponse;
import com.google.gson.Gson;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import util.WorkQueue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * API provided by Message Server
 */
public class DataServerAPI {

  //Lists of channels and their postings
  private MessageChannelList messageChannelList;

  //List of Web Servers
  private WebServerList webServers;

  //List of Data Servers
  private DataServerList dataServers;

  //Queue of runnables executed by Threads
  private WorkQueue workQueue;

  HTTPHelper httpHelper = new HTTPHelper();

  boolean caching = false;

  //Gson object for JSON parsing
  Gson gson;

  //static reference for access in threads without reference to instance
  private static boolean isPrimary;

  //TODO implement caching for secondary that is booting up
  private ArrayList<Socket> cachedRequests;

  static final Logger logger = LogManager.getLogger(DataServer.class);

  public DataServerAPI(){
    messageChannelList = new MessageChannelList();
    webServers = new WebServerList();
    dataServers = new DataServerList();
    workQueue = new WorkQueue();
    cachedRequests = new ArrayList<>();
    gson = new Gson();
  }

  public static boolean isPrimary() {
    return isPrimary;
  }

  public static void setPrimary() {
    DataServerAPI.isPrimary = true;
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
    }else {
      workQueue.execute(new DataServerAPIHelper(socketConnection, messageChannelList, webServers, dataServers));
    }
  }

  /**
   * TODO Implement caching
   * @param socket
   */
  public void cacheRequest(Socket socket){
    cachedRequests.add(socket);
  }

  /**
   * TODO implement caching
   */
  public void applyCachedRequests(){
    for (int i=0; i < cachedRequests.size(); i++){
      Socket request = cachedRequests.get(i);
      workQueue.execute(new DataServerAPIHelper(request, messageChannelList, webServers, dataServers));
      logger.info("Applying cached request " + i);
    }
  }

  /**
   * Queries the primary Data Server for information about group membership and the retrieve the current database
   * @param secondaryPort The port for this data server instance
   * @param primaryIp The primary's Ip Address
   * @param primaryPort The primary's Port
   */
  public void queryPrimaryForData(int secondaryPort, String primaryIp, int primaryPort){
    //Contact primary for group membership list and database
    String primaryBaseURL = "http://" + primaryIp + ":" + primaryPort + "/api/config.newSecondary?port=" + secondaryPort;
    logger.info("Sending Secondary Data Server Request to Primary Data Server");
    Runnable newSecondaryTask = new Runnable() {
      @Override
      public void run() {
        setCaching(true);
        String response = new HTTPHelper().performHttpGet(primaryBaseURL);
        NewSecondaryResponse newSecondaryResponse = new Gson().fromJson(response, NewSecondaryResponse.class);
        List<WebServerInfo> webServerInfo = newSecondaryResponse.getWebServers();
        List<DataServerInfo> dataServerInfo = newSecondaryResponse.getDataServers();
        Map<String, List<ChannelPosting>> db = newSecondaryResponse.getDatabase();
        setWebServers(webServerInfo);
        setDataServers(dataServerInfo);
        addDatabase(db);
        logger.info("Primary Group Membership Received");
        logger.info("Web Servers: " + webServerInfo);
        logger.info("Data Servers: " + dataServerInfo);
        logger.info("Database: " + db.size() + " channels");
        logger.info("Secondary Data Server successfully set up");
        applyCachedRequests();
        setCaching(false);
      }
    };
    workQueue.execute(newSecondaryTask);
  }

  /**
   * TODO implement caching
   * @return
   */
  public synchronized boolean isCaching() {
    return caching;
  }

  /**
   * TODO implement caching
   * @param caching
   */
  public synchronized void setCaching(boolean caching) {
    this.caching = caching;
  }
}
