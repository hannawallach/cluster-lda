package edu.umass.cs.wallach.cluster;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import gnu.trove.*;

public class ExtractTopFeatures {

  private TIntObjectHashMap alphabet;

  private int numTopFeatures;
  private boolean dups;

  private TIntObjectHashMap clusterFeatures;
  private TIntObjectHashMap clusterNames;

  private int F, C;

  public ExtractTopFeatures(int numTopFeatures, boolean dups) {

    this.numTopFeatures = numTopFeatures;
    this.dups = dups;

    alphabet = new TIntObjectHashMap();

    if (!dups) {
      clusterFeatures = new TIntObjectHashMap();
      clusterNames = new TIntObjectHashMap();
    }
  }

  public void processFile(String inputFileName, String featuresFileName) {

    try {

      BufferedReader in = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(new File(featuresFileName)))));

      String line = null;
      String[] fields = null;

      int numFeaturesProcessed = 0;

      while ((line = in.readLine()) != null) {

        fields = line.split("\\s+");

        int t = Integer.parseInt(fields[1].substring(0, fields[1].length()-1));

        assert numFeaturesProcessed == t;

        alphabet.put(t, line);

        numFeaturesProcessed++;
      }

      F = numFeaturesProcessed;

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

        if (!dups)
          clusterNames.put(c, fields[0]);
        else
          System.out.println("\nCluster " + fields[0] + ":\n");

        int j = -1;
        double p;

        numFeaturesProcessed = 0;

        double[] features = new double[F];

        for (int i=1; i<fields.length; i++) {

          if (numFeaturesProcessed >= numTopFeatures)
            continue;

          if (i % 2 == 1)
            j = Integer.parseInt(fields[i]); // this is a feature
          else {

            if (!dups)
              features[j] = Double.parseDouble(fields[i]);
            else
              System.out.println(alphabet.get(j) + "\n");

            numFeaturesProcessed++;
          }
        }

        if (!dups)
          clusterFeatures.put(c, features);

        c++;
      }

      if (!dups)
        C = c;
      else
        System.out.println("Processed " + c + " clusters");
    }
    catch (IOException e) {
      System.out.println(e);
    }

    if (!dups) {

      int[] appearedIn = new int[F];
      Arrays.fill(appearedIn, 0);

      for (int c=0; c<C; c++) {

        double[] features = (double[]) clusterFeatures.get(c);

        for (int f=0; f<F; f++)
          if (features[f] > 0.0)
            appearedIn[f]++;
      }

      for (int c=0; c<C; c++) {

        double[] features = (double[]) clusterFeatures.get(c);

        System.out.println("\nCluster " + clusterNames.get(c) + ":\n");

        for (int f=0; f<F; f++)
          if ((features[f] > 0.0) && (appearedIn[f] == 1))
            System.out.println(alphabet.get(f) + "\n");
      }

      System.out.println("Processed " + C + " clusters");
    }
  }

  public static void main(String[] args) throws java.io.IOException {

    if (args.length != 4) {
      System.err.println("Usage: ExtractTopFeatures <cluster_features_file> <feature_keys_file> <num_top_features> <show_duplicates>");
      System.exit(1);
    }

    int index = 0;

    String inputFileName = args[index++];
    String featuresFileName = args[index++];

    int numTopFeatures = Integer.parseInt(args[index++]);

    boolean dups = Boolean.valueOf(args[index++]);

    assert index == 4;

    ExtractTopFeatures extract = new ExtractTopFeatures(numTopFeatures, dups);

    extract.processFile(inputFileName, featuresFileName);
  }
}
