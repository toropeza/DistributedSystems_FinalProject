package GsonModels.ResponseModels;

import DataModel.ChannelPosting;
import DataModel.ServerInfo;

import java.util.List;
import java.util.Map;

/**
 * Created by thomasoropeza on 4/4/17.
 */
public class NewSecondaryResponse {
  private boolean success;
  private Map<String, List<ChannelPosting>> database;
  private List<ServerInfo> webServers;
  private List<ServerInfo> dataServers;
  private long versionNumber;

  public long getVersionNumber() {
    return versionNumber;
  }

  public void setVersionNumber(long versionNumber) {
    this.versionNumber = versionNumber;
  }

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

  public List<ServerInfo> getWebServers() {
    return webServers;
  }

  public void setWebServers(List<ServerInfo> webServers) {
    this.webServers = webServers;
  }

  public List<ServerInfo> getDataServers() {
    return dataServers;
  }

  public void setDataServers(List<ServerInfo> dataServers) {
    this.dataServers = dataServers;
  }
}
