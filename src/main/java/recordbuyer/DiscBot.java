package recordbuyer;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlSpan;
import com.gargoylesoftware.htmlunit.html.HtmlStrong;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableCell;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;
import org.jasypt.util.text.StrongTextEncryptor;

public class DiscBot {
  static String version = "1.01";
  
  static String login = "login";
  
  static String password = "password";
  
  static int minimumMediaCondition = 2;
  
  static int sleepTime = 700;
  
  static boolean quietMode = false;
  
  static String localCurrency = "SEK";
  
  static WebClient webClient;
  
  public static void main(String[] args) {
    Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
    Logger.getLogger("org.apache.http").setLevel(Level.OFF);
    System.out.println("DiscBot v" + version + " - A Discogs shopping assistant");
    System.out.println();
    System.out.println("Contact: martin.arnsrud@gmail.com");
    System.out.println();
    (new Thread(new Runnable() {
          public void run() {
            Scanner sc = new Scanner(System.in);
            while (sc.hasNext()) {
              if (sc.nextLine().equals("q")) {
                System.out.println("Quiting...");
                DiscBot.logout();
                System.exit(0);
              } 
            } 
          }
        },  "KeyListener")).start();
    GlobalProperties.loadFromDisk();
    login = GlobalProperties.getProperty("login");
    password = GlobalProperties.getProperty("password");
    sleepTime = GlobalProperties.getIntegerProperty("sleep_time").intValue();
    quietMode = GlobalProperties.getBooleanProperty("quiet_mode").booleanValue();
    localCurrency = GlobalProperties.getProperty("local_currency");
    if ("eur".equals(localCurrency.toLowerCase())) {
      localCurrency = "€";
    } else if ("usd".equals(localCurrency.toLowerCase())) {
      localCurrency = "$";
    } else if ("gbp".equals(localCurrency.toLowerCase())) {
      localCurrency = "£";
    } 
    minimumMediaCondition = parseCondition(GlobalProperties.getProperty("minimum_media_condition"));
    webClient = new WebClient();
    webClient.getOptions().setThrowExceptionOnScriptError(false);
    webClient.getOptions().setJavaScriptEnabled(false);
    webClient.waitForBackgroundJavaScript(50000L);
    webClient.getOptions().setUseInsecureSSL(true);
    webClient.getOptions().setTimeout(5000);
    ArrayList<ReleaseItem> itemsInFile = FileLoader.loadItemFile("releases.txt");
    System.out.println("Number of items in releases.txt: " + itemsInFile.size());
    System.out.println("Autobuy enabled: " + GlobalProperties.getBooleanProperty("autobuy_enable"));
    System.out.println("Search monitor started.");
    System.out.println();
    System.out.println("Hit [q] + [enter] to exit");
    System.out.println();
    if (GlobalProperties.getBooleanProperty("wantlist_search").booleanValue())
      login(login, password); 
    while (true) {
      while (GlobalProperties.getBooleanProperty("wantlist_search").booleanValue()) {
        ArrayList<ReleaseItem> releasesMatchInWantlist = checkWantlist();
        systemOut("Number of items with sales listed in wantlist: " + releasesMatchInWantlist.size());
        for (ReleaseItem releaseInWantlist : releasesMatchInWantlist) {
          for (ReleaseItem itemInFile : itemsInFile) {
            if (itemInFile.getReleaseNumber().equals(releaseInWantlist.getReleaseNumber())) {
              systemOut("Checking out release: " + releaseInWantlist.getReleaseNumber());
              if (releaseInWantlist.getMaxPrice() <= itemInFile.getMaxPrice()) {
                systemOut("Release " + releaseInWantlist.getReleaseNumber() + " price OK.");
                String buyItem = getFirstMatchItemFromUrl(itemInFile.getUrl(), itemInFile.getMaxPrice());
                if (buyItem.length() > 0) {
                  systemOut("Release " + releaseInWantlist.getReleaseNumber() + " media condition OK.");
                  purchaseItem(buyItem);
                  continue;
                } 
                systemOut("Release " + releaseInWantlist.getReleaseNumber() + " does not match minimum media condition.");
                continue;
              } 
              systemOut("Release " + releaseInWantlist.getReleaseNumber() + " is too expensive. Price was " + releaseInWantlist
                  .getMaxPrice() + " and your max: " + itemInFile
                  .getMaxPrice());
            } 
          } 
        } 
        try {
          Thread.sleep(sleepTime);
        } catch (InterruptedException ex) {
          System.err.println("Error! Thread interrupted.");
        } 
      } 
      for (ReleaseItem item : itemsInFile) {
        String buyItem = getFirstMatchItemFromUrl(item.getUrl(), item.getMaxPrice());
        if (buyItem.length() > 0) {
          if (GlobalProperties.getBooleanProperty("autobuy_enable").booleanValue() && 
            GlobalProperties.getBooleanProperty("wantlist_search").booleanValue())
            login(login, password); 
          purchaseItem(buyItem);
          if (GlobalProperties.getBooleanProperty("autobuy_enable").booleanValue() && 
            GlobalProperties.getBooleanProperty("wantlist_search").booleanValue())
            logout(); 
        } 
        try {
          Thread.sleep(sleepTime);
        } catch (InterruptedException ex) {
          System.err.println("Error! Thread interrupted.");
        } 
      } 
    } 
  }
  
  public static void systemOut(String text) {
    if (!quietMode)
      System.out.println(text); 
  }
  
