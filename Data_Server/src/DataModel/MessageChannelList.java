package DataModel;

import java.util.logging.Logger;


/**
 * Message Channel List is a Thread safe collection of channel postings
 */
public class MessageChannelList {

  static final Logger logger = Logger.getLogger(MessageChannelList.class.getName());

  private String channelPosting;

  public MessageChannelList(){
    channelPosting = "empty";
  }

  /**
   * @param message The text to post
   * */
  public void postMessage(String message){
    channelPosting = message;
  }
}
