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
      //Deep Copy
      List<ChannelPosting> referencedhistory = channelPostings.get(channelName);
      ArrayList<Object> starredPostingList = new ArrayList<>();
      for (ChannelPosting referencedPosting: referencedhistory){
        long id = referencedPosting.getId();
        String text = referencedPosting.getText();
        ChannelPosting copiedChannelPosting = new ChannelPosting(id, text);
        starredPostingList.add(copiedChannelPosting);
      }
      starredChannelHistory = starredPostingList.toArray();
    }
    readWriteLock.unlockRead();
    return starredChannelHistory;
  }

  public void updateCache(long newVersion, String channel, ChannelPosting[] freshData){
    ArrayList<ChannelPosting> newStarredPostings = new ArrayList<>();
    for (ChannelPosting channelPosting: freshData){
      newStarredPostings.add(channelPosting);
    }
    readWriteLock.lockWrite();
    versionNumber = newVersion;
    channelPostings.put(channel, newStarredPostings);
    readWriteLock.unlockWrite();
  }

  public long getVersionNumber(){
    long version;
    readWriteLock.lockRead();
    version = versionNumber;
    readWriteLock.unlockRead();
    return version;
  }
}
