package Paxos.PaxosResponseModels;

/**
 * Created by thomasoropeza on 5/11/17.
 */
public class CommittedResponse {

  boolean committed;

  public boolean isCommitted() {
    return committed;
  }

  public void setCommitted(boolean committed) {
    this.committed = committed;
  }
}
