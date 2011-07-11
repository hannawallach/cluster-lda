package edu.umass.cs.wallach.cluster;

import gnu.trove.*;

import java.util.*;
import java.io.*;

import cc.mallet.types.*;
import cc.mallet.pipe.*;

public class ClusterPermuteEvaluator {

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

		if (args.length != 8) {
			System.out.println("Usage: ClusterPermuteEvaluator <data> <clusters> <alpha_file> <theta_file> <num_clusters> <use_doc_counts> <prior_type> <output_dir>");
			System.exit(1);
		}

    String fileName = args[0];

		String clustersFileName = args[1];
		String alphaFileName = args[2];
		String thetaFileName = args[3];

    int C = Integer.parseInt(args[4]); // # clusters to use intially

		boolean useDocCounts = Boolean.valueOf(args[5]);

		String priorType = args[6]; // type of prior

		assert priorType.equals("UP") || priorType.equals("DP");

		String outputDir = args[7]; // output directory

    Alphabet dict = new Alphabet();

		Corpus docs = new Corpus(dict, null);

		InstanceListLoader.load(fileName, 0, -1, docs);

		System.out.println("Data loaded.");

		System.out.println(docs.size());

		int W = dict.size();

		// get cluster assignments

		getClusteredCorpus(docs, clustersFileName);

		// load hyperparameters

		double[] alpha = new double[3];
		double[] thetaVec = new double[1];

		HyperparamLoader.load(3, alphaFileName, alpha);
		HyperparamLoader.load(1, thetaFileName, thetaVec);

		double theta = thetaVec[0];

		ClusterFeature ct = new ClusterFeature();

		int max = docs.size();

		String permutedStateFileName = outputDir + "/permuted_state.txt";

		// permute the data and compute P(clusters under the UP)

		String logProbFileName = outputDir + "/log_prob_clusters_permutations.txt";

		try {

			PrintWriter pw = new PrintWriter(logProbFileName);

			for (int permNum=0; permNum<10; permNum++) {

				docs.permute();

				// now have a permuted clustered corpus

				int[][] z = new int[docs.size()][];

				for (int d=0; d<docs.size(); d++)
					z[d] = docs.getDocument(d).getTokens().clone();

				docs.printAssignments(z, -1, 0, permutedStateFileName + "." + permNum);

				// compute P(clusters under the UP)

				ct.initialize(theta, "UP", C, max, alpha, W, permutedStateFileName + "." + permNum, null, useDocCounts, docs);

				pw.println(ct.getLogProbClusters()); pw.flush();
			}

			pw.close();
		}
		catch (IOException e) {
			System.out.println(e);
		}
	}
}
