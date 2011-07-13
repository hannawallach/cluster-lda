package edu.umass.cs.wallach.cluster;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import gnu.trove.*;

import cc.mallet.types.*;
import cc.mallet.util.Maths;

public class ClusterTopicScore {

  // observed counts

  private int[][] topicDocCounts; // N_{j|d}
  private int[] topicDocCountsNorm; // N_{.|d}

  private int[][] topicClusterCounts; // N_{j|c}
  private int[] topicClusterCountsNorm; // N_{.|c}

  private int[] topicCounts; // N_{j}
  private int topicCountsNorm; // N_{.}

  private int[][] topicClusterCountsTrain;
  private int[] topicClusterCountsNormTrain;

  private int[] topicCountsTrain;
  private int topicCountsNormTrain;

  private int T, D, C; // constants

  // hyperparamters

  private double[] alpha;

  private boolean resetToTrain = false;

  private String score;

  // create a score function with zero counts

  public ClusterTopicScore(int T, int D, int C, double alpha, String score) {

    this.T = T;
    this.D = D;
    this.C = C;

    this.alpha = new double[3];
    Arrays.fill(this.alpha, alpha);

    this.score = score;

    // allocate space for counts

    topicDocCounts = new int[T][D];
    topicDocCountsNorm = new int[D];

    topicClusterCounts = new int[T][C];
    topicClusterCountsNorm = new int[C];

    topicCounts = new int[T];
    topicCountsNorm = 0;
  }

  public double getScore(int j, int d, int c) {

    double score = 1.0 / T;

    int nj = topicCounts[j];
    int n = topicCountsNorm;

    score *= alpha[0] / (n + alpha[0]);
    score += nj / (n + alpha[0]);

    int njc = topicClusterCounts[j][c];
    int nc = topicClusterCountsNorm[c];

    score *= alpha[1] / (nc + alpha[1]);
    score += njc / (nc + alpha[1]);

    int njd = topicDocCounts[j][d];
    int nd = topicDocCountsNorm[d];

    score *= alpha[2] / (nd + alpha[2]);
    score += njd / (nd + alpha[2]);

    return score;
  }

  public double getScoreNoPrior(int j, int d) {

    int nd = topicDocCountsNorm[d];

    if (nd == 0)
      return 0.0;
    else
      return (double) topicDocCounts[j][d] / (double) nd;
  }

  public double getClusterScoreNoPrior(int j, int c) {

    int nc = topicClusterCountsNorm[c];

    if (nc == 0)
      return 0.0;
    else
      return (double) topicClusterCounts[j][c] / (double) nc;
  }

  public void incrementCounts(int j, int d, int c) {

    int oldCount = topicDocCounts[j][d]++;
    topicDocCountsNorm[d]++;

    if (score.equals("minimal")) {
      if (oldCount == 0) {

        oldCount = topicClusterCounts[j][c]++;
        topicClusterCountsNorm[c]++;

        if (oldCount == 0) {

          topicCounts[j]++;
          topicCountsNorm++;
        }
      }
    }
    else {

      topicClusterCounts[j][c]++;
      topicClusterCountsNorm[c]++;

      topicCounts[j]++;
      topicCountsNorm++;
    }
  }

  public void decrementCounts(int j, int d, int c) {

    int oldCount = topicDocCounts[j][d]--;
    topicDocCountsNorm[d]--;

    if (score.equals("minimal")) {
      if (oldCount == 1) {

        oldCount = topicClusterCounts[j][c]--;
        topicClusterCountsNorm[c]--;

        if (oldCount == 1) {

          topicCounts[j]--;
          topicCountsNorm--;
        }
      }
    }
    else {

      topicClusterCounts[j][c]--;
      topicClusterCountsNorm[c]--;

      topicCounts[j]--;
      topicCountsNorm--;
    }
  }

  // this must be called before processing test data

  public void lock(int numTestDocs) {

    this.D = numTestDocs;

    topicDocCounts = new int[T][D];
    topicDocCountsNorm = new int[D];

    // only need to lock the non-document-specific counts

    topicClusterCountsTrain = topicClusterCounts.clone();
    topicClusterCountsNormTrain = topicClusterCountsNorm.clone();

    topicCountsTrain = topicCounts.clone();
    topicCountsNormTrain = topicCountsNorm;

    resetToTrain = true;
  }

  public void resetCounts() {

    for (int j=0; j<T; j++)
      Arrays.fill(topicDocCounts[j], 0);

    Arrays.fill(topicDocCountsNorm, 0);

    if (resetToTrain) {

      topicClusterCounts = topicClusterCountsTrain.clone();
      topicClusterCountsNorm = topicClusterCountsNormTrain.clone();

      topicCounts = topicCountsTrain.clone();
      topicCountsNorm = topicCountsNormTrain;
    }
    else {

      for (int j=0; j<T; j++)
        Arrays.fill(topicClusterCounts[j], 0);

      Arrays.fill(topicClusterCountsNorm, 0);

      Arrays.fill(topicCounts, 0);
      topicCountsNorm = 0;
    }
  }

  // computes log prob using the predictive distribution

