package edu.umass.cs.wallach.cluster;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import gnu.trove.*;

import cc.mallet.types.*;
import cc.mallet.pipe.*;

public class ClusterWordExperiment extends ClusterFeatureExperiment {

  public static int createFeatureFiles(String instanceListFileName, String featureUsageFileName, String featureSummaryFileName) {

    Alphabet wordDict = new Alphabet();

    Corpus docs = new Corpus(wordDict, null);

    InstanceListLoader.load(instanceListFileName, docs);

    int W = wordDict.size();

    int[][] z = new int[docs.size()][];

    for (int d=0; d<docs.size(); d++)
      z[d] = docs.getDocument(d).getTokens().clone();

    docs.printFeatures(z, featureUsageFileName);

    try {

      PrintStream pw = new PrintStream(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(new File(featureSummaryFileName)))));

      for (int w=0; w<W; w++)
        pw.println("Feature " + w + ": " + wordDict.lookupObject(w));

      pw.close();
    }
    catch (IOException e) {
      System.out.println(e);
    }

    return W;
  }

  public static void main(String[] args) throws java.io.IOException {

    if (args.length != 11) {
      System.out.println("Usage: ClusterWordExperiment <instance_list> <num_clusters> <num_itns> <num_cluster_itns> <save_state_interval> <theta_init> <eps_init> <sample_cluster_params> <use_doc_counts> <prior_type> <output_dir>");
      System.exit(1);
    }

    String instanceListFileName = args[0];
    String outputDir = args[9]; // output directory

    int F = createFeatureFiles(instanceListFileName, outputDir + "/feature_usage.txt.gz", outputDir + "/feature_summary.txt.gz");

    ArrayList<String> argList = new ArrayList<String>(Arrays.asList(args));
    argList.addAll(1, Arrays.asList(new String[] { outputDir + "/feature_usage.txt.gz", Integer.toString(F) }));

    args = new String[argList.size()];
    args = argList.toArray(args);

    ClusterFeatureExperiment.main(args);
  }
}
