import DataModel.DataServerList;
import DataModel.MessageChannelList;
import GsonModels.ResponseModels.ChannelsPostingResponse;
import GsonModels.ResponseModels.ChatPostMessageResponse;
import GsonModels.ResponseModels.SuccessResponse;
import Paxos.PaxosManager;
import com.google.gson.Gson;
import util.HTTPHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Runnable Class that parses the Requests coming into the MessageServerAPI
 * */
public class DataServerAPIHelper implements Runnable {

  static final Logger logger = Logger.getLogger(DataServer.class.getName());

  private final int ELECTION_TIMEOUT = 5000;

  //API Methods
  private final String postMessageMethod = "chat.postMessage";
  private final String channelPostingMethod = "channels.posting";

  //Socket connection for the request
  private Socket socket;

  //List of channels with postings
  private MessageChannelList channelList;

  //HTTP Helper for parsing HTTP Requests
  util.HTTPHelper httpHelper;

  //List of all DataServers
  DataServerList dataServers;

  //Gson object for JSON parsing
  Gson gson;

  //used for agreeing on values with other nodes
  PaxosManager paxosManager;

  /**
   * Creates a new Runnable Helper
   * @param socket The socket connection wishing to make an API Request
   * @param channelList The main DataModel.MessageChannelList reference
   * */
  public DataServerAPIHelper(Socket socket, MessageChannelList channelList, DataServerList dataServers, PaxosManager paxosManager){
    this.socket = socket;
    this.channelList = channelList;
    this.dataServers = dataServers;
    this.paxosManager = paxosManager;
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
          String response;
          if (request.contains("/api/paxos")){
            response = paxosManager.parsePaxosRequest(request);
          }else {
            response = parseAPIRequest(request);
          }
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
        }
      }else {
        //methods with no parameters
        if (method.equals(channelPostingMethod)){
          response = returnChannelPosting();
        }
      }
    }
    return response;
  }

  /**
   * Returns the JSON Response for retrieving the channel history
   * @return The JSON Response
   * */
  public String returnChannelPosting(){
    String response = httpHelper.HTTP404;
    String channelPosting = paxosManager.sendReadRequest().toString();;
    boolean success = !channelPosting.equals("empty");
    String json = getChannelPostingResponse(channelPosting, success);
    response = httpHelper.buildHTTPResponse(json);
    return response;
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
      if (urlParamsMap != null && urlParamsMap.containsKey("text")){
        String text = urlParamsMap.get("text");
        if (text != null){
          boolean success = true;
          boolean consensus = paxosManager.sendWriteRequest(text);
          if (consensus){
            long messageID = channelList.postMessage(text);
            logger.info("Posted message to channel");
            String json = getPostMessageResponse(success, String.valueOf(messageID));
            response = httpHelper.buildHTTPResponse(json);
          }else {
            logger.info("Could not reach consensus");
          }
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
   * Returns the JSON response for retrieving a channel's history
   * @return the JSON response for retrieving a channel's history
   * */
  public String getChannelPostingResponse(String posting, boolean success){
    ChannelsPostingResponse channelsHistoryResponse = new ChannelsPostingResponse();

    channelsHistoryResponse.setSuccess(success);
    channelsHistoryResponse.setPosting(posting);
    return gson.toJson(channelsHistoryResponse);
  }
}