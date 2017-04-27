package Paxos.PaxosResponseModels;

/**
 * Class describing the information in a Prepare Response
 */
public class PrepareResponse {

  boolean promise;

  public boolean promised() {
    return promise;
  }

  public void setPromise(boolean promise) {
    this.promise = promise;
  }
}
