package edu.umass.cs.wallach.cluster;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import gnu.trove.*;

import cc.mallet.types.*;
import cc.mallet.pipe.*;

public class ClusterFeatureExperiment {

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

  public static void main(String[] args) throws java.io.IOException {

    if (args.length != 12) {
      System.out.println("Usage: ClusterFeatureExperiment <instance_list> <feature_usage_file> <num_features> <num_clusters> <num_itns> <num_cluster_itns> <save_state_interval> <theta_init> <sample_conc_param> <use_doc_counts> <prior_type> <output_dir>");
      System.exit(1);
    }

    String instanceListFileName = args[0];
    String featureUsageFileName = args[1];

    int F = Integer.parseInt(args[2]); // total # features
    int C = Integer.parseInt(args[3]); // # clusters to use intially

    int numIterations = Integer.parseInt(args[4]);
    int numClusterIterations = Integer.parseInt(args[5]);

    int saveStateInterval = Integer.parseInt(args[6]);

    double theta = Double.parseDouble(args[7]);

    boolean sampleConcentrationParameter = Boolean.valueOf(args[8]);

    boolean useDocCounts = Boolean.valueOf(args[9]);

    String priorType = args[10]; // type of prior

    assert priorType.equals("UP") || priorType.equals("DP");

    String outputDir = args[11]; // output directory

    Alphabet wordDict = new Alphabet();

    Corpus docs = new Corpus(wordDict, null);

    InstanceListLoader.load(instanceListFileName, docs);

    getClusteredCorpus(docs, C);

    // sample...

    ClusterFeature ct = new ClusterFeature();

    int max = docs.size();

    double[] alpha = new double[] { 20000.0, 1000.0, 10.0 };

    ct.initialize(theta, priorType, max, alpha, F, featureUsageFileName, null, useDocCounts, docs); // initialize the clustering model

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