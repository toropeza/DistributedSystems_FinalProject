package ResponseModels;

/**
 * GSON Class Client response for posting a message
 */
public class ChatPostMessageResponse {

  private boolean success;
  private String id;

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public void setId(String id) {
    this.id = id;
  }
}
