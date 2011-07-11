package edu.umass.cs.wallach.cluster;

import java.io.*;
import java.util.*;

import gnu.trove.*;

import cc.mallet.types.*;
import cc.mallet.util.Maths;

public class ClusterFeatureScoreSparse {

	// observed counts

	private TLongIntHashMap featureItemCounts; // N_{f|d}
	private int[] featureItemCountsNorm; // N_{.|d}

	private TLongIntHashMap featureClusterCounts; // N_{f|c}
	private int[] featureClusterCountsNorm; // N_{.|c}

	private int[] featureCounts; // N_{f}
	private int featureCountsNorm; // N_{.}

	private TLongIntHashMap featureClusterCountsTrain;
	private int[] featureClusterCountsNormTrain;

	private int[] featureCountsTrain;
	private int featureCountsNormTrain;

	private TIntIntHashMap unseenCounts;

	private int F, D, C; // constants

	// hyperparamters

	private double[] alpha;

	private boolean useDocs;

	private boolean resetToTrain = false;

	private String score;

	// create a score function with zero counts

	public ClusterFeatureScoreSparse(int F, int D, int C, double[] alpha, TIntIntHashMap unseenCounts, String score) {

		this(F, D, C, alpha, unseenCounts, score, true);
	}

	public ClusterFeatureScoreSparse(int F, int D, int C, double[] alpha, TIntIntHashMap unseenCounts, String score, boolean useDocs) {

		this.F = F;
		this.D = D;
		this.C = C;

		this.alpha = alpha;

		this.unseenCounts = unseenCounts;

		this.score = score;

		this.useDocs = useDocs;

		// allocate space for counts

		if (useDocs) {
			featureItemCounts = new TLongIntHashMap();
			featureItemCountsNorm = new int[D];
		}

		featureClusterCounts = new TLongIntHashMap();
		featureClusterCountsNorm = new int[C];

		featureCounts = new int[F];
		featureCountsNorm = 0;
	}

	public double getScore(int f, int d, int c) {

		double score = 1.0 / F;

		int nf = featureCounts[f];
		int n = featureCountsNorm;

		score *= alpha[0] / (n + alpha[0]);
		score += nf / (n + alpha[0]);

		long index = ((long) f * C) + c;

		int nfc = featureClusterCounts.get(index);
		int nc = featureClusterCountsNorm[c];

		score *= alpha[1] / (nc + alpha[1]);
		score += nfc / (nc + alpha[1]);

		if (useDocs) {

			index = ((long) f * D + d);

			int nfd = featureItemCounts.get(index);
			int nd = featureItemCountsNorm[d];

			score *= alpha[2] / (nd + alpha[2]);
			score += nfd / (nd + alpha[2]);
		}

		if (unseenCounts != null)
			if (unseenCounts.containsKey(f))
				score /= (double) unseenCounts.get(f);

		return score;
	}

  // unseen handling!

	public double getClusterScoreNoPrior(int f, int c) {

		int nc = featureClusterCountsNorm[c];

		if (nc == 0)
			return 0.0;
		else {

			long index = ((long) f * C) + c;

			return (double) featureClusterCounts.get(index) / (double) nc;
		}
	}

	public void incrementCounts(int f, int d, int c) {

		if (useDocs) {

			long index = ((long) f * D) + d;

			int oldCount = featureItemCounts.get(index);
			featureItemCounts.put(index, oldCount + 1);
			featureItemCountsNorm[d]++;

			if (score.equals("minimal")) {
				if (oldCount == 0) {

					index = ((long) f * C) + c;

					oldCount = featureClusterCounts.get(index);
					featureClusterCounts.put(index, oldCount + 1);
					featureClusterCountsNorm[c]++;

					if (oldCount == 0) {
						featureCounts[f]++;
						featureCountsNorm++;
					}
				}
			}
			else {

				index = ((long) f * C) + c;

				oldCount = featureClusterCounts.get(index);
				featureClusterCounts.put(index, oldCount + 1);
				featureClusterCountsNorm[c]++;

				featureCounts[f]++;
				featureCountsNorm++;
			}
		}
		else {

			long index = ((long) f * C) + c;

			int oldCount = featureClusterCounts.get(index);
			featureClusterCounts.put(index, oldCount + 1);
			featureClusterCountsNorm[c]++;

			if (score.equals("minimal")) {
				if (oldCount == 0) {
					featureCounts[f]++;
					featureCountsNorm++;
				}
			}
			else {
				featureCounts[f]++;
				featureCountsNorm++;
			}
		}
	}