  public double logProb(Corpus docs, int[][] z) {

    double logProb = 0.0;

    resetCounts();

    assert docs.size() == D;
    assert z.length == D;

    for (int d=0; d<D; d++) {

      int c = docs.getDocument(d).getCluster();

      int[] fs = docs.getDocument(d).getTokens();

      int nd = fs.length;

      for (int i=0; i<nd; i++) {

        int j = z[d][i];

        logProb += Math.log(getScore(j, d, c));

        incrementCounts(j, d, c);
      }
    }

    return logProb;
  }

  public double[] getAlpha() {

    return alpha;
  }

  public void printAlpha(String fileName) {

    try {

      PrintWriter pw = new PrintWriter(fileName);

      for (int i=0; i<alpha.length; i++)
        pw.println(alpha[i]);

      pw.close();
    }
    catch (IOException e) {
      System.out.println(e);
    }
  }

  private double logProb(Corpus docs, int[][] z, double[] newLogAlpha) {

    double[] oldAlpha = alpha.clone();

    for (int i=0; i<alpha.length; i++)
      alpha[i] = Math.exp(newLogAlpha[i]);

    double logProb = logProb(docs, z);

    alpha = oldAlpha.clone();

    return logProb;
  }

  public void sampleAlpha(Corpus docs, int[][] z, LogRandoms rng, int numIterations, double stepSize) {

    int I = alpha.length;

    double[] rawParam = new double[I];
    double rawParamSum = 0.0;

    for (int i=0; i<I; i++) {
      rawParam[i] = Math.log(alpha[i]);
      rawParamSum += rawParam[i];
    }

    double[] l = new double[I];
    double[] r = new double[I];

    for (int s=0; s<numIterations; s++) {

      double lp = logProb(docs, z, rawParam) + rawParamSum;
      double lpNew = Math.log(rng.nextUniform()) + lp;

      for (int i=0; i<I; i++) {
        l[i] = rawParam[i] - rng.nextUniform() * stepSize;
        r[i] = l[i] + stepSize;
      }

      double[] rawParamNew = new double[I];
      double rawParamNewSum = 0.0;

      while (true) {

        rawParamNewSum = 0.0;

        for (int i=0; i<I; i++) {
          rawParamNew[i] = l[i] + rng.nextUniform() * (r[i] - l[i]);
          rawParamNewSum += rawParamNew[i];
        }

        if (logProb(docs, z, rawParamNew) + rawParamNewSum > lpNew)
          break;
        else
          for (int i=0; i<I; i++)
            if (rawParamNew[i] < rawParam[i])
              l[i] = rawParamNew[i];
            else
              r[i] = rawParamNew[i];
      }

      rawParam = rawParamNew;
      rawParamSum = rawParamNewSum;
    }

    for (int i=0; i<I; i++)
      alpha[i] = Math.exp(rawParam[i]);
  }

  public void print(Corpus docs, String fileName) {

    print(docs, 0.0, -1, fileName);
  }

  public void print(Corpus docs, double threshold, int numTopics, String fileName) {

    assert docs.size() == D;

    try {

      PrintStream pw = new PrintStream(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(new File(fileName)))));

      pw.println("#doc source topic proportion ...");

      Probability[] probs = new Probability[T];

      for (int d=0; d<D; d++) {

        int c = docs.getDocument(d).getCluster();

        pw.print(d); pw.print(" ");
        pw.print(docs.getDocument(d).getSource()); pw.print(" ");

        for (int j=0; j<T; j++)
          probs[j] = new Probability(j, getScore(j, d, c));

        Arrays.sort(probs);

        if ((numTopics > T) || (numTopics < 0))
          numTopics = T;

        for (int i=0; i<numTopics; i++) {

          // break if there are no more topics whose proportion is
          // greater than zero or threshold...

          if ((probs[i].prob == 0) || (probs[i].prob < threshold))
            break;

          pw.print(probs[i].index); pw.print(" ");
          pw.print(probs[i].prob); pw.print(" ");
        }

        pw.println();
      }

      pw.close();
    }
    catch (IOException e) {
      System.out.println(e);
    }
  }

  public void print(String fileName) {

    print(0.0, -1, fileName);
  }

  public void print(double threshold, int numTopics, String fileName) {

    try {

      PrintStream pw = new PrintStream(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(new File(fileName)))));

      pw.println("#cluster topic proportion ...");

      Probability[] probs = new Probability[T];

      int clusterNum = 0;

      for (int c=0; c<C; c++) {
        for (int j=0; j<T; j++)
          probs[j] = new Probability(j, getClusterScoreNoPrior(j, c));

        Arrays.sort(probs);

        if (probs[0].prob == 0)
          continue;
        else {

          pw.print(clusterNum); pw.print(" ");
          clusterNum++;
        }

        if ((numTopics > T) || (numTopics < 0))
          numTopics = T;

        for (int i=0; i<numTopics; i++) {

          // break if there are no more topics whose proportion is
          // greater than zero or threshold...

          if ((probs[i].prob == 0) || (probs[i].prob < threshold))
            break;

          pw.print(probs[i].index); pw.print(" ");
          pw.print(probs[i].prob); pw.print(" ");
        }

        pw.println();
      }

      pw.close();
    }
    catch (IOException e) {
      System.out.println(e);
    }
  }
}
