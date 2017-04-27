package Paxos.PaxosResponseModels;

/**
 * Class describing the information in a Prepare Response
 */
public class AcceptResponse {

  boolean accepted;

  public boolean accepted() {
    return accepted;
  }

  public void setAccepted(boolean accepted) {
    this.accepted = accepted;
  }
}
