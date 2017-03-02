package DataModel;

import util.ReadWriteLock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Message Channel List is a Thread safe collection of channel postings
 */
public class MessageChannelList {

  private final ReadWriteLock readWriteLock;
  private final ConcurrentHashMap<String, List<ChannelPosting>> channelPostings;
  private long messageIDCount;
  private long versionNumber;

  public MessageChannelList(){
    readWriteLock = new ReadWriteLock();
    channelPostings = new ConcurrentHashMap<>();
    messageIDCount = 0;
    versionNumber = 0;
  }

  /**
   * Returns the most up-to-date version number. Will return the passed in version if up to date
   * @param currentVersion The version of the Web Server
   * @return The most up-to-date version number.
   * */
  public long freshVersion(long currentVersion){
    long upToDateVersion;
    long channelListVersion = getVersionNumber();
    if (currentVersion < channelListVersion){
      upToDateVersion = channelListVersion;
    }else {
      upToDateVersion = currentVersion;
    }
    return upToDateVersion;
  }

  /**
   * Thread safe method for retrieving a channel's history
   * @return The Channel's History
   * */
  public Object[] getChannelHistory(String channelName){
    Object[] channelHistory = null;
    readWriteLock.lockRead();
    if (channelPostings.containsKey(channelName)){
      //Return thread safe array
      channelHistory = channelPostings.get(channelName).toArray();
    }
    readWriteLock.unlockRead();
    return channelHistory;
  }

  /**
   * Thread safe method for retrieving a channel's starred history
   * @return The Channel's Starred History
   * */
  public Object[] getChannelStarredHistory(String channelName){
    Object[] starredChannelHistory = null;
    readWriteLock.lockRead();
    if (channelPostings.containsKey(channelName)){
      //Return thread safe array
      ArrayList<Object> starredHistoryList = new ArrayList<>();
      Object[] channelHistory = channelPostings.get(channelName).toArray();
      for (Object object: channelHistory){
        ChannelPosting posting = (ChannelPosting) object;
        if (posting.isStarred()){
          starredHistoryList.add(posting);
        }
      }
      starredChannelHistory = starredHistoryList.toArray();
    }
    readWriteLock.unlockRead();
    return starredChannelHistory;
  }

  /**
   * Thread safe method for posting to a given channel.
   * If the channel does not exist it will be created.
   * @param channel The channel to post to
   * @param message The text to post
   * @return the id associated with the posting
   * */
  public long postMessage(String channel, String message){
    incrementIDCount();
    incrementVersionNumber();
    readWriteLock.lockWrite();
    long channelListIDCount = getMessageIDCount();
    if (channelPostings.containsKey(channel)){
      ChannelPosting posting = new ChannelPosting(channelListIDCount, message);
      channelPostings.get(channel).add(posting);
    }else {
      List<ChannelPosting> channelList = Collections.synchronizedList(new ArrayList<>());
      ChannelPosting posting = new ChannelPosting(channelListIDCount, message);
      channelList.add(posting);
      channelPostings.put(channel, channelList);
    }
    readWriteLock.unlockWrite();
    return channelListIDCount;
  }

  /**
   * Thread safe method for starring a message
   * If the message does not exist, will return false
   * @param messageId The id of the message to star
   * @return Whether the message was successfully starred or not
   * */
  public boolean starMessage(String messageId){
    incrementVersionNumber();
    boolean starredSuccessful = false;
    readWriteLock.lockWrite();
    for (String channel: channelPostings.keySet()){
      for (ChannelPosting channelPosting: channelPostings.get(channel)){
        if (String.valueOf(channelPosting.getId()).equals(messageId)){
          channelPosting.setStarred(true);
          starredSuccessful = true;
          break;
        }
      }
      if (starredSuccessful){
        break;
      }
    }
    readWriteLock.unlockWrite();
    return starredSuccessful;
  }

  /**
   * Thread safe method for accessing the message ID Count
   * */
  public synchronized long getMessageIDCount() {
    return messageIDCount;
  }

  /**
   * Thread safe method for incrementing message ID Count
   * */
  public synchronized void incrementIDCount() {
    this.messageIDCount++;
  }

  /**
   * Thread safe method for accessing the DB Version number
   * */
  public synchronized long getVersionNumber() {
    return versionNumber;
  }

  /**
   * Thread safe method for incrementing the DB Version number
   * */
  public synchronized void incrementVersionNumber() {
    this.versionNumber++;
  }
}
