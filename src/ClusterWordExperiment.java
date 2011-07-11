package edu.umass.cs.wallach.cluster;

import gnu.trove.*;

import java.util.*;
import java.io.*;

import cc.mallet.types.*;
import cc.mallet.pipe.*;

public class ClusterWordExperiment {

	public static void getClusteredCorpus(Corpus docs, ClusterFeature.Cluster[] clusterAssignments) {

		assert docs.size() == clusterAssignments.length;

		for (int d=0; d<docs.size(); d++)
			docs.getDocument(d).setCluster(clusterAssignments[d].ID);
	}

	public static void main(String[] args) {

		if (args.length != 9) {
			System.out.println("Usage: ClusterWordExperiment <data> <num_clusters> <num_itns> <num_cluster_itns> <theta_init> <sample_conc_param> <use_doc_counts> <prior_type> <output_dir>");
			System.exit(1);
		}

    String fileName = args[0];

		int C = Integer.parseInt(args[1]); // # clusters to use intially

		int numIterations = Integer.parseInt(args[2]);
		int numClusterIterations = Integer.parseInt(args[3]);

		double theta = Double.parseDouble(args[4]);

		boolean sampleConcentrationParameter = Boolean.valueOf(args[5]);

		boolean useDocCounts = Boolean.valueOf(args[6]);

		String priorType = args[7]; // type of prior

		assert priorType.equals("UP") || priorType.equals("DP");

		String outputDir = args[8]; // output directory

		String stateFileName = outputDir + "/state.txt";

    Alphabet wordDict = new Alphabet();

		Corpus docs = new Corpus(wordDict, null);

		InstanceListLoader.load(fileName, 0, -1, docs);

		System.out.println("Data loaded.");

		System.out.println(docs.size());

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

		// sample clusters and topics...

		int[][] z = new int[docs.size()][];

		for (int d=0; d<docs.size(); d++)
			z[d] = docs.getDocument(d).getTokens().clone();

		docs.printAssignments(z, -1, 0, stateFileName);

		ClusterFeature ct = new ClusterFeature();

		int max = docs.size();

		double[] alpha = new double[] { 20000.0, 1000.0, 10.0 };

		for (int s=0; s<numIterations; s++) {

			String iteration = Integer.toString(s);

			// cluster documents and output final clustering

			ct.initialize(theta, priorType, C, max, alpha, W, stateFileName, null, useDocCounts, docs);

			for (int i=0; i<1; i++) {

				ct.estimate(sampleConcentrationParameter, numClusterIterations, outputDir + "/cluster_assignments.txt", outputDir + "/num_clusters.txt", outputDir + "/theta.txt", outputDir + "/topic_log_prob.txt." + iteration);

				alpha = ct.sampleAlpha(5, outputDir + "/alpha.txt");

				ct.printClusterFeatures(outputDir + "/cluster_features.txt");
			}

			// extract new concentration parameter value

			theta = ct.getConcentrationParameter();

			// create InstanceList with labels that are cluster assignments

			ClusterFeature.Cluster[] clusters = ct.getClusterAssignments();
			getClusteredCorpus(docs, clusters);
		}
	}
}
