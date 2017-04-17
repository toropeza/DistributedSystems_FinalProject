package DataModel;

import util.ReadWriteLock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Message Channel List is a Thread safe collection of channel postings
 */
public class MessageChannelList {

  private final ReadWriteLock readWriteLock;
  private ConcurrentHashMap<String, List<ChannelPosting>> channelPostings;
  private long messageIDCount;
  private long versionNumber;

  public MessageChannelList(){
    readWriteLock = new ReadWriteLock();
    channelPostings = new ConcurrentHashMap<>();
    messageIDCount = 0;
    versionNumber = 0;
  }

  /**
   * Applies the given database.
   * @param channelPostings The new database to add
   */
  public void addDatabase(Map<String, List<ChannelPosting>> channelPostings){
    readWriteLock.lockWrite();
    this.channelPostings.putAll(channelPostings);
    readWriteLock.unlockWrite();
  }

  /**
   * Sets the given database.
   * @param channelPostings The new database to add
   */
  public void setDatabase(Map<String, List<ChannelPosting>> channelPostings){
    readWriteLock.lockWrite();
    this.channelPostings = new ConcurrentHashMap<>();
    channelPostings.putAll(channelPostings);
    readWriteLock.unlockWrite();
  }

  /**
   * Returns the most up-to-date version number. Will return the passed in version if up to date
   * @param currentVersion The version of the Web Server
   * @return The most up-to-date version number.
   * */
  public long freshVersion(long currentVersion){
    long upToDateVersion;
    readWriteLock.lockRead();
    long channelListVersion = versionNumber;
    readWriteLock.unlockRead();
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
    readWriteLock.lockRead();
    version = versionNumber;
    readWriteLock.unlockRead();
    return version;
  }

  /**
   * Thread safe method for retrieving a channel's history
   * @return The Channel's History
   * */
  public Object[] getChannelHistory(String channelName){
    Object[] channelHistory = null;
    readWriteLock.lockRead();
    if (channelPostings.containsKey(channelName)){
      List<ChannelPosting> copiedHistory = deepCopyHistory(channelName);
      channelHistory = copiedHistory.toArray();
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
      List<ChannelPosting> starredHistoryList = deepCopyStarredHistory(channelName);
      starredChannelHistory = starredHistoryList.toArray();
    }
    readWriteLock.unlockRead();
    return starredChannelHistory;
  }

  /**
   * Thread safe method for retrieving a channel's database
   * @return The Channel's database
   * */
  public Map<String, List<ChannelPosting>> getDatabase(){
    Map<String, List<ChannelPosting>> database = null;
    readWriteLock.lockRead();
    database = deepCopyDatabase();
    readWriteLock.unlockRead();
    return database;
  }

  /**
   * Returns a deep copy of the database for thread safety
   * Used to reply to a spawning secondary server
   * @return The deep copy of the database structure
   */
  private Map<String, List<ChannelPosting>> deepCopyDatabase(){
    HashMap<String, List<ChannelPosting>> copiedDatabase = new HashMap<>();
    for (String channelName: channelPostings.keySet()){
      copiedDatabase.put(channelName, deepCopyHistory(channelName));
    }
    return copiedDatabase;
  }

  /**
   * Returns a deep copy of the channel's history for thread safety
   * Used to reply to a spawning secondary server
   * @param channelName The channel to copy
   * @return The deep copy of the channel's history
   */
  private List<ChannelPosting> deepCopyHistory(String channelName){
    ArrayList<ChannelPosting> copiedHistory = new ArrayList<>();
    List<ChannelPosting> referencedHistory = channelPostings.get(channelName);
    for (ChannelPosting channelPosting: referencedHistory){
      long id = channelPosting.getId();
      String text = channelPosting.getText();
      ChannelPosting copiedPosting = new ChannelPosting(id, text);
      copiedHistory.add(copiedPosting);
    }
    return copiedHistory;
  }

  /**
   * Returns a deep copy of the channel's starred history for thread safety
   * Used to reply to a spawning secondary server
   * @param channelName The channel to copy
   * @return The deep copy of the channel's starred history
   */
  private List<ChannelPosting> deepCopyStarredHistory(String channelName){
    List<ChannelPosting> starredHistoryList = new ArrayList<>();
    List<ChannelPosting> referencedHistory = channelPostings.get(channelName);
    for (ChannelPosting referencedPosting: referencedHistory){
      if (referencedPosting.isStarred()){
        long id = referencedPosting.getId();
        String text = referencedPosting.getText();
        ChannelPosting copiedPosting = new ChannelPosting(id, text);
        starredHistoryList.add(copiedPosting);
      }
    }
    return starredHistoryList;
  }

  /**
   * Thread safe method for posting to a given channel.
   * If the channel does not exist it will be created.
   * @param channel The channel to post to
   * @param message The text to post
   * @return the id associated with the posting
   * */
  public long postMessage(String channel, String message){
    readWriteLock.lockWrite();
    messageIDCount++;
    versionNumber++;
    if (channelPostings.containsKey(channel)){
      ChannelPosting posting = new ChannelPosting(messageIDCount, message);
      channelPostings.get(channel).add(posting);
    }else {
      List<ChannelPosting> channelList = Collections.synchronizedList(new ArrayList<>());
      ChannelPosting posting = new ChannelPosting(messageIDCount, message);
      channelList.add(posting);
      channelPostings.put(channel, channelList);
    }
    readWriteLock.unlockWrite();
    return messageIDCount;
  }

  /**
   * Thread safe method for starring a message
   * If the message does not exist, will return false
   * @param messageId The id of the message to star
   * @return Whether the message was successfully starred or not
   * */
  public boolean starMessage(String messageId){
    boolean starredSuccessful = false;
    readWriteLock.lockWrite();
    versionNumber++;
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
}
