package DataModel;

/**
 * Class wrapping the information for a channel posting
 */
public class ChannelPosting {

  private String text;
  private transient boolean starred;
  private long id;

  public ChannelPosting(long id, String text) {
    this.id = id;
    this.text = text;
    this.starred = false;
  }

  public void setStarred(boolean starred) {
    this.starred = starred;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public boolean isStarred() {
    return starred;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }
}
