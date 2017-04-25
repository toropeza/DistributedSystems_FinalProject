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
        dataServerList.removeDataServer(dataServer);
      }
    }
  }
}
