package edu.umass.cs.wallach.cluster;

import java.io.*;
import java.util.*;

import gnu.trove.*;

import cc.mallet.util.Maths;

// ClusterFeature clusters documents from one type of information
// (e.g., authors or topics)

public class ClusterFeature {

	// observed counts

	private ClusterFeatureScoreSparse featureScore;

	private int F, D, C; // constants

	// default concentration parameter for the DP prior over clusters

	private double theta;

	private String priorType;

	private Corpus docs; // actual documents

	private Item[] items; // list of items
	private Cluster[] clusters; // list of clusters
  private Cluster[] clusterAssignments; // item-cluster mapping

	private Stack<Cluster> emptyClusterStack; // stack of empty clusters

	private LogRandoms rng;

	private boolean initialized = false;

	public Cluster[] getClusterAssignments() {

		return clusterAssignments;
	}

	public double getConcentrationParameter() {

		return theta;
	}

	public ClusterFeature() {

		this.rng = new LogRandoms();
	}

	// sample cluster assignments

	// theta is the concentration parameter for the DP prior over
	// clusters, initClusters is the number of clusters to use,
	// itemFile indicates which features have been used in each document

	public void initialize(double theta, String priorType, int initClusters, int maxClusters, double[] alpha, int F, String itemFile, TIntIntHashMap unseenCounts, boolean useDocCounts, Corpus docs) {

		this.theta = theta;

		this.priorType = priorType;

		// keep track of documents

		this.docs = docs;

		this.D = docs.size();

		this.F = F;

		TIntIntHashMap[] z = new TIntIntHashMap[docs.size()];

		int maxFeature = ItemLoader.load(itemFile, z);

		assert maxFeature <= F;

		// have to initialize set of items...

		this.items = new Item[D];

		for (int d=0; d<D; d++)
			items[d] = new Item(z[d], d);

		z = null; // finished using z

		// there can never be more than D clusters (where D is the number
		// of items), therefore our list of all possible clusters should
		// be of length D -- initializing this now means that all the
		// memory we might need is allocated immediately

		C = maxClusters;

		clusters = new Cluster[C]; // initialize the list of all clusters that could possibly be used

		for (int c=0; c<C; c++)
			clusters[c] = new Cluster(c);

		featureScore = new ClusterFeatureScoreSparse(F, D, C, alpha, unseenCounts, "minimal", useDocCounts);

		// clusterAssignments is an array of D clusters -- remember that
		// because objects are pointers in Java, this is an array of N
		// pointers -- e.g., if item d1 and d2 are in the same cluster c
		// then clusterAssignments[d1] = c and clusterAssignmentsn[d2] =
		// c, where c is a single Cluster object

		clusterAssignments = new Cluster[D];

		// randomly assign items to clusters and initialize feature counts

		for (int d=0; d<D; d++) {

			Item item = items[d];

			// get the old cluster ID for this document (if one exists)

			int c = docs.getDocument(d).getCluster();

			if (c == -1)
				c = rng.nextInt(initClusters);

			Cluster cluster = clusters[c];

			// add the item to that cluster

			cluster.add(item);

			// set the dth entry in the clusterAssignments array to the
			// Cluster object for the dth item)

			clusterAssignments[d] = cluster;

			// initialize feature counts...

			TIntIntHashMap itemCounts = item.counts;

			for (int f: itemCounts.keys())
				featureScore.incrementCounts(f, d, c, itemCounts.get(f));
		}

		// when we want to use a new cluster we want it to be an empty
		// cluster -- therefore, when a cluster goes out of use we want to
		// push it back onto the stack of emtpy clusters that we can use

		emptyClusterStack = new Stack<Cluster>();

		for (Cluster cluster: clusters)
			if (cluster.isEmpty())
				emptyClusterStack.push(cluster);

		//System.out.println("Initialized cluster assignments.");

		initialized = true;
	}

