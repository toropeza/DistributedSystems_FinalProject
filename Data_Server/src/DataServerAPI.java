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

  //Collection for caching requests
  private ArrayList<String> cachedRequests;

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
        Map<String, List<ChannelPosting>> db = newSecondaryResponse.getDatabase();
        setWebServers(webServerInfo);
        setDataServers(dataServerInfo);
        addDatabase(db);
        logger.info("Primary Group Membership Received");
        logger.info("Web Servers: " + webServerInfo);
        logger.info("Data Servers: " + dataServerInfo);
        logger.info("Database: " + db.size() + " channels");
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
