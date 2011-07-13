package edu.umass.cs.wallach.cluster;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import gnu.trove.*;

import cc.mallet.types.*;
import cc.mallet.util.Maths;

public class WordScore {

  // observed counts

  private int[][] wordTopicCounts; // N_{w|j}
  private int[] wordTopicCountsNorm; // N_{.|j}

  private int[][] wordTopicCountsTrain;
  private int[] wordTopicCountsNormTrain;

  private TIntIntHashMap unseenCounts;

  private int W, T; // constants

  // hyperparamters

  private double[] beta;

  private boolean resetToTrain = false;

  // create a score function with zero counts

  public WordScore(int W, int T, double beta, TIntIntHashMap unseenCounts) {

    this.W = W;
    this.T = T;

    this.beta = new double[1];
    Arrays.fill(this.beta, beta);

    this.unseenCounts = unseenCounts;

    // allocate space for counts

    wordTopicCounts = new int[W][T];
    wordTopicCountsNorm = new int[T];
  }

  public double getScore(int w, int j) {

    double score = 1.0 / W;

    int nwj = wordTopicCounts[w][j];
    int nj = wordTopicCountsNorm[j];

    score *= beta[0] / (nj + beta[0]);
    score += nwj / (nj + beta[0]);

    if (unseenCounts != null)
      if (unseenCounts.containsKey(w))
        score /= (double) unseenCounts.get(w);

    return score;
  }

  public double getScoreNoPrior(int w, int j) {

    int nj = wordTopicCountsNorm[j];

    if (nj == 0)
      return 0.0;
    else {

      double score = (double) wordTopicCounts[w][j] / (double) nj;

      if (unseenCounts != null)
        if (unseenCounts.containsKey(w))
          score /= (double) unseenCounts.get(w);

      return score;
    }
  }

  public void incrementCounts(int w, int j) {

    wordTopicCounts[w][j]++;
    wordTopicCountsNorm[j]++;
  }

  public void decrementCounts(int w, int j) {

    wordTopicCounts[w][j]--;
    wordTopicCountsNorm[j]--;
  }

  // this must be called before processing test data

  public void lock() {

    wordTopicCountsTrain = new int[W][];

    for (int w=0; w<W; w++)
      wordTopicCountsTrain[w] = wordTopicCounts[w].clone();

    wordTopicCountsNormTrain = wordTopicCountsNorm.clone();

    resetToTrain = true;
  }

  public void resetCounts() {

    if (resetToTrain) {

      for (int w=0; w<W; w++)
        wordTopicCounts[w] = wordTopicCountsTrain[w].clone();

      wordTopicCountsNorm = wordTopicCountsNormTrain.clone();
    }
    else {

      for (int w=0; w<W; w++)
        Arrays.fill(wordTopicCounts[w], 0);

      Arrays.fill(wordTopicCountsNorm, 0);
    }
  }

  // computes log prob using the predictive distribution

  public double logProb(Corpus docs, int[][] z) {

    double logProb = 0.0;

    resetCounts();

    int D = docs.size();

    assert z.length == D;

    for (int d=0; d<D; d++) {

      int[] fs = docs.getDocument(d).getTokens();

      int nd = fs.length;

      for (int i=0; i<nd; i++) {

        int w = fs[i];
        int j = z[d][i];

        logProb += Math.log(getScore(w, j));

        incrementCounts(w, j);
      }
    }

    return logProb;
  }

  public double[] getBeta() {

    return beta;
  }

  public void printBeta(String fileName) {

    try {

      PrintWriter pw = new PrintWriter(fileName);

      for (int i=0; i<beta.length; i++)
        pw.println(beta[i]);

      pw.close();
    }
    catch (IOException e) {
      System.out.println(e);
    }
  }

  private double logProb(Corpus docs, int[][] z, double[] newLogBeta) {

    double[] oldBeta = beta.clone();

    for (int i=0; i<beta.length; i++)
      beta[i] = Math.exp(newLogBeta[i]);

    double logProb = logProb(docs, z);

    beta = oldBeta.clone();

    return logProb;
  }

  public void sampleBeta(Corpus docs, int[][] z, LogRandoms rng, int numIterations, double stepSize) {

    int I = beta.length;

    double[] rawParam = new double[I];
    double rawParamSum = 0.0;

    for (int i=0; i<I; i++) {
      rawParam[i] = Math.log(beta[i]);
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
      beta[i] = Math.exp(rawParam[i]);
  }

  public void print(Alphabet dict, String fileName) {

    print(dict, 0.0, -1, false, fileName);
  }

  public void print(Alphabet dict, double threshold, int numWords, boolean summary, String fileName) {

    assert dict.size() == W;

    try {

      PrintStream pw = null;

      if (fileName == null)
        pw = new PrintStream(System.out, true);
      else {

        pw = new PrintStream(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(new File(fileName)))));

        if (!summary)
          pw.println("#topic typeindex type proportion");
      }

      Probability[] probs = new Probability[W];

      for (int j=0; j<T; j++) {
        for (int w=0; w<W; w++)
          probs[w] = new Probability(w, getScore(w, j));

        Arrays.sort(probs);

        if ((numWords > W) || (numWords < 0))
          numWords = W;

        StringBuffer line = new StringBuffer();

        for (int i=0; i<numWords; i++) {

          // break if there are no more words whose proportion is
          // greater than zero or threshold...

          if ((probs[i].prob == 0) || (probs[i].prob < threshold))
            break;

          if ((fileName == null) || summary){
            line.append(dict.lookupObject(probs[i].index));
            line.append(" ");
          }
          else {
            pw.print(j); pw.print(" ");
            pw.print(probs[i].index); pw.print(" ");
            pw.print(dict.lookupObject(probs[i].index)); pw.print(" ");
            pw.print(probs[i].prob); pw.println();
          }
        }

        String string = line.toString();

        if ((fileName == null) || summary)
          if (!string.equals(""))
            pw.println("Topic " + j + ": " + string);
      }

      if (fileName != null)
        pw.close();
    }
    catch (IOException e) {
      System.out.println(e);
    }
  }
}
