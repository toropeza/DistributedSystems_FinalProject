import DataModel.ServerInfo;
import ResponseModels.SuccessResponse;
import com.google.gson.Gson;
import util.RequestBuilder;

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
public class WebServerAPIHelper implements Runnable {

  static final Logger logger = Logger.getLogger(WebServer.class.getName());

  //HTTP Response Codes
  private final String HTTP200 = "HTTP/1.1 200 OK\n";
  private final String HTTP400 = "HTTP/1.1 400 Bad Request \r\n";
  private final String HTTP404 = "HTTP/1.1 404 Not Found \r\n";
  private final String HTTP405 = "HTTP/1.1 405 Method Not Allowed \r\n";
  private final String CONTENT_TYPE = "Content-Type: application/json \n";
  private String CONTENT_LENGTH = "Content-Length:";

  //Socket connection for the request
  private Socket socket;

  //HTTP Helper for parsing HTTP Requests
  HTTPHelper httpHelper;

  //Gson object for JSON parsing
  Gson gson;

  //Data Server Configuration
  String dataServerPort;
  String dataServerIP;

  //Builds Requests for the API
  RequestBuilder requestBuilder;

  /**
   * Creates a new Runnable Helper
   * @param socket The socket connection wishing to make an API Request
   * @param dataServerPort port for data server running on the same ip
   * @param dataServerIP The IP Adress of the server running the Data Server
   * */
  public WebServerAPIHelper(Socket socket, String dataServerPort, String dataServerIP){
    this.socket = socket;
    this.dataServerPort = dataServerPort;
    this.dataServerIP = dataServerIP;
    gson = new Gson();
    httpHelper = new HTTPHelper();
    requestBuilder = new RequestBuilder(new ServerInfo(dataServerIP, Integer.valueOf(dataServerPort)));
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
      String method = methodAndParams[0];
      if (methodAndParams.length == 2){
        String params = methodAndParams[1];
        if (method.equals(RequestBuilder.postMessageMethod)){
          response = postMessageToChannel(params);
        }
      }else {
        if (method.equals(RequestBuilder.channelPostingMethod)){
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
    String response = HTTP404;
    try {
      String channelHistoryRequest = requestBuilder.buildDSChannelHistoryRequest();
      String json = httpHelper.performHttpGet(channelHistoryRequest);
      response = buildHTTPResponse(json);
    }catch (Exception e){
      SuccessResponse successResponse = new SuccessResponse();
      successResponse.setSuccess(false);
      response = buildHTTPResponse(gson.toJson(successResponse));
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
      if (urlParamsMap != null && urlParamsMap.containsKey("text")){
        String text = urlParamsMap.get("text");
        if (text != null){
          try {
            String postMessageRequest = requestBuilder.buildDSPostMessageRequest(text);
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
   * Returns the full HTTP Response with headers
   * */
  public String buildHTTPResponse(String json){
    return HTTP200 + CONTENT_TYPE + CONTENT_LENGTH + json.length() + "\n\r\n" + json;
  }
}