package edu.umass.cs.wallach.cluster;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import gnu.trove.*;

import cc.mallet.types.*;
import cc.mallet.pipe.*;

public class ClusterLDAExperiment extends ClusterFeatureExperiment {

  public static void aggregate(int[][] z, TIntIntHashMap[] items) {

    assert z.length == items.length;

    for (int d=0; d<z.length; d++) {

      items[d] = new TIntIntHashMap();

      for (int i=0; i<z[d].length; i++) {

        int topic = z[d][i];

        if (items[d].containsKey(topic))
          items[d].put(topic, items[d].get(topic) + 1);
        else
          items[d].put(topic, 1);
      }
    }
  }

  public static void main(String[] args) throws java.io.IOException {

    if (args.length != 15) {
      System.out.println("Usage: ClusterLDAExperiment <instance_list> <feature_usage_file> <num_features> <num_clusters> <num_itns> <num_cluster_itns> <num_topic_itns> <save_state_interval> <theta_init> <eps_init> <sample_cluster_params> <alpha_per_cluster> <use_doc_counts> <prior_type> <output_dir>");
      System.exit(1);
    }

    int index = 0;

    String instanceListFileName = args[index++];
    String featureUsageFileName = args[index++];

    int F = Integer.parseInt(args[index++]); // total # features
    int C = Integer.parseInt(args[index++]); // # clusters to use intially

    int numIterations = Integer.parseInt(args[index++]);
    int numClusterIterations = Integer.parseInt(args[index++]);
    int numTopicIterations = Integer.parseInt(args[index++]);

    int saveStateInterval = Integer.parseInt(args[index++]);

    double theta = Double.parseDouble(args[index++]);
    double eps = Double.parseDouble(args[index++]);

    boolean sampleClusterParameters = Boolean.valueOf(args[index++]);

    boolean alphaPerCluster = Boolean.valueOf(args[index++]);

    boolean useDocCounts = Boolean.valueOf(args[index++]);

    String priorType = args[index++]; // type of prior (UP, UPH, DP or PYP)

    String outputDir = args[index++]; // output directory

    assert index == 15;

    Alphabet wordDict = new Alphabet();

    Corpus docs = new Corpus(wordDict, null);

    InstanceListLoader.load(instanceListFileName, docs);

    int W = wordDict.size();

    getClusteredCorpus(docs, C);

    // sample...

    ClusterFeature ct = new ClusterFeature();

    int max = docs.size();

    double[] alpha = (alphaPerCluster) ? new double[max + 2] : new double[3];
    Arrays.fill(alpha, 0.1 * F);

    double[] beta = new double[1];
    Arrays.fill(beta, 0.01 * W);

    int[][] z = new int[docs.size()][];

    for (int d=0; d<docs.size(); d++)
      z[d] = new int[docs.getDocument(d).getLength()];

    TIntIntHashMap[] counts = new TIntIntHashMap[docs.size()];

    StateLoader.load(featureUsageFileName, z, counts);

    double[] param;

    if (priorType.equals("PYP"))
      param = new double[] { theta, eps };
    else {

      assert eps == 0.0;

      param = new double[] { theta };
    }

    long start = System.currentTimeMillis();

    for (int s=1; s<=numIterations; s++) {

      String itn = Integer.toString(s);

      ct.initialize(param, priorType, max, alpha, F, counts, null, useDocCounts, docs); // initialize the clustering model

      // cluster documents and output final clustering

      boolean sample = false;

      if (s > (numIterations / 2))
        sample = sampleClusterParameters;

      if (s % saveStateInterval == 0) {

        ct.estimate(sample, numClusterIterations, outputDir + "/cluster_assignments.txt.gz." + itn, outputDir + "/num_clusters.txt", outputDir + "/param.txt", outputDir + "/log_prob.txt");

        alpha = ct.sampleAlpha(5, outputDir + "/alpha.txt." + itn);

        ct.printClusterFeatures(outputDir + "/cluster_features.txt.gz." + itn);
      }
      else {

        ct.estimate(sample, numClusterIterations);

        alpha = ct.sampleAlpha(5);
      }

      // extract new parameter value(s)

      param = ct.getParam();

      // create InstanceList with labels that are cluster assignments

      ClusterFeature.Cluster[] clusters = ct.getClusterAssignments();
      getClusteredCorpus(docs, clusters);

      // infer topics

      ClusterLDA lda = new ClusterLDA();

      if (s % saveStateInterval == 0)
        lda.estimate(docs, null, z, (s-1) * numTopicIterations, F, max, alpha, beta, numTopicIterations, 1, 1, new boolean[] { false, true }, null, null, null, outputDir + "/topic_summary.txt.gz." + itn, outputDir + "/state.txt.gz", null, null, outputDir + "/log_prob_topics_and_words.txt");
      else
        lda.estimate(docs, null, z, (s-1) * numTopicIterations, F, max, alpha, beta, numTopicIterations, 1, 0, new boolean[] { false, true }, null, null, null, null, null, null, null, outputDir + "/log_prob_topics_and_words.txt");

      // extract new topic assignments and parameter values

      z = lda.getTopics();
      alpha = lda.getAlpha();
      beta = lda.getBeta();

      aggregate(z, counts);
    }

    Timer.printTimingInfo(start, System.currentTimeMillis());
  }
}
