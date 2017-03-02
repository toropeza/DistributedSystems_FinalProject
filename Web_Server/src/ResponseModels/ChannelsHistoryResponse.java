package ResponseModels;

/**
 * GSON Class Client response for requesting a Channel's history
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

  public boolean isSuccess() {
    return success;
  }

  public Object[] getMessages() {
    return messages;
  }
}
