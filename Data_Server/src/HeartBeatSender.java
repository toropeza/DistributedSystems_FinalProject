import DataModel.DataServerInfo;
import DataModel.DataServerList;
import GsonModels.ResponseModels.SuccessResponse;
import com.google.gson.Gson;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.TimerTask;

/**
 * Constantly sends heartbeats between Data Servers
 */
public class HeartBeatSender extends TimerTask{

  static final Logger logger = LogManager.getLogger(DataServer.class);

  public static final int TIME_TO_SEND = 5000;
  public static final int TIME_TO_EXPIRE = 5000;
  private HTTPHelper httpHelper;
  private Gson gson;
  private DataServerList dataServerList;
  private String requestMethod = "/api/routine.heartBeat";

  public HeartBeatSender(DataServerList dataServerList){
    this.dataServerList = dataServerList;
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
    List<DataServerInfo> dataServerInfo = dataServerList.getDataServerInfo();
    for (int i=0; i < dataServerInfo.size();i++){
      DataServerInfo dataServer = dataServerInfo.get(i);
      try {
        String request = "http://" + dataServer.getIp() + ":" + dataServer.getPort() + requestMethod;
        logger.info("Sending heartbeat to "+ dataServer.getIp() + ":" + dataServer.getPort());
        String jsonResponse = httpHelper.performHttpGetWithTimeout(request, TIME_TO_EXPIRE);
        SuccessResponse successResponse = gson.fromJson(jsonResponse, SuccessResponse.class);
        if (successResponse.isSuccess()){
          logger.info("Received heartbeat response from " + dataServer.getIp() + ":" + dataServer.getPort());
        }
      }catch (SocketTimeoutException e){
        startElection();
      }
    }
  }

  /**
   * TODO Implement Election algorithm
   */
  private void startElection(){
    logger.info("Failed to esatblish connection with primary data server");
    logger.info("Starting election algorithm");
    //TODO send Election message

  }
}
