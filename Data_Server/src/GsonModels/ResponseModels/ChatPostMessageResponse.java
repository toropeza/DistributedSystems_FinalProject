package GsonModels.ResponseModels;

/**
 * Created by thomasoropeza on 2/20/17.
 */
public class ChatPostMessageResponse {

  private boolean success;
  private String id;

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public void setId(String id) {
    this.id = id;
  }
}
