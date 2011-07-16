package edu.umass.cs.wallach.cluster;

import java.io.*;
import java.util.*;

import gnu.trove.*;

import cc.mallet.types.*;

public class LDAExperiment {

  public static void main(String[] args) throws java.io.IOException {

    if (args.length != 7) {
      System.out.println("Usage: LDAExperiment <instance_list> <num_topics> <num_itns> <print_interval> <save_state_interval> <sample> <output_dir>");
      System.exit(1);
    }

    int index = 0;

    String instanceListFileName = args[index++];

    int T = Integer.parseInt(args[index++]); // # of topics

    int numIterations = Integer.parseInt(args[index++]); // # Gibbs iterations
    int printInterval = Integer.parseInt(args[index++]); // # iterations between printing out topics
    int saveStateInterval = Integer.parseInt(args[index++]);

    assert args[index].length() == 2;
    boolean[] sample = new boolean[2]; // whether to sample hyperparameters

    for (int i=0; i<2; i++)
      switch(args[index].charAt(i)) {
      case '0': sample[i] = false; break;
      case '1': sample[i] = true; break;
      default: System.exit(1);
      }

    index++;

    String outputDir = args[index++]; // output directory

    assert index == 7;

    // load data

    Alphabet wordDict = new Alphabet();

    Corpus docs = new Corpus(wordDict, null);

    InstanceListLoader.load(instanceListFileName, docs);

    int W = wordDict.size();

    double[] alpha = new double[2];
    Arrays.fill(alpha, 0.1 * T);

    double[] beta = new double[1];
    Arrays.fill(beta, 0.01 * W);

    // form output filenames

    String optionsFileName = outputDir + "/options.txt";

    String documentTopicsFileName = outputDir + "/doc_topics.txt.gz";
    String topicWordsFileName = outputDir + "/topic_words.txt.gz";
    String topicSummaryFileName = outputDir + "/topic_summary.txt.gz";
    String stateFileName = outputDir + "/state.txt.gz";
    String alphaFileName = outputDir + "/alpha.txt";
    String betaFileName = outputDir + "/beta.txt";
    String logProbFileName = outputDir + "/log_prob.txt";

    PrintWriter pw = new PrintWriter(optionsFileName);

    pw.println("Instance list = " + instanceListFileName);

    int corpusLength = 0;

    for (int d=0; d<docs.size(); d++)
      corpusLength += docs.getDocument(d).getLength();

    pw.println("# tokens = " + corpusLength);

    pw.println("T = " + T);
    pw.println("# iterations = " + numIterations);
    pw.println("Print interval = " + printInterval);
    pw.println("Save state interval = " + saveStateInterval);
    pw.println("Sample alpha = " + sample[0]);
    pw.println("Sample beta = " + sample[1]);
    pw.println("Date = " + (new Date()));

    pw.close();

    LDA lda = new LDA();

    lda.estimate(docs, null, null, 0, T, alpha, beta, numIterations, printInterval, saveStateInterval, sample, documentTopicsFileName, topicWordsFileName, topicSummaryFileName, stateFileName, alphaFileName, betaFileName, logProbFileName);

  }
}
