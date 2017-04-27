package util;

import DataModel.ServerInfo;

/**
 * Builds Web Server Requests
 */
public class RequestBuilder {

  public final String dataServerBaseURL;

  //API
  public static final String postMessageMethod = "chat.postMessage";
  public static final String channelHistoryMethod = "channels.history";
  public static final String messageStarMethod = "message.star";
  public static final String channelStarMethod = "channels.star";


  public RequestBuilder(ServerInfo dataServer){
    dataServerBaseURL = "http://" + dataServer.getIp() + ":" + dataServer.getPort() + "/api/";
  }

  public String buildNewWebServerRequest(ServerInfo dataServer){
    return "";
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
}
