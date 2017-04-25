package DataModel;

/**
 * Information describing a channel's message posting
 */
public class ChannelPosting {

  private String text;
  private long id;

  public ChannelPosting(long id, String text) {
    this.id = id;
    this.text = text;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }
}
