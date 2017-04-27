package DataModel;

import java.util.logging.Logger;


/**
 * Message Channel List is a Thread safe collection of channel postings
 */
public class MessageChannelList {

  static final Logger logger = Logger.getLogger(MessageChannelList.class.getName());

  private String channelPosting;
  private long versionNumber;

  public MessageChannelList(){
    channelPosting = "empty";
    versionNumber = 0;
  }

  /**
   * Returns the most up-to-date version number. Will return the passed in version if up to date
   * @param currentVersion The version of the Web Server
   * @return The most up-to-date version number.
   * */
  public long freshVersion(long currentVersion){
    long upToDateVersion;
    long channelListVersion = versionNumber;
    if (currentVersion < channelListVersion){
      upToDateVersion = channelListVersion;
    } else {
      upToDateVersion = currentVersion;
    }
    return upToDateVersion;
  }

  /**
   * @return The version number
   */
  public long versionNumber(){
    long version;
    version = versionNumber;
    return version;
  }

  /**
   * Thread safe method for retrieving a channel's history
   * @return The Channel's History
   * */
  public String getChannelPosting(){
    return channelPosting;
  }

  /**
   * Thread safe method for posting a message
   * If the channel does not exist it will be created.
   * @param message The text to post
   * @return the id associated with the posting
   * */
  public long postMessage(String message){
    versionNumber++;
    channelPosting = message;
    return versionNumber;
  }


  public void setVersionNumber(long versionNumber){
    this.versionNumber = versionNumber;
  }
}