	public void estimate(boolean sampleTheta, int S, String clustersFileName, String numClustersFileName, String thetaFileName, String dataLogProbFileName) {

		assert initialized == true;

		System.out.println((C - emptyClusterStack.size()) + " clusters");

		for (int s=1; s<=S; s++) {

			sampleClusters(true);

			System.out.print((C - emptyClusterStack.size()) + " ");

			// sample concentration parameter based on previous clustering

			if (sampleTheta)
				sampleTheta(5, 1.0);

			printClusterAssignments(clustersFileName);
			printNumClusters(numClustersFileName);
			printTheta(thetaFileName);

			printLogProbData(dataLogProbFileName);
		}

		System.out.println();
	}

	public void estimate(boolean sampleTheta, int S) {

		assert initialized == true;

		System.out.println((C - emptyClusterStack.size()) + " clusters");

		for (int s=1; s<=S; s++) {

			sampleClusters(true);

			System.out.print((C - emptyClusterStack.size()) + " ");

			// sample concentration parameter based on previous clustering

			if (sampleTheta)
				sampleTheta(5, 1.0);
		}

		System.out.println();
	}

	public double[] sampleAlpha(int numItns, String alphaFileName) {

		featureScore.sampleAlpha(clusterAssignments, items, rng, numItns, 1.0);

		double[] alpha = featureScore.getAlpha();

		printAlpha(alpha, alphaFileName);

		return alpha;
	}

	// sample cluster assignments

	public void sampleClusters(boolean initialized) {

		for (int d=0; d<D; d++) {

			Item item = items[d]; assert item.ID == d;

			// pull out the item

			TIntIntHashMap itemCounts = item.counts;

			if (initialized) {

				// get the old cluster assignment for this item

				Cluster oldCluster = clusterAssignments[d];
				int oldID = oldCluster.ID;

				// remove the item from this cluster -- if this was the only
				// thing assigned to this cluster, pop it back on the stack

				oldCluster.remove(item);

				if (oldCluster.isEmpty())
					emptyClusterStack.push(oldCluster);

				// remove item from feature counts

				for (int f: itemCounts.keys())
					featureScore.decrementCounts(f, d, oldID, itemCounts.get(f));
			}
			else
				assert docs.getDocument(d).getCluster() == -1;

			// loop over all possible clusters

			double[] logDist = new double[C];

			for (int c=0; c<C; c++) {

				Cluster cluster = clusters[c];

				// if this cluster is empty, ignore it

				if (cluster.isEmpty()) {
					logDist[c] = Double.NEGATIVE_INFINITY;
					continue;
				}

				// otherwise, compute P(c_d = c | f_d, c_{\d}, f_{\d}) -- if
				// we were to normalize the prior we'd use (for a DP prior)
				// (theta + items.size() - 1), however this is a constant from
				// the perspective of the dist. we're sampling from

				logDist[c] = cluster.getLogProb(item);

				if (priorType.equals("UP")) {

					// we're not actually computing the probability of this
					// cluster assignment given all cluster assignments here,
					// we're actually going to compute the JOINT probability of
					// all cluster assignments, since the conditional
					// probability is proportional to this...

					// we're on item d -- so we need to process items 0...d-1
					// and see how many unique clusters there are for those
					// items... we can do this by...

					TIntArrayList prevIDs = new TIntArrayList();

					for (int dp=0; dp<d; dp++) {

						int dpID = clusterAssignments[dp].ID;

						if (!prevIDs.contains(dpID))
							prevIDs.add(dpID);
					}

					// the probability of adding item d to cluster c at this
					// point is therefore 1.0 / (# prev. clusters + theta)

					double logPrior = Math.log(1.0) - Math.log(prevIDs.size() + theta);

					// but we have to compute the impact on future clusters

					for (int dp=(d+1); dp<D; dp++) {

						int dpID = clusterAssignments[dp].ID;

						if (prevIDs.contains(dpID))
							logPrior += Math.log(1.0) - Math.log(prevIDs.size() + theta);
						else {
							logPrior += Math.log(theta) - Math.log(prevIDs.size() + theta);
							prevIDs.add(dpID);
						}
					}

					logDist[c] += logPrior;
				}
				else
					logDist[c] += Math.log(cluster.clusterSize);
			}

			// compute probability of picking a new cluster

			assert !emptyClusterStack.empty();

			// pop a lucky empty cluster from the stack

			Cluster empty = emptyClusterStack.pop();

			logDist[empty.ID] = empty.getLogProb(item);

			if (priorType.equals("UP")) {

				// make a list of all the clusters used so far

				TIntArrayList prevIDs = new TIntArrayList();

				for (int dp=0; dp<d; dp++) {

					int dpID = clusterAssignments[dp].ID;

					if (!prevIDs.contains(dpID))
						prevIDs.add(dpID);
				}

				double logPrior = Math.log(theta) - Math.log(prevIDs.size() + theta);

				prevIDs.add(empty.ID);

				// the first term is now computed... now to compute the second
				// term -- this involves looking at all future items

				for (int dp=(d+1); dp<D; dp++) {

					int dpID = clusterAssignments[dp].ID;

					if (prevIDs.contains(dpID))
						logPrior += Math.log(1.0) - Math.log(prevIDs.size() + theta);
					else {
						logPrior += Math.log(theta) - Math.log(prevIDs.size() + theta);
						prevIDs.add(dpID);
					}
				}

				logDist[empty.ID] += logPrior;
			}
			else
				logDist[empty.ID] += Math.log(theta);

			// draw a new cluster for this item

			int c = rng.nextDiscreteLogDist(logDist);

			Cluster newCluster = clusters[c];

			// if the empty cluster wasn't selected, pop it back on the
			// stack for future use

			if (newCluster != empty) {

				emptyClusterStack.push(empty);
				assert empty.ID != c;
			}
			else
				assert empty.ID == c;

			// add item to feature counts

			newCluster.add(item);

			for (int f: itemCounts.keys())
				featureScore.incrementCounts(f, d, c, itemCounts.get(f));

			// update the clusterAssignments array list so that the entry
			// for this item points to the newly selected cluster

			clusterAssignments[d] = newCluster;
		}
	}

