package ResponseModels;

import DataModel.ChannelPosting;

/**
 * GSON Class Data Server response when requesting starred history
 */
public class DSChannelsStarResponse {

  private boolean success;
  private String version;
  private ChannelPosting[] messages;

  public boolean isSuccess() {
    return success;
  }

  public String getVersion() {
    return version;
  }

  public ChannelPosting[] getMessages() {
    return messages;
  }
}
