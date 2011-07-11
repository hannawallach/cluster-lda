package edu.umass.cs.wallach.cluster;

import gnu.trove.*;
import java.io.*;

public class ExtractTopFeatures {

	private TIntObjectHashMap alphabet;

	private int C, T, numTopFeatures;

	public ExtractTopFeatures(int C, int T, int numTopFeatures) {

		this.C = C;
		this.T = T;

		this.numTopFeatures = numTopFeatures;

		alphabet = new TIntObjectHashMap();
	}

	public void processFile(String inputFileName, String featureFileName) {

		try {

			BufferedReader buf = new BufferedReader(new FileReader(featureFileName));

			String line = null;
			String[] tokens = null;

			int numFeaturesProcessed = 0;

			while ((line = buf.readLine()) != null) {
				if (line.equals(""))
					continue;

				tokens = line.split("\\s+");

				assert tokens[0].equals("Feature");

				int t = Integer.parseInt(tokens[1].substring(0, tokens[1].length()-1));

				assert numFeaturesProcessed == t;

				alphabet.put(t, line);

				numFeaturesProcessed++;
			}

			assert numFeaturesProcessed == T;

			buf.close();

			buf = new BufferedReader(new FileReader(inputFileName));

			int c = 0;

			line = null;
			tokens = null;

			line = buf.readLine(); // discard first line....

			while ((line = buf.readLine()) != null) {
				if (line.equals(""))
					continue;

				tokens = line.split("\\s+");

				if (tokens.length > 1) {

					System.out.println("\nCluster " + tokens[0] + ":\n");

					int j = -1;
					double p;

					numFeaturesProcessed = 0;

					for (int i=1; i<tokens.length; i++) {

						if (numFeaturesProcessed >= numTopFeatures)
							continue;

						if (i % 2 == 1)
							j = Integer.parseInt(tokens[i]); // this is a feature
						else {

							System.out.println(alphabet.get(j) + "\n");
							numFeaturesProcessed++;
						}
					}

					c++;
				}
			}

			assert c == C;
		}
		catch (IOException e) {
			System.out.println(e);
		}
	}

	public static void main(String[] args) throws java.io.IOException {

		if (args.length < 5) {
			System.err.println("Usage: ExtractTopFeatures <cluster_feature_file> <feature_file> <num_clusters> <num_features> <num_top_features>");
			System.exit(1);
		}

		String inputFileName = args[0];
		String featureFileName = args[1];

		int C = Integer.parseInt(args[2]);
		int T = Integer.parseInt(args[3]);

		int numTopFeatures = Integer.parseInt(args[4]);

		ExtractTopFeatures extract = new ExtractTopFeatures(C, T, numTopFeatures);

		extract.processFile(inputFileName, featureFileName);
	}
}
