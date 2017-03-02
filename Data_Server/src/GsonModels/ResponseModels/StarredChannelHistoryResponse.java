package GsonModels.ResponseModels;

import DataModel.ChannelPosting;

import java.util.List;

/**
 * Created by thomasoropeza on 2/27/17.
 */
public class StarredChannelHistoryResponse {


  private boolean success;
  private String version;
  private Object[] messages;

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public void setMessages(Object[] messages) {
    this.messages = messages;
  }
}
