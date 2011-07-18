package edu.umass.cs.wallach.cluster;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import gnu.trove.*;

public class StateLoader {

  public static int load(String fileName, int[][] z, TIntIntHashMap[] counts) {

    int currentDoc = -1;
    int currentPosition = 0;
    int maxFeature = 0;

    assert !((counts == null) && (z == null));

    if (z != null)
      for (int d=0; d<z.length; d++)
        Arrays.fill(z[d], -1);

    if (counts != null)
      for (int d=0; d<counts.length; d++)
        counts[d] = null;

    try {

      BufferedReader in = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(new File(fileName)))));

      String line = null;

      while ((line = in.readLine()) != null) {
        if (line.startsWith("#"))
          continue;

        String[] fields = line.split("\\s+");

        // each line consists of doc source X X X feature

        if (fields.length != 6)
          continue;

        int docIndex = Integer.parseInt(fields[0]);
        int feature = Integer.parseInt(fields[5]);

        if (feature > maxFeature)
          maxFeature = feature;

        if (docIndex != currentDoc) {
          currentDoc = docIndex;
          currentPosition = 0;

          if (counts != null)
            counts[currentDoc] = new TIntIntHashMap();
        }

        if (z != null)
          z[currentDoc][currentPosition] = feature;

        if (counts != null) {

          TIntIntHashMap item = counts[currentDoc];

          if (item.containsKey(feature))
            item.put(feature, item.get(feature) + 1);
          else
            item.put(feature, 1);
        }

        currentPosition++;
      }

      in.close();
    }
    catch (IOException e) {
      System.out.println(e);
    }

    if (z != null) {
      for (int d=0; d<z.length; d++) {
        for (int i=0; i<z[d].length; i++) {
          if (z[d][i] == -1)
            System.out.println("Error loading item " + d);

          assert z[d][i] != -1;
        }
      }
    }

    if (counts != null) {
      for (int d=0; d<counts.length; d++) {
        if (counts[d] == null)
          System.out.println("Error loading item " + d);

        assert counts[d] != null;
      }
    }

    return maxFeature + 1;
  }
}
