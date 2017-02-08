import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Message Channel List is a Thread safe collection of channel postings
 */
public class MessageChannelList {

  ConcurrentHashMap<String, List<String>> channelPostings;

  public MessageChannelList(){
    channelPostings = new ConcurrentHashMap<>();
  }

  /**
   * Thread safe method for retrieving a channel's history
   * @return The Channel's History
   * */
  public List<String> getChannelHistory(String channelName){
    if (channelPostings.containsKey(channelName)){
      return channelPostings.get(channelName);
    }else {
      return null;
    }
  }

  /**
   * Thread safe method for posting to a given channel.
   * If the channel does not exist it will be created.
   * @param channel The channel to post to
   * @param message The message to post
   * */
  public void postMessage(String channel, String message){
    if (channelPostings.containsKey(channel)){
      channelPostings.get(channel).add(message);
    }else {
      List<String> channelList = Collections.synchronizedList(new ArrayList<>());
      channelList.add(message);
      channelPostings.put(channel, channelList);
    }
  }
}
