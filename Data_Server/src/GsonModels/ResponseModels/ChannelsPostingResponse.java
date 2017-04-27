package GsonModels.ResponseModels;

/**
 * Created by thomasoropeza on 2/20/17.
 */
public class ChannelsPostingResponse {

  private boolean success;
  private String posting;

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public void setPosting(String posting) {
    this.posting = posting;
  }
}
