package Paxos.PaxosResponseModels;

/**
 * Class describing the information in a Prepare Response
 */
public class PrepareResponse {

  boolean promise;
  Object prevValue;

  public Object getPrevValue() {
    return prevValue;
  }

  public void setPrevValue(Object prevValue) {
    this.prevValue = prevValue;
  }

  public boolean promised() {
    return promise;
  }

  public void setPromise(boolean promise) {
    this.promise = promise;
  }
}
