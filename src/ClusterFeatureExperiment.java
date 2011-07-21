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

    int index = 0;

    String instanceListFileName = args[index++];
    String featureUsageFileName = args[index++];

    int F = Integer.parseInt(args[index++]); // total # features
    int C = Integer.parseInt(args[index++]); // # clusters to use intially

    int numIterations = Integer.parseInt(args[index++]);
    int numClusterIterations = Integer.parseInt(args[index++]);

    int saveStateInterval = Integer.parseInt(args[index++]);

    double theta = Double.parseDouble(args[index++]);

    boolean sampleConcentrationParameter = Boolean.valueOf(args[index++]);

    boolean useDocCounts = Boolean.valueOf(args[index++]);

    String priorType = args[index++]; // type of prior (UP, DP or PYP)

    String outputDir = args[index++]; // output directory

    assert index == 12;

    Alphabet wordDict = new Alphabet();

    Corpus docs = new Corpus(wordDict, null);

    InstanceListLoader.load(instanceListFileName, docs);

    getClusteredCorpus(docs, C);

    // sample...

    ClusterFeature ct = new ClusterFeature();

    int max = docs.size();

    double[] alpha = new double[3];
    Arrays.fill(alpha, 0.1 * F);

    TIntIntHashMap[] counts = new TIntIntHashMap[docs.size()];

    StateLoader.load(featureUsageFileName, null, counts);

    double[] param;

    if (priorType.equals("PYP"))
      param = new double[] { theta, 0.01 };
    else
      param = new double[] { theta };

    ct.initialize(param, priorType, max, alpha, F, counts, null, useDocCounts, docs); // initialize the clustering model

    for (int s=1; s<=numIterations; s++) {

      String itn = Integer.toString(s);

      // cluster documents and output final clustering

      if (s % saveStateInterval == 0) {

        ct.estimate(sampleConcentrationParameter, numClusterIterations, outputDir + "/cluster_assignments.txt.gz." + itn, outputDir + "/num_clusters.txt", outputDir + "/param.txt", outputDir + "/log_prob.txt");

        alpha = ct.sampleAlpha(5, outputDir + "/alpha.txt." + itn);

        ct.printClusterFeatures(outputDir + "/cluster_features.txt.gz." + itn);
      }
      else {

        ct.estimate(sampleConcentrationParameter, numClusterIterations);

        alpha = ct.sampleAlpha(5);
      }

      // extract new parameter value(s)

      param = ct.getParam();

      // create InstanceList with labels that are cluster assignments

      ClusterFeature.Cluster[] clusters = ct.getClusterAssignments();
      getClusteredCorpus(docs, clusters);
    }
  }
}
