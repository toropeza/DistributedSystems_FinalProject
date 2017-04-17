import DataModel.ChannelPosting;
import DataModel.DataServerInfo;
import DataModel.DataServerList;
import DataModel.MessageChannelList;
import DataModel.WebServerInfo;
import DataModel.WebServerList;
import GsonModels.ResponseModels.ElectionAcceptResponse;
import GsonModels.ResponseModels.NewSecondaryResponse;
import GsonModels.ResponseModels.StarMessageResponse;
import GsonModels.ResponseModels.ChannelsHistoryResponse;
import GsonModels.ResponseModels.ChatPostMessageResponse;
import GsonModels.ResponseModels.StarredChannelHistoryResponse;
import GsonModels.ResponseModels.SuccessResponse;
import GsonModels.ResponseModels.UpdateDataResponse;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Runnable Class that parses the Requests coming into the MessageServerAPI
 * */
public class DataServerAPIHelper implements Runnable {

  static final Logger logger = Logger.getLogger(DataServer.class.getName());

  private final int ELECTION_TIMEOUT = 5000;

  //API Methods
  private final String postMessageMethod = "chat.postMessage";
  private final String channelHistoryMethod = "channels.history";
  private final String messageStarMethod = "message.star";
  private final String channelStarMethod = "channels.star";
  private final String configNewSecondaryMethod = "config.newSecondary";
  private final String configNewWebServerMethod = "config.newWebServer";
  private final String notifyNewWebServerMethod = "config.notifyNewWebServer";
  private final String notifyNewSecondaryServerMethod = "config.notifyNewDataServer";
  private final String heartBeatMethod = "routine.heartBeat";
  private final String electionMethod = "election.elect";
  private final String electionCoordinatorMethod = "election.coordinator";
  private final String updateDataMethod = "update.data";

  //Socket connection for the request
  private Socket socket;

  //List of channels with postings
  private MessageChannelList channelList;

  //HTTP Helper for parsing HTTP Requests
  HTTPHelper httpHelper;

  //List of all WebServers
  WebServerList webServers;

  //List of all DataServers
  DataServerList dataServers;

  //Gson object for JSON parsing
  Gson gson;

  /**
   * Creates a new Runnable Helper
   * @param socket The socket connection wishing to make an API Request
   * @param channelList The main DataModel.MessageChannelList reference
   * */
  public DataServerAPIHelper(Socket socket, MessageChannelList channelList, WebServerList webServers, DataServerList dataServers){
    this.socket = socket;
    this.channelList = channelList;
    this.webServers = webServers;
    this.dataServers = dataServers;
    gson = new Gson();
    httpHelper = new HTTPHelper();
  }