	public void printClusterAssignments(String fileName) {

		try {

			PrintWriter pw = new PrintWriter(fileName);

			for (int d=0; d<D; d++) {

				String docName = docs.getDocument(d).getSource();

				pw.println(docName + " " + clusterAssignments[d].ID);
			}

			pw.close();
		}
		catch (IOException e) {
			System.out.println(e);
		}
	}

	public void printTheta(String fileName) {

		try {

			PrintWriter pw = new PrintWriter(fileName);

			pw.println(theta);

			pw.close();
		}
		catch (IOException e) {
			System.out.println(e);
		}
	}

	public void printLogProbData(String fileName) {

		try {

			PrintWriter pw = new PrintWriter(fileName);

			pw.println(getLogProbData());

			pw.close();
		}
		catch (IOException e) {
			System.out.println(e);
		}
	}

	public void printAlpha(double[] alpha, String fileName) {

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

	public void printNumClusters(String fileName) {

		try {

			PrintWriter pw = new PrintWriter(fileName);

			pw.println((C - emptyClusterStack.size()));

			pw.close();
		}
		catch (IOException e) {
			System.out.println(e);
		}
	}

	public void printClusterFeatures(String fileName) {

		featureScore.printClusterFeatures(0.0, -1, fileName);
	}

	public double getLogProbData() {

		return featureScore.logProb(clusterAssignments, items);
	}

	public double getLogProbClusters() {

		return getLogProbClusters(theta);
	}

	public double getLogProbClusters(double newTheta) {

		double logProb = 0.0;

		double logNewTheta = Math.log(newTheta);

		int[] clusterCounts = new int[C];
		int clusterCountsNorm = 0;

		for (int d=0; d<D; d++) {

			int c = clusterAssignments[d].ID;

			int nc = clusterCounts[c];

			if (priorType.equals("UP"))
				logProb += (nc == 0) ? logNewTheta : Math.log(1.0);
			else
				logProb += (nc == 0) ? logNewTheta : Math.log(nc);

			logProb -= Math.log(newTheta + clusterCountsNorm);

			clusterCounts[c]++;

			if (priorType.equals("UP")) {
				if (nc == 0)
					clusterCountsNorm++;
			}
			else
				clusterCountsNorm++;
		}

		if (priorType.equals("UP")) {

			int numActive = 0;

			for (int c=0; c<C; c++) {
				assert clusterCounts[c] == clusters[c].clusterSize;

				if (clusterCounts[c] != 0)
					numActive++;
			}

			assert clusterCountsNorm == numActive;
		}
		else
			for (int c=0; c<C; c++)
				assert clusterCounts[c] == clusters[c].clusterSize;

		return logProb;
	}

	public void sampleTheta(int numSamples, double stepSize) {

		double rawParam = Math.log(theta);

		double l = 0.0;
		double r = 0.0;

		for (int s=0; s<numSamples; s++) {

			double lp = getLogProbClusters(Math.exp(rawParam)) + rawParam;
			double lpNew = Math.log(rng.nextUniform()) + lp;

			l = rawParam - rng.nextUniform() * stepSize;
			r = l + stepSize;

			double rawParamNew = 0.0;

			while (true) {

				rawParamNew = l + rng.nextUniform() * (r - l);

				if (getLogProbClusters(Math.exp(rawParamNew)) + rawParamNew > lpNew)
					break;
				else
					if (rawParamNew < rawParam)
						l = rawParamNew;
					else
						r = rawParamNew;
			}

			rawParam = rawParamNew;
		}

		theta = Math.exp(rawParam);

		System.out.println("Theta: " + theta);
	}

	class Cluster {

		public int ID;

		private int clusterSize; // number of items assigned to this cluster

		// list of items assigned to this cluster (with no duplicates) --
		// note that we can just say items.remove(item) and this will
		// quickly remove item from the set

		private HashSet<Item> clusterItems;

		public Cluster (int ID) { // # features, cluster ID

	    this.ID = ID;

			this.clusterSize = 0;

			this.clusterItems = new HashSet<Item>();
		}

		public boolean isEmpty() {

			if (clusterSize == 0)
				return true;
			else
				return false;
		}

		public void remove(Item item) {

			TIntIntHashMap itemCounts = item.counts;

			this.clusterItems.remove(item);
			this.clusterSize--;

			assert this.clusterItems.size() == clusterSize;
		}

		public void add(Item item) {

			TIntIntHashMap itemCounts = item.counts;

			this.clusterItems.add(item);
			this.clusterSize++;

			assert this.clusterItems.size() == clusterSize;
		}

		// P(f_d | c_d = c, c^{\d}, f^{\d}) -- this assumes that f_d is
		// NOT part of this cluster and the counts reflect this

		public double getLogProb(Item item) {

			TIntIntHashMap itemCounts = item.counts;

			double logProb = 0;

			for (int f: itemCounts.keys()) {

				int nfd = itemCounts.get(f);

				for (int i=0; i<nfd; i++) {

					logProb += Math.log(featureScore.getScore(f, item.ID, this.ID));

					featureScore.incrementCounts(f, item.ID, this.ID);
				}
			}

			for (int f: itemCounts.keys()) {

				int nfd = itemCounts.get(f);

				featureScore.decrementCounts(f, item.ID, this.ID, nfd);
			}

			return logProb;
		}
	}

	class Item implements Comparable {

		public TIntIntHashMap counts;

		public int ID;

		public Item(TIntIntHashMap counts, int ID) {

			this.counts = counts;

			this.ID = ID;
		}

		public int compareTo(Object item) throws ClassCastException {

			if (!(item instanceof Item))
				throw new ClassCastException("Expected an Item object.");

			return this.ID - ((Item) item).ID;
		}
	}
}
