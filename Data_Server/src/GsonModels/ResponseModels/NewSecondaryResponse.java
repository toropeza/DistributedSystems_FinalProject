package GsonModels.ResponseModels;

import DataModel.ChannelPosting;
import DataModel.DataServerInfo;
import DataModel.WebServerInfo;

import java.util.List;
import java.util.Map;

/**
 * Created by thomasoropeza on 4/4/17.
 */
public class NewSecondaryResponse {
  private boolean success;
  private Map<String, List<ChannelPosting>> database;
  private List<WebServerInfo> webServers;
  private List<DataServerInfo> dataServers;

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public Map<String, List<ChannelPosting>> getDatabase() {
    return database;
  }

  public void setDatabase(Map<String, List<ChannelPosting>> database) {
    this.database = database;
  }

  public List<WebServerInfo> getWebServers() {
    return webServers;
  }

  public void setWebServers(List<WebServerInfo> webServers) {
    this.webServers = webServers;
  }

  public List<DataServerInfo> getDataServers() {
    return dataServers;
  }

  public void setDataServers(List<DataServerInfo> dataServers) {
    this.dataServers = dataServers;
  }
}
