package recordbuyer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

public class FileLoader {
  public static ArrayList<ReleaseItem> loadItemFile(String filename) {
    ArrayList<ReleaseItem> items = new ArrayList<>();
    try {
      BufferedReader br = new BufferedReader(new FileReader(filename));
      try {
        String line;
        while ((line = br.readLine()) != null) {
          if (line.startsWith("#"))
            continue; 
          String[] lineColumns = line.split(";");
          if (lineColumns.length == 2) {
            ReleaseItem r = new ReleaseItem();
            if (lineColumns[0].startsWith("https")) {
              r.setUrl(lineColumns[0]);
            } else {
              r.setReleaseNumber(lineColumns[0]);
            } 
            int maxPrice = Integer.valueOf(lineColumns[1]).intValue();
            r.setMaxPrice(maxPrice);
            items.add(r);
          } 
        } 
        br.close();
      } catch (Throwable throwable) {
        try {
          br.close();
        } catch (Throwable throwable1) {
          throwable.addSuppressed(throwable1);
        } 
        throw throwable;
      } 
    } catch (Exception e) {
      System.err.println("Error while reading " + filename + "!");
    } 
    return items;
  }
}
