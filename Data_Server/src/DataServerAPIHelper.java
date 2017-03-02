import DataModel.MessageChannelList;
import GsonModels.ResponseModels.StarMessageResponse;
import GsonModels.ResponseModels.ChannelsHistoryResponse;
import GsonModels.ResponseModels.ChatPostMessageResponse;
import GsonModels.ResponseModels.StarredChannelHistoryResponse;
import GsonModels.ResponseModels.SuccessResponse;
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
public class DataServerAPIHelper implements Runnable {

  static final Logger logger = LogManager.getLogger(DataServer.class);

  //HTTP Response Codes
  private final String HTTP200 = "HTTP/1.1 200 OK\n";
  private final String HTTP400 = "HTTP/1.1 400 Bad Request \r\n";
  private final String HTTP404 = "HTTP/1.1 404 Not Found \r\n";
  private final String HTTP405 = "HTTP/1.1 405 Method Not Allowed \r\n";

  private final String CONTENT_TYPE = "Content-Type: application/json \n";
  private String CONTENT_LENGTH = "Content-Length:";

  //API Methods
  private final String postMessageMethod = "chat.postMessage";
  private final String channelHistoryMethod = "channels.history";
  private final String messageStarMethod = "message.star";
  private final String channelStarMethod = "channels.star";

  //Socket connection for the request
  private Socket socket;

  //List of channels with postings
  private MessageChannelList channelList;

  //HTTP Helper for parsing HTTP Requests
  HTTPHelper httpHelper;

  //Gson object for JSON parsing
  Gson gson;

  /**
   * Creates a new Runnable Helper
   * @param socket The socket connection wishing to make an API Request
   * @param channelList The main DataModel.MessageChannelList reference
   * */
  public DataServerAPIHelper(Socket socket, MessageChannelList channelList){
    this.socket = socket;
    this.channelList = channelList;
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
            if (response.contains(HTTP200)){
              out.write(response);
            }else {
              out.write(response);
              SuccessResponse successResponse = new SuccessResponse();
              successResponse.setSuccess(false);
              out.write(gson.toJson(successResponse));
            }
          }
        } else {
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
          response = starMessage(params);
        }else if (method.equals(channelStarMethod)){
          response = getChannelStarredHistory(params);
        }
      }
    }
    return response;
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
          response = buildHTTPResponse(json);
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
  public String starMessage(String params){
    String response = HTTP404;
    if (params != null){
      HashMap<String, String> urlParamsMap = httpHelper.parseURLParams(params);
      if (urlParamsMap != null && urlParamsMap.containsKey("messageid")){
        String messageid = urlParamsMap.get("messageid");
        if (messageid != null){
          boolean success = channelList.starMessage(messageid);
          StarMessageResponse starMessageResponse = new StarMessageResponse();
          starMessageResponse.setSuccess(success);
          String json = gson.toJson(starMessageResponse);
          response = buildHTTPResponse(json);
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
          Object[] channelHistory = channelList.getChannelHistory(channel);
          boolean success = channelHistory != null;
          String json = getChannelHistoryResponse(channelHistory, success);
          response = buildHTTPResponse(json);
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
          long messageID = channelList.postMessage(channel, text);
          String json = getPostMessageResponse(true, String.valueOf(messageID));
          response = buildHTTPResponse(json);
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
   * @return the JSON response for posting to a channel
   * */
  public String getPostMessageResponse(boolean success, String messageID){
    ChatPostMessageResponse chatPostMessageResponse = new ChatPostMessageResponse();
    chatPostMessageResponse.setSuccess(success);
    chatPostMessageResponse.setId(messageID);
    return gson.toJson(chatPostMessageResponse);
  }

  /**
   * Returns the full HTTP Response with full headers
   * */
  public String buildHTTPResponse(String json){
    return HTTP200 + CONTENT_TYPE + CONTENT_LENGTH + json.length() + "\n\r\n" + json;
  }
}