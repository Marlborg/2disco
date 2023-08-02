package recordbuyer;

public class ReleaseItem {
  public static final int CONDITION_POOR = 1;
  
  public static final int CONDITION_FAIR = 2;
  
  public static final int CONDITION_GOOD = 3;
  
  public static final int CONDITION_GOOD_PLUS = 4;
  
  public static final int CONDITION_VERY_GOOD = 5;
  
  public static final int CONDITION_VERY_GOOD_PLUS = 6;
  
  public static final int CONDITION_NEAR_MINT = 7;
  
  public static final int CONDITION_MINT = 8;
  
  private String url = "";
  
  private String releaseNumber = "";
  
  private double maxPrice = 0.0D;
  
  public ReleaseItem() {}
  
  public ReleaseItem(String releaseNumber, double price) {
    this.releaseNumber = releaseNumber;
    this.maxPrice = price;
  }
  
  public String getReleaseNumber() {
    if (this.releaseNumber.length() == 0 && this.url.contains("/release/")) {
      int begin = this.url.indexOf("/release/") + 9;
      int end = this.url.indexOf("?");
      return this.url.substring(begin, end);
    } 
    return this.releaseNumber;
  }
  
  public void setReleaseNumber(String itemNumber) {
    this.releaseNumber = itemNumber;
  }
  
  public double getMaxPrice() {
    return this.maxPrice;
  }
  
  public void setMaxPrice(double maxPrice) {
    this.maxPrice = maxPrice;
  }
  
  public String getUrl() {
    if (this.url.length() == 0 && this.releaseNumber.length() > 0)
      return "https://www.discogs.com/sell/release/" + this.releaseNumber; 
    return this.url;
  }
  
  public void setUrl(String url) {
    this.url = url;
  }
}