  public static ArrayList<ReleaseItem> checkWantlist() {
    ArrayList<ReleaseItem> releases = new ArrayList<>();
    try {
      HtmlPage wantlistPage = (HtmlPage)webClient.getPage("https://www.discogs.com/mywantlist?limit=250&page=1");
      List<?> numOfItemsSpan = wantlistPage.getByXPath("//strong[@class='pagination_total']");
      int numberOfWantlistItems = numOfItemsSpan.isEmpty() ? 0 : getNumberOfWantListItems(((HtmlStrong)numOfItemsSpan.get(0)).asXml());
      int numberOfWantlistPages = numberOfWantlistItems / 250 + 1;
      for (Object span : removeTotalWithShippingElements(wantlistPage.getByXPath("//span[@class='marketplace_for_sale_count']"))) {
        String responseXml = ((HtmlSpan)span).asXml();
        int begin = responseXml.indexOf("/sell/release/") + 14;
        int end = responseXml.indexOf("?", begin);
        String releaseString = responseXml.substring(begin, end);
        Double price = extractPriceFromXml(responseXml);
        releases.add(new ReleaseItem(releaseString, price.doubleValue()));
      } 
      if (numberOfWantlistItems > 250)
        for (int i = 2; i <= numberOfWantlistPages + 1; i++) {
          try {
            wantlistPage = (HtmlPage)webClient.getPage("https://www.discogs.com/mywantlist?limit=250&page=" + i);
            for (Object span : removeTotalWithShippingElements(wantlistPage.getByXPath("//span[@class='marketplace_for_sale_count']"))) {
              String responseXml = ((HtmlSpan)span).asXml();
              int begin = responseXml.indexOf("/sell/release/") + 14;
              int end = responseXml.indexOf("?", begin);
              String releaseString = responseXml.substring(begin, end);
              Double price = extractPriceFromXml(responseXml);
              releases.add(new ReleaseItem(releaseString, price.doubleValue()));
            } 
            Thread.sleep(sleepTime);
          } catch (Exception e) {
            System.err.println("Error while reading multiple wantlist pages. " + e);
          } 
        }  
      return releases;
    } catch (Exception e) {
      e.printStackTrace();
      return releases;
    } 
  }
  
  public static int getNumberOfWantListItems(String xml) {
    int begin = xml.indexOf(" of ") + 4;
    int end = xml.indexOf("<", begin);
    return Integer.valueOf(xml.substring(begin, end).replaceAll("\\.", "").replaceAll("\\,", "").trim()).intValue();
  }
  
  public static Double extractPriceFromXml(String xml) {
    int begin = xml.indexOf("price\">") + 7;
    int middle = firstNumeric(xml, begin);
    int end = xml.indexOf("<", middle);
    return Double.valueOf(xml.substring(middle, end).trim().replace(",", ""));
  }
  
  public static String getLocalCurrency(String xmlPriceSpan) {
    int begin = xmlPriceSpan.indexOf("price\">") + 7;
    int middle = firstNumeric(xmlPriceSpan, begin);
    for (int i = middle; i > 0; i--) {
      if (xmlPriceSpan.charAt(i) == '>') {
        begin = i + 1;
        break;
      } 
    } 
    return xmlPriceSpan.substring(begin, middle).trim();
  }
  
  public static final int firstNumeric(String s, int begin) {
    for (int i = begin; i < s.length(); i++) {
      char c = s.charAt(i);
      if (Character.isDigit(c))
        return i; 
    } 
    return -1;
  }
  
  public static void login(String user, String password) {
    try {
      HtmlPage firstPage = (HtmlPage)webClient.getPage("https://discogs.com");
      HtmlAnchor loginLink = firstPage.getByXPath("//a[@id='log_in_link']").get(0);
      HtmlPage loginPage = (HtmlPage)loginLink.click();
      HtmlForm form = loginPage.getByXPath("//form[@class='mb-3']").get(0);
      HtmlButton loginButton = form.getByXPath("//button[@type='submit']").get(0);
      HtmlTextInput textField = (HtmlTextInput)form.getInputByName("username");
      textField.setValueAttribute(login);
      HtmlPasswordInput textField2 = (HtmlPasswordInput)form.getInputByName("password");
      textField2.setValueAttribute(password);
      HtmlPage myDiscogsPage = (HtmlPage)loginButton.click();
      if ("Discogs - Dashboard".equals(myDiscogsPage.getTitleText())) {
        System.out.println("Logged in successfully!");
      } else {
        System.err.println("Error while logging in. Check username/password.");
        System.exit(0);
      } 
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("Error while logging in. Check username/password.");
    } 
  }
  
  public static void logout() {
    try {
      WebRequest logoutRequest1 = new WebRequest(new URL("https://www.discogs.com/logout"), HttpMethod.GET);
      webClient.getPage(logoutRequest1);
      WebRequest logoutRequest2 = new WebRequest(new URL("https://auth.discogs.com/logout?service=https://www.discogs.com/logout"), HttpMethod.GET);
      webClient.getPage(logoutRequest2);
    } catch (Exception ex) {
      ex.printStackTrace();
    } 
  }
  
  public static int parseCondition(String conditionDescription) {
    int condition = 1;
    if (conditionDescription.contains("(P)"))
      return 1; 
    if (conditionDescription.contains("(F)"))
      return 2; 
    if (conditionDescription.contains("(G)"))
      return 3; 
    if (conditionDescription.contains("(G+)"))
      return 4; 
    if (conditionDescription.contains("(VG)"))
      return 5; 
    if (conditionDescription.contains("(VG+)"))
      return 6; 
    if (conditionDescription.contains("(NM or M-)"))
      return 7; 
    if (conditionDescription.contains("(M)"))
      return 8; 
    return condition;
  }
}
