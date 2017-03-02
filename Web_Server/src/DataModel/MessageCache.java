package DataModel;

import util.ReadWriteLock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Message Channel List is a Thread safe collection of channel postings
 */
public class MessageCache {

  private final ReadWriteLock readWriteLock;
  private final ConcurrentHashMap<String, List<ChannelPosting>> channelPostings;
  private long versionNumber;

  public MessageCache(){
    readWriteLock = new ReadWriteLock();
    channelPostings = new ConcurrentHashMap<>();
    versionNumber = 0;
  }

  /**
   * Thread safe method for retrieving a channel's starred history
   * @return The Channel's Starred History
   * */
  public Object[] getChannelStarredHistory(String channelName){
    Object[] starredChannelHistory = null;
    readWriteLock.lockRead();
    if (channelPostings.containsKey(channelName)){
      //Return new reference to release read lock
      starredChannelHistory = channelPostings.get(channelName).toArray();
      ArrayList<Object> starredPostingList = new ArrayList<>();
      for (Object object: starredChannelHistory){
        ChannelPosting posting = (ChannelPosting) object;
        starredPostingList.add(posting);
      }
      starredChannelHistory = starredPostingList.toArray();
    }
    readWriteLock.unlockRead();
    return starredChannelHistory;
  }

  public void updateCache(long newVersion, String channel, ChannelPosting[] freshData){
    setVersionNumber(newVersion);
    ArrayList<ChannelPosting> newStarredPostings = new ArrayList<>();
    for (ChannelPosting channelPosting: freshData){
      newStarredPostings.add(channelPosting);
    }
    readWriteLock.lockWrite();
    channelPostings.put(channel, newStarredPostings);
    readWriteLock.unlockWrite();
  }

  /**
   * Thread safe method for accessing the cache Version number
   * */
  public synchronized long getVersionNumber() {
    return versionNumber;
  }

  /**
   * Thread safe method for setting the cache Version number
   * */
  public synchronized void setVersionNumber(long versionNumber) {
    this.versionNumber = versionNumber;
  }
}
