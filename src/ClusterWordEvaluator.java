package edu.umass.cs.wallach.cluster;

import gnu.trove.*;

import java.util.*;
import java.io.*;

import cc.mallet.types.*;
import cc.mallet.pipe.*;

public class ClusterWordEvaluator {

  public static void getClusteredCorpus(Corpus docs, String fileName) {

		int currentDoc = 0;

		try {

			BufferedReader in = new BufferedReader(new FileReader(fileName));

			String line = null;

			while ((line = in.readLine()) != null) {
				if (line.startsWith("#"))
					continue;

				String[] fields = line.split("\\s+");

				// each line consists of docname clusterindex

				if (fields.length != 2)
					continue;

				assert fields[0].equals(docs.getDocument(currentDoc).getSource());

				int clusterID = Integer.parseInt(fields[1]);

				docs.getDocument(currentDoc).setCluster(clusterID);

				currentDoc++;
			}
		}
		catch (IOException e) {
			System.out.println(e);
		}

		assert currentDoc == docs.size();
	}

	public static void main(String[] args) {

		if (args.length != 9) {
			System.out.println("Usage: ClusterWordEvaluator <train_data> <test_data> <train_clusters> <alpha_file> <theta_file> <num_clusters> <use_doc_counts> <prior_type> <output_dir>");
			System.exit(1);
		}

    String trainFileName = args[0];
		String testFileName = args[1];

		String clustersFileName = args[2];
		String alphaFileName = args[3];
		String thetaFileName = args[4];

    int C = Integer.parseInt(args[5]); // # clusters to use intially

		boolean useDocCounts = Boolean.valueOf(args[6]);

		String priorType = args[7]; // type of prior

		assert priorType.equals("UP") || priorType.equals("DP");

		String outputDir = args[8]; // output directory

    Alphabet dict = new Alphabet();

		Corpus trainDocs = new Corpus(dict, null);
		Corpus testDocs = new Corpus(dict, null);

		// load training data

		InstanceListLoader.load(trainFileName, 0, -1, trainDocs);

		int trainW = dict.size();

		// load test data

		InstanceListLoader.load(testFileName, 0, -1, testDocs);

		assert dict.size() > trainW;

		System.out.println("Data loaded.");

		System.out.println(trainDocs.size() + " " + testDocs.size());

		int W = dict.size();

		// get training cluster assignments

		getClusteredCorpus(trainDocs, clustersFileName);

		// load hyperparameters

		double[] alpha = new double[3];
		double[] thetaVec = new double[1];

		HyperparamLoader.load(3, alphaFileName, alpha);
		HyperparamLoader.load(1, thetaFileName, thetaVec);

		double theta = thetaVec[0];

		ClusterFeatureEvaluator ct = new ClusterFeatureEvaluator();

		int max = trainDocs.size() + testDocs.size();

		String stateFileName = outputDir + "/test_state.txt";

		// compute P(test data under the UP)

		String logProbFileName = outputDir + "/log_prob_test.txt";

		try {

			PrintWriter pw = new PrintWriter(logProbFileName);

			ct.initialize(theta, priorType, C, max, alpha, trainW, outputDir + "/state.txt", null, useDocCounts, trainDocs, 20);

			// the model has been initialized using the training data...

			int[][] z = new int[testDocs.size()][];

			for (int d=0; d<testDocs.size(); d++)
				z[d] = testDocs.getDocument(d).getTokens().clone();

			testDocs.printAssignments(z, -1, 0, stateFileName);

			ct.initializeEval(stateFileName, testDocs);

			double lp = ct.particleFilter();

			pw.println(lp); pw.flush();

			pw.close();
		}
		catch (IOException e) {
			System.out.println(e);
		}
	}
}
