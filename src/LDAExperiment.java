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

    String instanceListFileName = args[0];

    int T = Integer.parseInt(args[1]); // # of topics

    int numIterations = Integer.parseInt(args[2]); // # Gibbs iterations
    int printInterval = Integer.parseInt(args[3]); // # iterations between printing out topics
    int saveStateInterval = Integer.parseInt(args[4]);

    assert args[5].length() == 2;
    boolean[] sample = new boolean[2]; // whether to sample hyperparameters

    for (int i=0; i<2; i++)
      switch(args[5].charAt(i)) {
      case '0': sample[i] = false; break;
      case '1': sample[i] = true; break;
      default: System.exit(1);
      }

    String outputDir = args[6]; // output directory

    // load data

    Alphabet wordDict = new Alphabet();

    Corpus docs = new Corpus(wordDict, null);

    InstanceListLoader.load(instanceListFileName, docs);

    int W = wordDict.size();

    double alpha = 0.1 * T;
    double beta = 0.01 * W;

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

    lda.estimate(docs, null, T, alpha, beta, numIterations, printInterval, saveStateInterval, sample, documentTopicsFileName, topicWordsFileName, topicSummaryFileName, stateFileName, alphaFileName, betaFileName, logProbFileName);

  }
}
