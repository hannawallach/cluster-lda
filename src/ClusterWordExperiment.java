package edu.umass.cs.wallach.cluster;

import java.io.*;
import java.util.*;

import gnu.trove.*;

import cc.mallet.types.*;
import cc.mallet.pipe.*;

public class ClusterWordExperiment {

  public static void getClusteredCorpus(Corpus docs, int C) {

    assert docs.size() >= C;

    LogRandoms rng = new LogRandoms();

    for (int d=0; d<docs.size(); d++)
      docs.getDocument(d).setCluster(rng.nextInt(C));
  }

  public static void getClusteredCorpus(Corpus docs, ClusterFeature.Cluster[] clusterAssignments) {

    assert docs.size() == clusterAssignments.length;

    for (int d=0; d<docs.size(); d++)
      docs.getDocument(d).setCluster(clusterAssignments[d].ID);
  }

  public static void main(String[] args) {

    if (args.length != 10) {
      System.out.println("Usage: ClusterWordExperiment <data> <num_clusters> <num_itns> <num_cluster_itns> <save_state_interval> <theta_init> <sample_conc_param> <use_doc_counts> <prior_type> <output_dir>");
      System.exit(1);
    }

    String fileName = args[0];

    int C = Integer.parseInt(args[1]); // # clusters to use intially

    int numIterations = Integer.parseInt(args[2]);
    int numClusterIterations = Integer.parseInt(args[3]);

    int saveStateInterval = Integer.parseInt(args[4]);

    double theta = Double.parseDouble(args[5]);

    boolean sampleConcentrationParameter = Boolean.valueOf(args[6]);

    boolean useDocCounts = Boolean.valueOf(args[7]);

    String priorType = args[8]; // type of prior

    assert priorType.equals("UP") || priorType.equals("DP");

    String outputDir = args[9]; // output directory

    String stateFileName = outputDir + "/state.txt.gz";

    Alphabet wordDict = new Alphabet();

    Corpus docs = new Corpus(wordDict, null);

    InstanceListLoader.load(fileName, docs);

    int W = wordDict.size();

    try {

      PrintWriter pw = new PrintWriter(outputDir + "/features.txt");

      for (int w=0; w<W; w++)
        pw.println("Feature " + w + ": " + wordDict.lookupObject(w));

      pw.close();
    }
    catch (IOException e) {
      System.out.println(e);
    }

    int[][] z = new int[docs.size()][];

    for (int d=0; d<docs.size(); d++)
      z[d] = docs.getDocument(d).getTokens().clone();

    docs.printFeatures(z, stateFileName);

    getClusteredCorpus(docs, C);

    // sample...

    ClusterFeature ct = new ClusterFeature();

    int max = docs.size();

    double[] alpha = new double[] { 20000.0, 1000.0, 10.0 };

    ct.initialize(theta, priorType, max, alpha, W, stateFileName, null, useDocCounts, docs); // initialize the clustering model

    for (int s=1; s<=numIterations; s++) {

      String itn = Integer.toString(s);

      // cluster documents and output final clustering

      if (s % saveStateInterval == 0) {

        ct.estimate(sampleConcentrationParameter, numClusterIterations, outputDir + "/cluster_assignments.txt.gz." + itn, outputDir + "/num_clusters.txt", outputDir + "/theta.txt", outputDir + "/log_prob.txt");

        alpha = ct.sampleAlpha(5, outputDir + "/alpha.txt." + itn);

        ct.printClusterFeatures(outputDir + "/cluster_features.txt.gz." + itn);
      }
      else {

        ct.estimate(sampleConcentrationParameter, numClusterIterations);

        alpha = ct.sampleAlpha(5);
      }

      // extract new concentration parameter value

      theta = ct.getConcentrationParameter();

      // create InstanceList with labels that are cluster assignments

      ClusterFeature.Cluster[] clusters = ct.getClusterAssignments();
      getClusteredCorpus(docs, clusters);
    }
  }
}
