import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;

/**
 * Runnable Class that parses the Requests coming into the MessageServerAPI
 * */
public class MessageServerAPIHelper implements Runnable {

  //HTTP Response Codes
  private final String HTTP200 = "HTTP-1.0 200 OK \r\n";
  private final String HTTP400 = "HTTP-1.0 400 Bad Request \r\n";
  private final String HTTP404 = "HTTP-1.0 404 Not Found \r\n";
  private final String HTTP405 = "HTTP-1.0 405 Method Not Allowed \r\n";

  //API Methods
  private final String postMessageMethod = "chat.postMessage";
  private final String channelHistoryMethod = "channels.history";

  //Socket connection for the request
  private Socket socket;

  //List of channels with postings
  private MessageChannelList channelList;

  //HTTP Helper for parsing HTTP Requests
  HTTPHelper httpHelper;

  /**
   * Creates a new Runnable Helper
   * @param socket The socket connection wishing to make an API Request
   * @param channelList The main MessageChannelList reference
   * */
  public MessageServerAPIHelper(Socket socket, MessageChannelList channelList){
    this.socket = socket;
    this.channelList = channelList;
    httpHelper = new HTTPHelper();
  }

  @Override
  public void run() {
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

      String line = in.readLine();
      if (httpHelper.isHTTPGET(line)){
        String request = httpHelper.getResource(line);
        String response = parseAPIRequest(request);
        if (response != null){
          if (response.contains(HTTP200)){
            out.write(response);
          }else {
            out.write(response);
            out.write(getSuccessResponse(false));
          }
        }
      }else {
        out.write(HTTP405);
      }
      out.flush();
      socket.close();
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
        }
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
          response = HTTP200 + getChannelHistoryResponse(channelHistory, success);
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
    StringBuffer buffer = new StringBuffer();
    buffer.append("{");
    buffer.append("\"success\":" + success + ",");
    buffer.append("\"messages\":[");
    if (success){
      buffer.append("\"");
      buffer.append(channelHistory[0].toString().replaceAll("%20", " "));
      buffer.append("\"");
      for (int i = 1; i < channelHistory.length; i++){
        buffer.append(",");
        buffer.append("\"");
        buffer.append(channelHistory[i].toString().replaceAll("%20", " "));
        buffer.append("\"");
      }
    }
    buffer.append("]");
    buffer.append("}\n");
    return buffer.toString();
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
          channelList.postMessage(channel, text);
          response = HTTP200 + getSuccessResponse(true);
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
  public String getSuccessResponse(boolean success){
    StringBuffer buffer = new StringBuffer();
    buffer.append("{");
    buffer.append("\"success\":" + success + "");
    buffer.append("}\n");
    return buffer.toString();
  }
}