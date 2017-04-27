package Paxos.PaxosResponseModels;

/**
 * Class describing the information in a Prepare Response
 */
public class ReadResponse {

  int sequenceNum;
  Object value;

  public int getSequenceNum() {
    return sequenceNum;
  }

  public void setSequenceNum(int sequenceNum) {
    this.sequenceNum = sequenceNum;
  }

  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }
}
