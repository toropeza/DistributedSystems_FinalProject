package GsonModels.ResponseModels;

import DataModel.ChannelPosting;

import java.util.List;
import java.util.Map;

/**
 * Created by thomasoropeza on 2/27/17.
 */
public class UpdateDataResponse {


  private boolean success;
  private String version;
  private Map<String, List<ChannelPosting>> data;

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public Map<String, List<ChannelPosting>> getData() {
    return data;
  }

  public void setData(Map<String, List<ChannelPosting>> data) {
    this.data = data;
  }
}