  @Override
  public void run() {
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

      String line =  in.readLine();
      if (line != null){
        line = URLDecoder.decode(line, "UTF-8");
        if (httpHelper.isHTTPGET(line)){
          String request = httpHelper.getResource(line);
          logger.info(request);
          String response = parseAPIRequest(request);
          if (response != null){
            if (response.contains(httpHelper.HTTP200)){
              out.write(response);
            }else {
              out.write(response);
              SuccessResponse successResponse = new SuccessResponse();
              successResponse.setSuccess(false);
              out.write(gson.toJson(successResponse));
            }
          }
        } else {
          out.write(httpHelper.HTTP405);
        }
        out.flush();
        socket.close();
      }
    } catch (IOException e){
      e.printStackTrace();
    }
  }

  /**
   * Parses the API Request line to interact with the API
   * @param request The REST Request to the Request
   * */
  public String parseAPIRequest(String request){
    String response = httpHelper.HTTP404;
    String[] path = request.split("/");
    if (path.length == 3){
      String[] methodAndParams = path[2].split("\\?");
      String method = methodAndParams[0];
      if (methodAndParams.length > 1){
        //methods with parameters
        String params = methodAndParams[1];
        if (method.equals(postMessageMethod)){
          response = postMessageToChannel(params);
        }else if (method.equals(channelHistoryMethod)){
          response = returnChannelHistory(params);
        }else if (method.equals(messageStarMethod)){
          response = starMessage(params);
        }else if (method.equals(channelStarMethod)){
          response = getChannelStarredHistory(params);
        }else if (method.equals(notifyNewWebServerMethod)){
          response = getNotifyNewWebServerResponse(params);
        }else if (method.equals(notifyNewSecondaryServerMethod)){
          response = getNotifyNewSecondaryServerResponse(params);
        }else if (method.equals(configNewSecondaryMethod)){
          response = getConfigNewSecondaryResponse(params);
        }else if (method.equals(configNewWebServerMethod)){
          response = getConfigNewWebServerResponse(params);
        }else if (method.equals(electionCoordinatorMethod)){
          parseElectionCoordinatorMessage(params);
          response = httpHelper.buildHTTPResponse(getSuccessResponse(true));
        }else if (method.equals(updateDataMethod)){
          response = getUpdateDataMethodResponse(params);
        }
      }else {
        //methods with no parameters
        if (method.equals(heartBeatMethod)){
          response = httpHelper.buildHTTPResponse(getSuccessResponse(true));
        }else if (method.equals(electionMethod)){
          response = httpHelper.buildHTTPResponse(getElectionAcceptResponse());
          sendElectionMessages();
        }
      }
    }
    return response;
  }

  /**
   * Returns the JSON Response for updating a data server's data
   * @return The JSON Response
   * */
  public String getUpdateDataMethodResponse(String params){
    String response = httpHelper.HTTP404;
    if (params != null){
      HashMap<String, String> urlParamsMap = httpHelper.parseURLParams(params);
      if (urlParamsMap != null && urlParamsMap.containsKey("version")){
        String version = urlParamsMap.get("version");
        if (version != null){
          UpdateDataResponse updateDataResponse = new UpdateDataResponse();
          boolean success = false;
          try {
            //add freshest version to response
            long dataServerVersion = Long.valueOf(version);
            long freshestVersion = channelList.freshVersion(dataServerVersion);
            updateDataResponse.setVersion(String.valueOf(freshestVersion));

            //Add History in response if version not up-to-date
            if (freshestVersion != dataServerVersion){
              logger.info("Data Server out of data, sending new database");
              Map<String, List<ChannelPosting>> channelHistory = channelList.getDatabase();
              success = channelHistory != null;
              updateDataResponse.setData(channelHistory);
            }else {
              success = true;
            }
          }catch (NumberFormatException e){
            success = false;
          }
          updateDataResponse.setSuccess(success);
          String json = gson.toJson(updateDataResponse);
          response = httpHelper.buildHTTPResponse(json);
        }else {
          response = httpHelper.HTTP400;
        }
      }else {
        response = httpHelper.HTTP404;
      }
    }
    return response;
  }

  /**
   * Parses the message indicating a new primary data server has been elected
   * @param params
   */
  public void parseElectionCoordinatorMessage(String params){
    if (params != null){
      HashMap<String, String> urlParamsMap = httpHelper.parseURLParams(params);
      if (urlParamsMap != null && urlParamsMap.containsKey("newPrimaryPort")) {
        //remove old primary info
        DataServerInfo oldPrimaryInfo = new DataServerInfo();
        oldPrimaryInfo.setPort(DataServerAPI.primaryPort);
        oldPrimaryInfo.setIp(DataServerAPI.primaryIp);
        dataServers.removeDataServer(oldPrimaryInfo);

        //set new primary info
        int newPrimaryPort = Integer.valueOf(urlParamsMap.get("newPrimaryPort"));
        String newPrimaryIp = socket.getInetAddress().toString().replaceAll("/", "");
        DataServerAPI.primaryPort = newPrimaryPort;
        DataServerAPI.primaryIp = newPrimaryIp;
        DataServerInfo newPrimaryInfo = new DataServerInfo();
        newPrimaryInfo.setIp(newPrimaryIp);
        newPrimaryInfo.setPort(newPrimaryPort);
        dataServers.addDataServer(newPrimaryInfo);
        logger.info("New Data Server Elected as primary, removing old from membership");
        DataServerAPI.setNotElecting();
      }
    }
  }

  /**
   * Returns the JSON Response for the start election method
   * @return The JSON Response
   * */
  public void sendElectionMessages(){
    //update data in case did not receive update
    for (DataServerInfo info: dataServers.getDataServerInfo()){
      long versionNum = channelList.versionNumber();
      String request = "http://" + info.getIp() + ":" + info.getPort() + "/api/" + updateDataMethod + "?version=" + versionNum;
      String response = httpHelper.performHttpGet(request);
      UpdateDataResponse updateDataResponse = gson.fromJson(response, UpdateDataResponse.class);
      if (updateDataResponse.isSuccess()){
        if (!updateDataResponse.getVersion().equals(String.valueOf(versionNum))){
          //data server cache is outdated, update database
          logger.info("Data Server is Outdated. Version is " + versionNum);
          long freshVersion = Long.valueOf(updateDataResponse.getVersion());
          Map<String, List<ChannelPosting>> db = updateDataResponse.getData();
          channelList.setDatabase(db);
          logger.info("Data Server Cache. New Version is " + freshVersion);
        }
      }
    }

    boolean encounteredResponse = false;
    List<DataServerInfo> higherNumberDS = dataServers.getHigherNumberServers(DataServerAPI.port);
    for (DataServerInfo info: higherNumberDS){
      String request = "http://" + info.getIp() + ":" + info.getPort() + "/api/" + electionMethod;
      try {
        //if received response, do nothing. Wait for coordinator message
        String acceptResponse = httpHelper.performHttpGetWithTimeout(request, ELECTION_TIMEOUT);
        encounteredResponse = true;
      } catch (SocketTimeoutException e) {
        //remove from membership and try next data server
        dataServers.removeDataServer(info);
        logger.info("No response from " + info.getIp() + ":" + info.getPort() + " removing from membership");
      }
    }
    if (!encounteredResponse){
      //If there are no replies.. self elect
      logger.info("No higher data servers replied, starting this instance as new primary");
      logger.info("Notifying Data Servers");
      for (DataServerInfo info: dataServers.getLowerNumberServers(DataServerAPI.port)){
        String request = "http://" + info.getIp() + ":" + info.getPort() + "/api/" + electionCoordinatorMethod + "?newPrimaryPort=" + DataServerAPI.port;
        httpHelper.performHttpGet(request);
      }
      logger.info("Notifying Web Servers");
      //notify web servers
      for (WebServerInfo info: webServers.getWebServerInfo()){
        String request = "http://" + info.getIp() + ":" + info.getPort() + "/api/" + electionCoordinatorMethod + "?newPrimaryPort=" + DataServerAPI.port;
        httpHelper.performHttpGet(request);
      }
      DataServerAPI.setPrimary();
      DataServerAPI.setNotElecting();
    }
  }

  /**
   * Returns the JSON Response for the new web server method
   * @return The JSON Response
   * */
  public String getNotifyNewSecondaryServerResponse(String params){
    String response = httpHelper.HTTP404;
    boolean success = false;
    if (params != null){
      HashMap<String, String> urlParamsMap = httpHelper.parseURLParams(params);
      if (urlParamsMap != null && urlParamsMap.containsKey("port") && urlParamsMap.containsKey("ip")){
        int port = Integer.valueOf(urlParamsMap.get("port"));
        String ip = urlParamsMap.get("ip");
        DataServerInfo info = new DataServerInfo();
        info.setPort(port);
        info.setIp(ip);
        dataServers.addDataServer(info);
        logger.info("New data server added to membership");
        success = true;
      }
    }

    String json = getSuccessResponse(success);
    response = httpHelper.buildHTTPResponse(json);
    return response;
  }

  /**
   * Returns the JSON Response for the new secondary method
   * @return The JSON Response
   * */
  public String getConfigNewSecondaryResponse(String params){
    String response = httpHelper.HTTP404;
    if (params != null){
      HashMap<String, String> urlParamsMap = httpHelper.parseURLParams(params);
      if (urlParamsMap != null && urlParamsMap.containsKey("port")){
        int port = Integer.valueOf(urlParamsMap.get("port"));
        String json = buildConfigNewSecondaryResponse(port);
        response = httpHelper.buildHTTPResponse(json);
      }
    }
    return response;
  }

  /**
   * Returns the JSON Response for the configure web server method
   * @return The JSON Response
   * */
  public String getConfigNewWebServerResponse(String params){
    boolean success = false;
    if (params != null){
      HashMap<String, String> urlParamsMap = httpHelper.parseURLParams(params);
      if (urlParamsMap != null && urlParamsMap.containsKey("port")){
        int wsPort = Integer.valueOf(urlParamsMap.get("port"));
        String wsIp = socket.getInetAddress().toString().replaceAll("/","");

        //add to web server list
        WebServerInfo info = new WebServerInfo();
        info.setPort(wsPort);
        info.setIp(wsIp);
        webServers.addWebServer(info);
        success = true;
        logger.info("New Web Server " + wsIp + ":" + wsPort + " added to Group Membership");

        List<DataServerInfo> dataServerList = dataServers.getDataServerInfo();
        if (dataServerList.size() > 0){
          //notify each secondary data server of the new web server
          boolean[] successfulResponses = new boolean[dataServerList.size()];
          for (int i=0; i < dataServerList.size(); i++){
            DataServerInfo dataServer = dataServerList.get(i);
            int port = dataServer.getPort();
            String ip = dataServer.getIp();

            String request = buildNotifyNewWebServerRequest(ip, port, wsIp, wsPort);
            String response = httpHelper.performHttpGet(request);
            SuccessResponse successResponse = gson.fromJson(response, SuccessResponse.class);
            if (successResponse != null){
              if (successResponse.isSuccess()){
                successfulResponses[i] = true;
                logger.info("Secondary Data Server " + i + " notified of new Web Server");
              }
            }
          }
          success = false;
          for (int i=0; i < successfulResponses.length; i++){
            if (successfulResponses[i]){
              success = true;
            }else {
              success = false;
              break;
            }
          }
        }
      }
    }
    if (success){
      logger.info("All Secondary Data Servers notified of new Web Server");
    }else {
      logger.warning("There was an error notifying secondary Data Servers of new Web Server");
    }
    String json = getSuccessResponse(success);
    String response = httpHelper.buildHTTPResponse(json);
    return response;
  }

  /**
   * Returns the JSON Response for the new web server method
   * @return The JSON Response
   * */
  public String getNotifyNewWebServerResponse(String params){
    String response = httpHelper.HTTP404;
    boolean success = false;
    if (params != null){
      HashMap<String, String> urlParamsMap = httpHelper.parseURLParams(params);
      if (urlParamsMap != null && urlParamsMap.containsKey("port") && urlParamsMap.containsKey("ip")){
        int port = Integer.valueOf(urlParamsMap.get("port"));
        String ip = urlParamsMap.get("ip");
        WebServerInfo info = new WebServerInfo();
        info.setPort(port);
        info.setIp(ip);
        webServers.addWebServer(info);
        success = true;
        logger.info("Web Server " + ip + ":" + port +" added to group membership");
      }
    }
    String json = getSuccessResponse(success);
    response = httpHelper.buildHTTPResponse(json);
    return response;
  }

  /**
   * Returns the JSON Response for retrieving the channel's starred history
   * @param params The parameters from the REST call
   * @return The JSON Response
   * */
  public String getChannelStarredHistory(String params){
    String response = httpHelper.HTTP404;
    if (params != null){
      HashMap<String, String> urlParamsMap = httpHelper.parseURLParams(params);
      if (urlParamsMap != null && urlParamsMap.containsKey("channel") && urlParamsMap.containsKey("version")){
        String channel = urlParamsMap.get("channel");
        String version = urlParamsMap.get("version");
        if (channel != null && version != null){
          StarredChannelHistoryResponse starredChannelHistoryResponse = new StarredChannelHistoryResponse();
          boolean success = false;
          try {
            //add freshest version to response
            long webServerVersion = Long.valueOf(version);
            long freshestVersion = channelList.freshVersion(webServerVersion);
            starredChannelHistoryResponse.setVersion(String.valueOf(freshestVersion));

            //Add History in response if version not up-to-date
            if (freshestVersion != webServerVersion){
              Object[] channelHistory = channelList.getChannelStarredHistory(channel);
              success = channelHistory != null;
              starredChannelHistoryResponse.setMessages(channelHistory);
            }else {
              success = true;
            }
          }catch (NumberFormatException e){
            success = false;
          }
          starredChannelHistoryResponse.setSuccess(success);
          String json = gson.toJson(starredChannelHistoryResponse);
          response = httpHelper.buildHTTPResponse(json);
        }else {
          response = httpHelper.HTTP400;
        }
      }else {
        response = httpHelper.HTTP404;
      }
    }
    return response;
  }

  /**
   * Stars a message
   * @param params The parameters from the REST call
   * @return The JSON Response
   * */
  public String starMessage(String params){
    String response = httpHelper.HTTP404;
    if (params != null){
      HashMap<String, String> urlParamsMap = httpHelper.parseURLParams(params);
      if (urlParamsMap != null && urlParamsMap.containsKey("messageid")){
        String messageid = urlParamsMap.get("messageid");
        if (messageid != null){
          boolean success = true;
          if (DataServerAPI.isPrimary()){
            //forward star to secondaries
            List<DataServerInfo> dataServerInfoList = dataServers.getDataServerInfo();
            for (int i=0; i < dataServerInfoList.size(); i++){
              DataServerInfo secondary = dataServerInfoList.get(i);
              int secondaryPort = secondary.getPort();
              String secondaryIp = secondary.getIp();
              String secondaryRequest = "http://" + secondaryIp + ":" + secondaryPort + "/api/" + messageStarMethod + "?messageid=" + messageid;
              String secondaryResponse = httpHelper.performHttpGet(secondaryRequest);
              StarMessageResponse messageResponse = gson.fromJson(secondaryResponse, StarMessageResponse.class);
              if (!messageResponse.isSuccess()){
                success = false;
                logger.info("Data server " + i + " unsuccessfully starred message");
              }else {
                logger.info("Data server " + i + " successfully starred message");
              }
            }
          }

          success = channelList.starMessage(messageid) && success;
          StarMessageResponse starMessageResponse = new StarMessageResponse();
          starMessageResponse.setSuccess(success);
          String json = gson.toJson(starMessageResponse);
          response = httpHelper.buildHTTPResponse(json);
        }else {
          response = httpHelper.HTTP400;
        }
      }else {
        response = httpHelper.HTTP404;
      }
    }
    return response;
  }

  /**
   * Returns the JSON Response for retrieving the channel history
   * @param params The parameters from the REST call
   * @return The JSON Response
   * */
  public String returnChannelHistory(String params){
    String response = httpHelper.HTTP404;
    if (params != null){
      HashMap<String, String> urlParamsMap = httpHelper.parseURLParams(params);
      if (urlParamsMap != null && urlParamsMap.containsKey("channel")){
        String channel = urlParamsMap.get("channel");
        if (channel != null){
          Object[] channelHistory = channelList.getChannelHistory(channel);
          boolean success = channelHistory != null;
          String json = getChannelHistoryResponse(channelHistory, success);
          response = httpHelper.buildHTTPResponse(json);
        }else {
          response = httpHelper.HTTP400;
        }
      }else {
        response = httpHelper.HTTP404;
      }
    }
    return response;
  }

  /**
   * Returns the JSON response for retrieving a channel's history
   * @param channelHistory The channel postings
   * @return the JSON response for retrieving a channel's history
   * */
  public String getChannelHistoryResponse(Object[] channelHistory, boolean success){
    ChannelsHistoryResponse channelsHistoryResponse = new ChannelsHistoryResponse();

    channelsHistoryResponse.setSuccess(success);
    channelsHistoryResponse.setMessages(channelHistory);
    return gson.toJson(channelsHistoryResponse);
  }

  /**
   * Returns the JSON response for the entire database
   * @return the JSON response for the entire database
   * */
  public String buildConfigNewSecondaryResponse(int newSecondaryPort){
    boolean success = false;
    String newSecondaryIp = socket.getInetAddress().toString().replaceAll("/", "");

    //notify other secondaries
    List<DataServerInfo> dataServerInfoList = dataServers.getDataServerInfo();
    if (dataServerInfoList.size() > 0){
      boolean[] successfulResponses = new boolean[dataServerInfoList.size()];
      for (int i=0; i < dataServerInfoList.size(); i++){
        DataServerInfo dataServerInfo = dataServerInfoList.get(i);
        int secondaryPort = dataServerInfo.getPort();
        String secondaryIp = dataServerInfo.getIp();

        String request = buildNotifyNewSecondaryServerRequest(secondaryIp, secondaryPort, newSecondaryIp, newSecondaryPort);
        String secondaryResponse = httpHelper.performHttpGet(request);
        SuccessResponse successResponse = gson.fromJson(secondaryResponse, SuccessResponse.class);
        if (successResponse != null){
          if (successResponse.isSuccess()){
            successfulResponses[i] = true;
            logger.info("Secondary " + i + " successfully added new data server to its membership list");
          }
        }
      }
      success = false;
      for (int i=0; i < successfulResponses.length; i++){
        if (successfulResponses[i]){
          success = true;
        }else {
          success = false;
          break;
        }
      }
    }

    NewSecondaryResponse response = new NewSecondaryResponse();
    Map<String, List<ChannelPosting>> db =  channelList.getDatabase();
    List<WebServerInfo> webServerInfo = webServers.getWebServerInfo();
    List<DataServerInfo> dataServerInfo = dataServers.getDataServerInfo();
    if (db != null && webServerInfo != null){
      response.setSuccess(success);
      response.setDatabase(db);
      response.setWebServers(webServerInfo);
      response.setDataServers(dataServerInfo);
    }else {
      response.setSuccess(false);
    }

    DataServerInfo info = new DataServerInfo();
    info.setIp(newSecondaryIp);
    info.setPort(newSecondaryPort);
    dataServers.addDataServer(info);
    logger.info("New secondary data server running at " + newSecondaryIp + ":" + newSecondaryPort + " added to group membership");

    return gson.toJson(response);
  }

  /**
   * Posts a message to the passed in channel
   * @param params The parameters from the REST call
   * @return The JSON Response
   * */
  public String postMessageToChannel(String params){
    String response = httpHelper.HTTP404;
    if (params != null){
      HashMap<String, String> urlParamsMap = httpHelper.parseURLParams(params);
      if (urlParamsMap != null && urlParamsMap.containsKey("channel") && urlParamsMap.containsKey("text")){
        String channel = urlParamsMap.get("channel");
        String text = urlParamsMap.get("text");
        if (channel != null && text != null){
          boolean success = true;
          if (DataServerAPI.isPrimary()){
            //notify secondaries of message
            logger.info("Forwarding request to secondaries");
            List<DataServerInfo> dataServerInfos = dataServers.getDataServerInfo();
            for (int i=0; i < dataServerInfos.size(); i++){
              if (DataServerAPI.testing && i==0){
                //skip first DS
                logger.info("Testing: skipping first secondary post");
                continue;
              }
              DataServerInfo dataServer = dataServerInfos.get(i);
              String dsIp = dataServer.getIp();
              int dsPort = dataServer.getPort();
              String dsRequest = "http://" + dsIp + ":" + dsPort + "/api/" + postMessageMethod + "?channel=" + channel + "&text=" + text;
              String dsResponse = httpHelper.performHttpGet(dsRequest);
              ChatPostMessageResponse chatPostMessageResponse = gson.fromJson(dsResponse, ChatPostMessageResponse.class);
              if (!chatPostMessageResponse.isSuccess()){
                success = false;
                logger.info("Secondary " + i + " unsuccessfully posted message");
              }else {
                logger.info("Secondary " + i + " successfully posted message");
              }
            }
            if (DataServerAPI.testing){
              logger.info("Forwarded post to all but first data server.");
              logger.info("Killing primary (self)");
              System.exit(1);
            }
          }
          long messageID = channelList.postMessage(channel, text);
          logger.info("Posted message to channel");
          String json = getPostMessageResponse(success, String.valueOf(messageID));
          response = httpHelper.buildHTTPResponse(json);
        }else {
          response = httpHelper.HTTP400;
        }
      }else {
        response = httpHelper.HTTP404;
      }
    }
    return response;
  }

  /**
   * @return the JSON response for posting to a channel
   * */
  public String getPostMessageResponse(boolean success, String messageID){
    ChatPostMessageResponse chatPostMessageResponse = new ChatPostMessageResponse();
    chatPostMessageResponse.setSuccess(success);
    chatPostMessageResponse.setId(messageID);
    return gson.toJson(chatPostMessageResponse);
  }

  /**
   * @return A simple JSON success response
   * */
  public String getSuccessResponse(boolean success){
    SuccessResponse successResponse = new SuccessResponse();
    successResponse.setSuccess(success);
    return gson.toJson(successResponse);
  }

  /**
   * @return A JSON election "Accept" response
   * */
  public String getElectionAcceptResponse(){
    ElectionAcceptResponse acceptResponse = new ElectionAcceptResponse();
    acceptResponse.setAccept(true);
    return gson.toJson(acceptResponse);
  }

  /**
   * Builds an http request for notifying secondary Data Servers of a new Web Server
   * @param dsIp the Ip of the data server to be notified
   * @param dsPort the Port of the data server to be notified
   * @param wsIp the Ip of the new Web Server
   * @param wsPort the Port of the new Web Server
   * @return the http Request ready to execute
   */
  public String buildNotifyNewWebServerRequest(String dsIp, int dsPort, String wsIp, int wsPort){
    String args = "?ip=" + wsIp + "&port=" + wsPort;
    return  "http://" + dsIp + ":" + dsPort + "/api/" + notifyNewWebServerMethod + args;
  }

  /**
   * Builds an http request for notifying secondary Data Servers of another Secondary Server
   * @param dsIp the Ip of the data server to be notified
   * @param dsPort the Port of the data server to be notified
   * @param newDSIp the Ip of the new Secondary Server
   * @param newDSPort the Port of the new Secondary Server
   * @return the http Request ready to execute
   */
  public String buildNotifyNewSecondaryServerRequest(String dsIp, int dsPort, String newDSIp, int newDSPort){
    String args = "?ip=" + newDSIp + "&port=" + newDSPort;
    return  "http://" + dsIp + ":" + dsPort + "/api/" + notifyNewSecondaryServerMethod + args;
  }
}