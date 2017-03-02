package GsonModels.ResponseModels;

/**
 * Created by thomasoropeza on 2/20/17.
 */
public class ChannelsHistoryResponse {

  private boolean success;
  private Object[] messages;

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public void setMessages(Object[] messages) {
    this.messages = messages;
  }
}
