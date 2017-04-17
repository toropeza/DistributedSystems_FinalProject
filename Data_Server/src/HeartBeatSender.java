import DataModel.ChannelPosting;
import DataModel.DataServerInfo;
import DataModel.DataServerList;
import DataModel.MessageChannelList;
import DataModel.WebServerInfo;
import DataModel.WebServerList;
import GsonModels.ResponseModels.SuccessResponse;
import GsonModels.ResponseModels.UpdateDataResponse;
import com.google.gson.Gson;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * Constantly sends heartbeats between Data Servers
 */
public class HeartBeatSender extends TimerTask{

  static final Logger logger = Logger.getLogger(DataServer.class.getName());

  public static final int TIME_TO_SEND = 10000;
  public static final int TIME_TO_EXPIRE = 5000;
  private final int ELECTION_TIMEOUT = 5000;
  private HTTPHelper httpHelper;
  private Gson gson;
  private DataServerList dataServerList;
  private MessageChannelList history;
  private WebServerList webServerList;
  private String requestMethod = "/api/routine.heartBeat";
  private final String electionMethod = "election.elect";
  private final String electionCoordinatorMethod = "election.coordinator";
  private final String updateDataMethod = "update.data";

  public HeartBeatSender(DataServerList dataServerList, WebServerList webServerList, MessageChannelList history){
    this.dataServerList = dataServerList;
    this.webServerList = webServerList;
    this.history = history;
    httpHelper = new HTTPHelper();
    gson = new Gson();
  }
    @Override
    public void run() {
      //Send heartbeat to Data Servers
      sendHeartBeats();
    }

  /**
   * Sends a heartbeat message to each Data Server.
   * Will start election algorithm on heartbeat failure
   */
  private void sendHeartBeats() {
    if (!DataServerAPI.isElecting){
      List<DataServerInfo> dataServerInfo = dataServerList.getDataServerInfo();
      for (int i=0; i < dataServerInfo.size();i++){
        DataServerInfo dataServer = dataServerInfo.get(i);
        try {
          String request = "http://" + dataServer.getIp() + ":" + dataServer.getPort() + requestMethod;
          logger.info("Sending heartbeat to "+ dataServer.getIp() + ":" + dataServer.getPort());
          String jsonResponse = httpHelper.performHttpGetWithTimeout(request, TIME_TO_EXPIRE);
          SuccessResponse successResponse = gson.fromJson(jsonResponse, SuccessResponse.class);
          if (successResponse.isSuccess()){
            logger.info("HBS:Received heartbeat response from " + dataServer.getIp() + ":" + dataServer.getPort());
          }
        }catch (SocketTimeoutException e){
          //remove from membership and try next data server
          dataServerList.removeDataServer(dataServer);
          logger.info("No response from " + dataServer.getIp() + ":" + dataServer.getPort() + " removing from membership");
          startElection();
        }
      }
    }
  }

  /**
   * Starts the election
   */
  private void startElection(){
    DataServerAPI.setElecting();
    logger.info("Failed to establish connection with primary data server");
    logger.info("Starting election algorithm");

    //update data in case did not receive update
    for (DataServerInfo info: dataServerList.getDataServerInfo()){
      long versionNum = history.versionNumber();
      String request = "http://" + info.getIp() + ":" + info.getPort() + "/api/" + updateDataMethod + "?version=" + versionNum;
      String response = httpHelper.performHttpGet(request);
      UpdateDataResponse updateDataResponse = gson.fromJson(response, UpdateDataResponse.class);
      if (updateDataResponse.isSuccess()){
        if (!updateDataResponse.getVersion().equals(String.valueOf(versionNum))){
          //data server cache is outdated, update database
          logger.info("Data Server is Outdated. Version is " + versionNum);
          long freshVersion = Long.valueOf(updateDataResponse.getVersion());
          Map<String, List<ChannelPosting>> db = updateDataResponse.getData();
          history.setDatabase(db);
          logger.info("Data Server Cache. New Version is " + freshVersion);
        }
      }
    }

    //send Election message
    boolean encounteredResponse = false;
    List<DataServerInfo> higherNumberDS = dataServerList.getHigherNumberServers(DataServerAPI.port);
    for (DataServerInfo info: higherNumberDS){
      String request = "http://" + info.getIp() + ":" + info.getPort() + "/api/" + electionMethod;
      try {
        //if received response, do nothing. Wait for coordinator message
        String acceptResponse = httpHelper.performHttpGetWithTimeout(request, ELECTION_TIMEOUT);
        encounteredResponse = true;
      } catch (SocketTimeoutException e) {
        //try next data server
        dataServerList.removeDataServer(info);
        logger.info("No response from " + info.getIp() + ":" + info.getPort() + " removing from membership");
      }
    }
    if (!encounteredResponse){
      //If there are no replies.. self elect
      logger.info("No higher data servers replied, starting this isntance as new primary");
      for (DataServerInfo info: dataServerList.getLowerNumberServers(DataServerAPI.port)){
        String request = "http://" + info.getIp() + ":" + info.getPort() + "/api/" + electionCoordinatorMethod + "?newPrimaryPort=" + DataServerAPI.port;
        httpHelper.performHttpGet(request);
      }
      logger.info("Notifying Web Servers");
      //notify web servers
      for (WebServerInfo info: webServerList.getWebServerInfo()){
        String request = "http://" + info.getIp() + ":" + info.getPort() + "/api/" + electionCoordinatorMethod + "?newPrimaryPort=" + DataServerAPI.port;
        httpHelper.performHttpGet(request);
      }
      DataServerAPI.setPrimary();
      DataServerAPI.setNotElecting();
    }
  }
}
