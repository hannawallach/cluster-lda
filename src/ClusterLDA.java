package edu.umass.cs.wallach.cluster;

import java.io.*;
import java.util.*;

import gnu.trove.*;

import cc.mallet.types.*;
import cc.mallet.util.*;

public class ClusterLDA {

  // observed counts

  private WordScore wordScore;
  private ClusterTopicScore topicScore;

  private int W, T, D, C; // constants

  private int[][] z; // topic assignments

  private LogRandoms rng; // random number generator

  private double getScore(int w, int j, int d, int c) {

    return wordScore.getScore(w, j) * topicScore.getScore(j, d, c);
  }

  // computes P(w, z) using the predictive distribution

  private double logProb(Corpus docs) {

    double logProb = 0;

    wordScore.resetCounts();
    topicScore.resetCounts();

    for (int d=0; d<D; d++) {

      int c = docs.getDocument(d).getCluster();

      int[] fs = docs.getDocument(d).getTokens();

      int nd = fs.length;

      for (int i=0; i<nd; i++) {

        int w = fs[i];
        int j = z[d][i];

        logProb += Math.log(getScore(w, j, d, c));

        wordScore.incrementCounts(w, j);
        topicScore.incrementCounts(j, d, c);
      }
    }

    return logProb;
  }

  private void sampleTopics(Corpus docs, boolean init) {

    // resample topics

    for (int d=0; d<D; d++) {

      int c = docs.getDocument(d).getCluster();

      int[] fs = docs.getDocument(d).getTokens();

      int nd = fs.length;

      if (init)
        z[d] = new int[nd];

      for (int i=0; i<nd; i++) {

        int w = fs[i];
        int oldTopic = z[d][i];

        if (!init) {
          wordScore.decrementCounts(w, oldTopic);
          topicScore.decrementCounts(oldTopic, d, c);
        }

        // build a distribution over topics

        double dist[] = new double[T];
        double distSum = 0.0;

        for (int j=0; j<T; j++) {

          double score = getScore(w, j, d, c);

          dist[j] = score;
          distSum += score;
        }

        int newTopic = rng.nextDiscrete(dist, distSum);

        z[d][i] = newTopic;

        wordScore.incrementCounts(w, newTopic);
        topicScore.incrementCounts(newTopic, d, c);
      }
    }
  }

  // estimate topics

  public void estimate(Corpus docs, TIntIntHashMap unseenCounts, int[][] zInit, int itnOffset, int T, int C, double[] alpha, double[] beta, int numItns, int printInterval, int saveStateInterval, boolean[] sample, String clusterTopicsFileName, String documentTopicsFileName, String topicWordsFileName, String topicSummaryFileName, String stateFileName, String alphaFileName, String betaFileName, String logProbFileName) {

    boolean append = false;

    if (zInit == null)
      assert itnOffset == 0;
    else {
      assert itnOffset >= 0;
      append = true;
    }

    Alphabet wordDict = docs.getWordDict();

    assert (saveStateInterval == 0) || (numItns % saveStateInterval == 0);

    rng = new LogRandoms();

    this.T = T;
    this.C = C;

    W = wordDict.size();
    D = docs.size();

    System.out.println("Num docs: " + D);
    System.out.println("Num words in vocab: " + W);
    System.out.println("Num topics: " + T);

    wordScore = new WordScore(W, T, beta, unseenCounts);
    topicScore = new ClusterTopicScore(T, D, C, alpha, "minimal");

    if (zInit == null) {

      z = new int[D][];
      sampleTopics(docs, true); // initialize topic assignments
    }
    else {

      z = zInit;

      for (int d=0; d<D; d++) {

        int c = docs.getDocument(d).getCluster();

        int[] fs = docs.getDocument(d).getTokens();

        int nd = fs.length;

        for (int i=0; i<nd; i++) {

          int w = fs[i];
          int topic = z[d][i];

          wordScore.incrementCounts(w, topic);
          topicScore.incrementCounts(topic, d, c);
        }
      }
    }

    long start = System.currentTimeMillis();

    try {

      PrintWriter logProbWriter = new PrintWriter(new FileWriter(logProbFileName, append));

      // count matrices have been populated, every token has been
      // assigned to a single topic, so Gibbs sampling can start

      for (int s=1; s<=numItns; s++) {

        if (s % 10 == 0)
          System.out.print(s);
        else
          System.out.print(".");

        System.out.flush();

        sampleTopics(docs, false);

        if (sample[0])
          topicScore.sampleAlpha(docs, z, rng, 5, 1.0);

        if (sample[1])
          wordScore.sampleBeta(docs, z, rng, 5, 1.0);

        if (printInterval != 0) {
          if (s % printInterval == 0) {
            System.out.println();
            wordScore.print(wordDict, 0.0, 20, true, null);

            logProbWriter.println(logProb(docs));
            logProbWriter.flush();
          }
        }

        if ((saveStateInterval != 0) && (s % saveStateInterval == 0)) {
          if (stateFileName != null)
            docs.printFeatures(z, stateFileName + "." + (itnOffset + s));
          if (alphaFileName != null)
            topicScore.printAlpha(alphaFileName + "." + (itnOffset + s));
          if (betaFileName != null)
            wordScore.printBeta(betaFileName + "." + (itnOffset + s));
        }
      }

      Timer.printTimingInfo(start, System.currentTimeMillis());

      if (saveStateInterval == 0) {
        if (stateFileName != null)
          docs.printFeatures(z, stateFileName);
        if (alphaFileName != null)
          topicScore.printAlpha(alphaFileName);
        if (betaFileName != null)
          wordScore.printBeta(betaFileName);
      }

      if (clusterTopicsFileName != null)
        topicScore.print(clusterTopicsFileName);
      if (documentTopicsFileName != null)
        topicScore.print(docs, documentTopicsFileName);
      if (topicWordsFileName != null)
        wordScore.print(wordDict, topicWordsFileName);

      if (topicSummaryFileName != null)
        wordScore.print(wordDict, 0.0, 20, true, topicSummaryFileName);
    }
    catch (IOException e) {
      System.out.println(e);
    }
  }

  public double[] getAlpha() {

    return topicScore.getAlpha();
  }

  public double[] getBeta() {

    return wordScore.getBeta();
  }

  public int[][] getTopics() {

    return z;
  }
}
