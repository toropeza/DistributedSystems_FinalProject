package util;

import DataModel.ServerInfo;

/**
 * Builds Web Server Requests
 */
public class RequestBuilder {

  public final String dataServerBaseURL;

  //API
  public static final String postMessageMethod = "chat.postMessage";
  public static final String channelPostingMethod = "channels.posting";


  public RequestBuilder(ServerInfo dataServer){
    dataServerBaseURL = "http://" + dataServer.getIp() + ":" + dataServer.getPort() + "/api/";
  }

  /**
   * Build a GET request for the Data Server to retrieve the given channel's posting
   * */
  public String buildDSChannelHistoryRequest(){
    return dataServerBaseURL + channelPostingMethod;
  }

  /**
   * Build a GET request for posting a message to the given channel
   * @param text The text to post
   * */
  public String buildDSPostMessageRequest(String text){
    String args = "?text=" + text;
    return dataServerBaseURL + postMessageMethod + args;
  }
}
