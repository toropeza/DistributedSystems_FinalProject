import DataModel.ChannelPosting;
import DataModel.MessageCache;
import ResponseModels.ChannelsHistoryResponse;
import ResponseModels.DSChannelsStarResponse;
import ResponseModels.SuccessResponse;
import com.google.gson.Gson;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;

/**
 * Runnable Class that parses the Requests coming into the MessageServerAPI
 * */
public class WebServerAPIHelper implements Runnable {

  static final Logger logger = LogManager.getLogger(WebServer.class);

  //HTTP Response Codes
  private final String HTTP200 = "HTTP/1.1 200 OK\n";
  private final String HTTP400 = "HTTP/1.1 400 Bad Request \r\n";
  private final String HTTP404 = "HTTP/1.1 404 Not Found \r\n";
  private final String HTTP405 = "HTTP/1.1 405 Method Not Allowed \r\n";
  private final String CONTENT_TYPE = "Content-Type: application/json \n";
  private String CONTENT_LENGTH = "Content-Length:";

  //API
  private final String postMessageMethod = "chat.postMessage";
  private final String channelHistoryMethod = "channels.history";
  private final String messageStarMethod = "message.star";
  private final String channelStarMethod = "channels.star";
  private final String electionCoordinatorMethod = "election.coordinator";
  private final String dataServerBaseURL;

  //Socket connection for the request
  private Socket socket;

  //HTTP Helper for parsing HTTP Requests
  HTTPHelper httpHelper;

  //Gson object for JSON parsing
  Gson gson;

  //Data Server Configuration
  String dataServerPort;
  String dataServerIP;

  //Cache for starred messages
  MessageCache starredMessageCache;

  /**
   * Creates a new Runnable Helper
   * @param socket The socket connection wishing to make an API Request
   * @param dataServerPort port for data server running on the same ip
   * @param dataServerIP The IP Adress of the server running the Data Server
   * @param starredMessageCache The Message Cache for storing starred messages
   * */
  public WebServerAPIHelper(Socket socket, String dataServerPort, String dataServerIP, MessageCache starredMessageCache){
    this.socket = socket;
    this.dataServerPort = dataServerPort;
    this.dataServerIP = dataServerIP;
    this.starredMessageCache = starredMessageCache;
    gson = new Gson();
    httpHelper = new HTTPHelper();
    dataServerBaseURL = "http://" + dataServerIP + ":" + dataServerPort + "/api/";
  }