	public void incrementCounts(int f, int d, int c, int nfd) {

		if (useDocs) {

			long index = ((long) f * D) + d;

			int oldCount = featureItemCounts.get(index);
			featureItemCounts.put(index, oldCount + nfd);
			featureItemCountsNorm[d] += nfd;

			if (score.equals("minimal")) {
				if (oldCount == 0) {

					index = ((long) f * C) + c;

					oldCount = featureClusterCounts.get(index);
					featureClusterCounts.put(index, oldCount + 1);
					featureClusterCountsNorm[c]++;

					if (oldCount == 0) {
						featureCounts[f]++;
						featureCountsNorm++;
					}
				}
			}
			else {

				index = ((long) f * C) + c;

				oldCount = featureClusterCounts.get(index);
				featureClusterCounts.put(index, oldCount + nfd);
				featureClusterCountsNorm[c] += nfd;

				featureCounts[f] += nfd;
				featureCountsNorm += nfd;
			}
		}
		else {

			long index = ((long) f * C) + c;

			int oldCount = featureClusterCounts.get(index);
			featureClusterCounts.put(index, oldCount + nfd);
			featureClusterCountsNorm[c] += nfd;

			if (score.equals("minimal")) {
				if (oldCount == 0) {
					featureCounts[f]++;
					featureCountsNorm++;
				}
			}
			else {
				featureCounts[f] += nfd;
				featureCountsNorm += nfd;
			}
		}
	}

	public void decrementCounts(int f, int d, int c) {

		if (useDocs) {

			long index = ((long) f * D) + d;

			int oldCount = featureItemCounts.get(index);
			featureItemCounts.put(index, oldCount - 1);
			featureItemCountsNorm[d]--;

			if (score.equals("minimal")) {
				if (oldCount == 1) {

					index = ((long) f * C) + c;

					oldCount = featureClusterCounts.get(index);
					featureClusterCounts.put(index, oldCount - 1);
					featureClusterCountsNorm[c]--;

					if (oldCount == 1) {
						featureCounts[f]--;
						featureCountsNorm--;
					}
				}
			}
			else {

				index = ((long) f * C) + c;

				oldCount = featureClusterCounts.get(index);
				featureClusterCounts.put(index, oldCount - 1);
				featureClusterCountsNorm[c]--;

				featureCounts[f]--;
				featureCountsNorm--;
			}
		}
		else {

			long index = ((long) f * C) + c;

			int oldCount = featureClusterCounts.get(index);
			featureClusterCounts.put(index, oldCount - 1);
			featureClusterCountsNorm[c]--;

			if (score.equals("minimal")) {
				if (oldCount == 1) {
					featureCounts[f]--;
					featureCountsNorm--;
				}
			}
			else {
				featureCounts[f]--;
				featureCountsNorm--;
			}
		}
	}

	public void decrementCounts(int f, int d, int c, int nfd) {

		if (useDocs) {

			long index = ((long) f * D) + d;

			int oldCount = featureItemCounts.get(index);
			featureItemCounts.put(index, oldCount - nfd);
			featureItemCountsNorm[d] -= nfd;

			if (score.equals("minimal")) {
				if (oldCount == nfd) {

					index = ((long) f * C) + c;

					oldCount = featureClusterCounts.get(index);
					featureClusterCounts.put(index, oldCount - 1);
					featureClusterCountsNorm[c]--;

					if (oldCount == 1) {
						featureCounts[f]--;
						featureCountsNorm--;
					}
				}
			}
			else {

				index = ((long) f * C) + c;

				oldCount = featureClusterCounts.get(index);
				featureClusterCounts.put(index, oldCount - nfd);
				featureClusterCountsNorm[c] -= nfd;

				featureCounts[f] -= nfd;
				featureCountsNorm -= nfd;
			}
		}
		else {

			long index = ((long) f * C) + c;

			int oldCount = featureClusterCounts.get(index);
			featureClusterCounts.put(index, oldCount - nfd);
			featureClusterCountsNorm[c] -= nfd;

			if (score.equals("minimal")) {
				if (oldCount == nfd) {
					featureCounts[f]--;
					featureCountsNorm--;
				}
			}
			else {
				featureCounts[f] -= nfd;
				featureCountsNorm -= nfd;
			}
		}
	}

