package edu.umass.cs.wallach.cluster;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import gnu.trove.*;

public class ExtractTopFeatures {

  private TIntObjectHashMap alphabet;

  private int numTopFeatures;

  public ExtractTopFeatures(int numTopFeatures) {

    this.numTopFeatures = numTopFeatures;

    alphabet = new TIntObjectHashMap();
  }

  public void processFile(String inputFileName, String featuresFileName) {

    try {

      BufferedReader in = new BufferedReader(new FileReader(featuresFileName));

      String line = null;
      String[] fields = null;

      int numFeaturesProcessed = 0;

      while ((line = in.readLine()) != null) {

        fields = line.split("\\s+");

        assert fields[0].equals("Feature");

        int t = Integer.parseInt(fields[1].substring(0, fields[1].length()-1));

        assert numFeaturesProcessed == t;

        alphabet.put(t, line);

        numFeaturesProcessed++;
      }

      in.close();

      in = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(new File(inputFileName)))));

      int c = 0;

      line = null;
      fields = null;

      line = in.readLine(); // discard first line....

      while ((line = in.readLine()) != null) {
        if (line.startsWith("#"))
          continue;

        fields = line.split("\\s+");

        // each line consists of cluster feature proportion ...

        if (fields.length <= 1)
          continue;

        System.out.println("\nCluster " + fields[0] + ":\n");

        int j = -1;
        double p;

        numFeaturesProcessed = 0;

        for (int i=1; i<fields.length; i++) {

          if (numFeaturesProcessed >= numTopFeatures)
            continue;

          if (i % 2 == 1)
            j = Integer.parseInt(fields[i]); // this is a feature
          else {

            System.out.println(alphabet.get(j) + "\n");
            numFeaturesProcessed++;
          }
        }

        c++;
      }

      System.out.println("Processed " + c + " clusters");
    }
    catch (IOException e) {
      System.out.println(e);
    }
  }

  public static void main(String[] args) throws java.io.IOException {

    if (args.length != 3) {
      System.err.println("Usage: ExtractTopFeatures <cluster_features_file> <feature_keys_file> <num_top_features>");
      System.exit(1);
    }

    String inputFileName = args[0];
    String featuresFileName = args[1];

    int numTopFeatures = Integer.parseInt(args[2]);

    ExtractTopFeatures extract = new ExtractTopFeatures(numTopFeatures);

    extract.processFile(inputFileName, featuresFileName);
  }
}