  @Override
  public void run() {
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

      String line = in.readLine();
      if (line != null){
        if (httpHelper.isHTTPGET(line)){
          line = URLDecoder.decode(line, "UTF-8");
          String request = httpHelper.getResource(line);
          logger.info(request);
          String response = parseAPIRequest(request);
          if (response != null){
            if (response.contains(HTTP200)){
              out.write(response);
            }else {
              out.write(response);
              SuccessResponse successResponse = new SuccessResponse();
              successResponse.setSuccess(false);
              out.write(gson.toJson(successResponse));
            }
          }
        }else {
          out.write(HTTP405);
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
  private String parseAPIRequest(String request){
    String response = HTTP404;
    String[] path = request.split("/");
    if (path.length == 3){
      String[] methodAndParams = path[2].split("\\?");
      if (methodAndParams.length == 2){
        String method = methodAndParams[0];
        String params = methodAndParams[1];
        if (method.equals(postMessageMethod)){
          response = postMessageToChannel(params);
        }else if (method.equals(channelHistoryMethod)){
          response = returnChannelHistory(params);
        }else if (method.equals(messageStarMethod)){
          response = starChannelMessage(params);
        }else if (method.equals(channelStarMethod)){
          response = getChannelStarredHistory(params);
        }else if (method.equals(electionCoordinatorMethod)){
          parseElectionCoordinatorMethod(params);
          SuccessResponse successResponse = new SuccessResponse();
          successResponse.setSuccess(true);
          response = gson.toJson(successResponse);
        }
      }
    }
    return response;
  }

  /**
   * Parses an election message for a new primary data server
   * @param params The parameters from the REST call
   * @return The JSON Response
   * */
  public void parseElectionCoordinatorMethod(String params){
    if (params != null){
      HashMap<String, String> urlParamsMap = httpHelper.parseURLParams(params);
      if (urlParamsMap != null && urlParamsMap.containsKey("newPrimaryPort")){
        int newPrimaryPort = Integer.valueOf(urlParamsMap.get("newPrimaryPort"));
        String newPrimaryIp = socket.getInetAddress().toString().replaceAll("/","");
        WebServerAPI.setPrimaryData(newPrimaryIp, newPrimaryPort);
        logger.info("New Primary : " + newPrimaryIp + ":" + newPrimaryPort + " added to membership");
      }
    }
  }

  /**
   * Returns the JSON Response for retrieving the channel's starred history
   * @param params The parameters from the REST call
   * @return The JSON Response
   * */
  public String getChannelStarredHistory(String params){
    String response = HTTP404;
    if (params != null){
      HashMap<String, String> urlParamsMap = httpHelper.parseURLParams(params);
      if (urlParamsMap != null && urlParamsMap.containsKey("channel")){
        String channel = urlParamsMap.get("channel");
        if (channel != null){
          String versionNumber = String.valueOf(starredMessageCache.getVersionNumber());
          String channelStarredHistoryRequest = buildDSChannelsStarRequest(channel, versionNumber);
          try {
            String json = httpHelper.performHttpGet(channelStarredHistoryRequest);
            DSChannelsStarResponse dsChannelsStarResponse = gson.fromJson(json, DSChannelsStarResponse.class);

            //Build the starred History response
            ChannelsHistoryResponse starredChannelHistoryResponse = new ChannelsHistoryResponse();
            if (dsChannelsStarResponse.isSuccess()){
              starredChannelHistoryResponse.setSuccess(true);
              //Serve starred messaged from cache or Data Server depending on version
              if (!dsChannelsStarResponse.getVersion().equals(versionNumber)){
                //web server cache is outdated, update cache
                logger.info("Web Server Cache is Outdated. Version is " + versionNumber);
                long freshVersion = Long.valueOf(dsChannelsStarResponse.getVersion());
                ChannelPosting[] starredMessages = dsChannelsStarResponse.getMessages();
                starredMessageCache.updateCache(freshVersion, channel, starredMessages);
                logger.info("Web Server Cache Updated. New Version is " + freshVersion);
              }else {
                logger.info("Web Server Cache is up to Date. Serving from Cache");
              }
              //respond with fresh cache data
              starredChannelHistoryResponse.setMessages(starredMessageCache.getChannelStarredHistory(channel));
            }else {
              starredChannelHistoryResponse.setSuccess(false);
            }
            String starredChannelHistoryJSON = gson.toJson(starredChannelHistoryResponse);
            response = buildHTTPResponse(starredChannelHistoryJSON);
          }catch (Exception e){
            SuccessResponse successResponse = new SuccessResponse();
            successResponse.setSuccess(false);
            response = buildHTTPResponse(gson.toJson(successResponse));
          }

        }else {
          response = HTTP400;
        }
      }else {
        response = HTTP404;
      }
    }
    return response;
  }

  /**
   * Stars a message
   * @param params The parameters from the REST call
   * @return The JSON Response
   * */
  public String starChannelMessage(String params){
    String response = HTTP404;
    if (params != null){
      HashMap<String, String> urlParamsMap = httpHelper.parseURLParams(params);
      if (urlParamsMap != null && urlParamsMap.containsKey("messageid")){
        String messageid = urlParamsMap.get("messageid");
        if (messageid != null){
          String starMessageRequest = buildDSStarMessageRequest(messageid);
          try {
            String json = httpHelper.performHttpGet(starMessageRequest);
            response = buildHTTPResponse(json);
          }catch (Exception e){
            SuccessResponse successResponse = new SuccessResponse();
            successResponse.setSuccess(false);
            response = buildHTTPResponse(gson.toJson(successResponse));
          }
        }else {
          response = HTTP400;
        }
      }else {
        response = HTTP404;
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
    String response = HTTP404;
    if (params != null){
      HashMap<String, String> urlParamsMap = httpHelper.parseURLParams(params);
      if (urlParamsMap != null && urlParamsMap.containsKey("channel")){
        String channel = urlParamsMap.get("channel");
        if (channel != null){
          try {
            String channelHistoryRequest = buildDSChannelHistoryRequest(channel);
            String json = httpHelper.performHttpGet(channelHistoryRequest);
            response = buildHTTPResponse(json);
          }catch (Exception e){
            SuccessResponse successResponse = new SuccessResponse();
            successResponse.setSuccess(false);
            response = buildHTTPResponse(gson.toJson(successResponse));
          }
        }else {
          response = HTTP400;
        }
      }else {
        response = HTTP404;
      }
    }
    return response;
  }

  /**
   * Posts a message to the passed in channel
   * @param params The parameters from the REST call
   * @return The JSON Response
   * */
  public String postMessageToChannel(String params){
    String response = HTTP404;
    if (params != null){
      HashMap<String, String> urlParamsMap = httpHelper.parseURLParams(params);
      if (urlParamsMap != null && urlParamsMap.containsKey("channel") && urlParamsMap.containsKey("text")){
        String channel = urlParamsMap.get("channel");
        String text = urlParamsMap.get("text");
        if (channel != null && text != null){
          try {
            String postMessageRequest = buildDSPostMessageRequest(channel, text);
            String json = httpHelper.performHttpGet(postMessageRequest);
            response = buildHTTPResponse(json);
          }catch (Exception e){
            SuccessResponse successResponse = new SuccessResponse();
            successResponse.setSuccess(false);
            response = buildHTTPResponse(gson.toJson(successResponse));
          }
        }else {
          response = HTTP400;
        }
      }else {
        response = HTTP404;
      }
    }
    return response;
  }

  /**
   * Build a GET request for the Data Server to retrieve the given channel's history
   * @param channel The channel to retrieve the history for
   * */
  public String buildDSChannelHistoryRequest(String channel){
    String args = "?channel=" + channel;
    return dataServerBaseURL + channelHistoryMethod + args;
  }

  /**
   * Build a GET request for posting a message to the given channel
   * @param channel The channel to post to
   * @param text The text to post
   * */
  public String buildDSPostMessageRequest(String channel, String text){
    String args = "?channel=" + channel + "&text=" + text;
    return dataServerBaseURL + postMessageMethod + args;
  }

  /**
   * Build a GET request for starring the given message
   * @param messageId The ID of the message to star
   * */
  public String buildDSStarMessageRequest(String messageId){
    String args = "?messageid=" + messageId;
    return dataServerBaseURL + messageStarMethod + args;
  }

  /**
   * Build a GET request for the Data Server to retrieve the given channel's Starred history
   * @param channel The channel to retreive the history for
   * @param version The version of the Message Cache
   * */
  public String buildDSChannelsStarRequest(String channel, String version){
    String args = "?channel=" + channel + "&version=" + version;
    return dataServerBaseURL + channelStarMethod + args;
  }

  /**
   * Returns the full HTTP Response with headers
   * */
  public String buildHTTPResponse(String json){
    return HTTP200 + CONTENT_TYPE + CONTENT_LENGTH + json.length() + "\n\r\n" + json;
  }
}