	// this must be called before processing test data

	public void lock(int numTestItems) {

		this.D = numTestItems;

		if (useDocs) {
			featureItemCounts = new TLongIntHashMap();
			featureItemCountsNorm = new int[D];
		}

		// only need to lock the non-document-specific counts

		featureClusterCountsTrain = (TLongIntHashMap) featureClusterCounts.clone();
		featureClusterCountsNormTrain = featureClusterCountsNorm.clone();

		featureCountsTrain = featureCounts.clone();
		featureCountsNormTrain = featureCountsNorm;

		resetToTrain = true;
	}

	public void resetCounts() {

		if (useDocs) {

			featureItemCounts.clear();

			Arrays.fill(featureItemCountsNorm, 0);
		}

		if (resetToTrain) {

			featureClusterCounts = (TLongIntHashMap) featureClusterCountsTrain.clone();
			featureClusterCountsNorm = featureClusterCountsNormTrain.clone();

			featureCounts = featureCountsTrain.clone();
			featureCountsNorm = featureCountsNormTrain;
		}
		else {

			featureClusterCounts.clear();

			Arrays.fill(featureClusterCountsNorm, 0);

			Arrays.fill(featureCounts, 0);
			featureCountsNorm = 0;
		}
	}

	// computes log prob using the predictive distribution

	public double logProb(ClusterFeature.Cluster[] assignments, ClusterFeature.Item[] items) {

		double logProb = 0;

		resetCounts();

		assert assignments.length == D;
		assert items.length == D;

		for (int d=0; d<D; d++) {

			int c = assignments[d].ID;

			assert items[d].ID == d;

			TIntIntHashMap itemCounts = items[d].counts;

			for (int f: itemCounts.keys()) {

				int nfd = itemCounts.get(f);

				for (int i=0; i<nfd; i++) {

					logProb += Math.log(getScore(f, d, c));

					incrementCounts(f, d, c);
				}
			}
		}

		return logProb;
	}

	private double logProb(ClusterFeature.Cluster[] assignments, ClusterFeature.Item[] items, double[] newLogAlpha) {

		double[] oldAlpha = alpha.clone();

		for (int i=0; i<alpha.length; i++)
			alpha[i] = Math.exp(newLogAlpha[i]);

		double logProb = logProb(assignments, items);

		alpha = oldAlpha.clone();

		return logProb;
	}

	public void sampleAlpha(ClusterFeature.Cluster[] assignments, ClusterFeature.Item[] items, LogRandoms rng, int numIterations, double stepSize) {

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

			double lp = logProb(assignments, items, rawParam) + rawParamSum;
			double lpNew = Math.log(rng.nextUniform()) + lp;

			for (int i=0; i<I; i++) {
				l[i] = rawParam[i] - rng.nextUniform() * stepSize;
				r[i] = l[i] + stepSize;
			}

			double[] rawParamNew = new double[I];
			double rawParamNewSum = 0.0;

			while (true) {

				for (int i=0; i<I; i++) {
					rawParamNew[i] = l[i] + rng.nextUniform() * (r[i] - l[i]);
					rawParamNewSum += rawParamNew[i];
				}

				if (logProb(assignments, items, rawParamNew) + rawParamNewSum > lpNew)
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

	public void printClusterFeatures(double threshold, int numFeatures, String fileName) {

		try {

			PrintWriter pw = new PrintWriter(fileName);

			pw.println("cluster feature proportion ...");

			double[] dist = new double[F];

			for (int c=0; c<C; c++) {

				pw.print(c); pw.print(" ");

				for (int f=0; f<F; f++)
					dist[f] = getClusterScoreNoPrior(f, c);

				if ((numFeatures > F) || (numFeatures < 0))
					numFeatures = F;

				for (int t=0; t<numFeatures; t++) {

					double max = 0.0;
					int index = -1;

					for (int f=0; f<F; f++) {

						if (dist[f] > max) {
							max = dist[f];
							index = f;
						}
					}

					// break if there are no more topics whose proportion is
					// greater than zero or threshold...

					if ((index == -1) || (dist[index] < threshold))
						break;

					pw.print(index + " " + dist[index] + " ");
					dist[index] = 0;
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
