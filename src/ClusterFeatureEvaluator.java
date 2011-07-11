package edu.umass.cs.wallach.cluster;

import java.io.*;
import java.util.*;

import gnu.trove.*;

import cc.mallet.util.Maths;

// ClusterFeatureEvaluator clusters documents from one type of information
// (e.g., authors or topics)

public class ClusterFeatureEvaluator {

	// observed counts

	private ClusterFeatureScoreSparse[] featureScore;

	private int F, D, trainD, C, R; // constants

	// default concentration parameter for the DP prior over clusters

	private double theta;

	private String priorType;

	private Corpus docs; // actual documents

	private Item[] items; // list of items
	private Cluster[][] clusters; // list of clusters
  private Cluster[][] clusterAssignments; // item-cluster mapping

	private Stack[] emptyClusterStack; // stack of empty clusters

	private LogRandoms rng;

	private boolean initialized = false;
	private boolean initializedEval = false;

	private TIntArrayList[] trainIDs;

	public ClusterFeatureEvaluator() {

		this.rng = new LogRandoms();
	}

	// sample cluster assignments

	// theta is the concentration parameter for the DP prior over
	// clusters, initClusters is the number of clusters to use,
	// itemFile indicates which features have been used in each document

	public void initialize(double theta, String priorType, int initClusters, int maxClusters, double[] alpha, int F, String itemFile, TIntIntHashMap unseenCounts, boolean useDocCounts, Corpus docs, int R) {

		this.theta = theta;

		this.priorType = priorType;

		// keep track of documents

		this.docs = docs;

		this.D = docs.size();

		this.F = F;

		this.R = R;

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

		clusters = new Cluster[R][C]; // initialize the list of all clusters that could possibly be used

		for (int r=0; r<R; r++)
			for (int c=0; c<C; c++)
				clusters[r][c] = new Cluster(r, c);

		featureScore = new ClusterFeatureScoreSparse[R];

		for (int r=0; r<R; r++)
			featureScore[r] = new ClusterFeatureScoreSparse(F, D, C, alpha, unseenCounts, "minimal", useDocCounts);

		// clusterAssignments is an array of D clusters -- remember that
		// because objects are pointers in Java, this is an array of N
		// pointers -- e.g., if item d1 and d2 are in the same cluster c
		// then clusterAssignments[d1] = c and clusterAssignmentsn[d2] =
		// c, where c is a single Cluster object

		clusterAssignments = new Cluster[R][D];

		// assign items to clusters and initialize feature counts

		for (int d=0; d<D; d++) {

			Item item = items[d];

			// get the old cluster ID for this document

			int c = docs.getDocument(d).getCluster();

			assert c != -1; // ensure we have an old cluster assignment

			for (int r=0; r<R; r++) {

				Cluster cluster = clusters[r][c];

				// add the item to that cluster

				cluster.add(item);

				// set the dth entry in the clusterAssignments array to the
				// Cluster object for the dth item)

				clusterAssignments[r][d] = cluster;

				// initialize feature counts...

				TIntIntHashMap itemCounts = item.counts;

				for (int f: itemCounts.keys()) {
					featureScore[r].incrementCounts(f, d, c, itemCounts.get(f));
					assert (f < F);
				}
			}
		}

		// when we want to use a new cluster we want it to be an empty
		// cluster -- therefore, when a cluster goes out of use we want to
		// push it back onto the stack of emtpy clusters that we can use

		emptyClusterStack = new Stack[R];

		for (int r=0; r<R; r++) {

			emptyClusterStack[r] = new Stack<Cluster>();

			for (Cluster cluster: clusters[r])
				if (cluster.isEmpty())
					emptyClusterStack[r].push(cluster);
		}

		trainIDs = new TIntArrayList[R];

		for (int r=0; r<R; r++) {

			trainIDs[r] = new TIntArrayList();

			for (int d=0; d<D; d++) {

				int ID = clusterAssignments[r][d].ID;

				if (!trainIDs[r].contains(ID))
					trainIDs[r].add(ID);
			}
		}

		//System.out.println("Initialized cluster assignments.");

		initialized = true;
	}

	public void initializeEval(String itemFile, Corpus docs) {

		this.trainD = this.D;

		// keep track of documents

		this.docs = docs;

		this.D = docs.size();

		TIntIntHashMap[] z = new TIntIntHashMap[docs.size()];

		ItemLoader.load(itemFile, z);

		// have to initialize set of items...

		this.items = new Item[D];

		for (int d=0; d<D; d++)
			items[d] = new Item(z[d], d);

		z = null; // finished using z

		for (int r=0; r<R; r++)
			featureScore[r].lock(D);

		// clusterAssignments is an array of D clusters -- remember that
		// because objects are pointers in Java, this is an array of N
		// pointers -- e.g., if item d1 and d2 are in the same cluster c
		// then clusterAssignments[d1] = c and clusterAssignmentsn[d2] =
		// c, where c is a single Cluster object

		clusterAssignments = new Cluster[R][D];

		initializedEval = true;

		// note that documents have NOT been assigned to clusters...
	}

	public void sampleClusters(boolean initialized, int r, int lim) {

		for (int d=0; d<lim; d++)
			sampleSingleCluster(initialized, r, d, lim);
	}

	public void sampleSingleCluster(boolean initialized, int r, int d, int lim) {

		Item item = items[d]; assert item.ID == d;

		// pull out the item

		TIntIntHashMap itemCounts = item.counts;

		if (initialized) {

			// get the old cluster assignment for this item

			Cluster oldCluster = clusterAssignments[r][d];
			int oldID = oldCluster.ID;

			// remove the item from this cluster -- if this was the only
			// thing assigned to this cluster, pop it back on the stack

			oldCluster.remove(item);

			if (oldCluster.isEmpty())
				emptyClusterStack[r].push(oldCluster);

			// remove item from feature counts

			for (int f: itemCounts.keys())
				if (f < F)
					featureScore[r].decrementCounts(f, d, oldID, itemCounts.get(f));
		}
		else
			assert docs.getDocument(d).getCluster() == -1;

		// loop over all possible clusters

		double[] logDist = new double[C];

		for (int c=0; c<C; c++) {

			Cluster cluster = clusters[r][c];

			// if this cluster is empty, ignore it

			if (cluster.isEmpty()) {
				logDist[c] = Double.NEGATIVE_INFINITY;
				continue;
			}

			// otherwise, compute P(c_d = c | f_d, c_{\d}, f_{\d}) -- if we
			// were to normalize the prior we'd use (for a DP prior) (theta
			// + items.size() - 1), however this is a constant from the
			// perspective of the dist. we're sampling from

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

				TIntArrayList prevIDs = (TIntArrayList) trainIDs[r].clone();

				for (int dp=0; dp<d; dp++) {

					int dpID = clusterAssignments[r][dp].ID;

					if (!prevIDs.contains(dpID))
						prevIDs.add(dpID);
				}

				// the probability of adding item d to cluster c at this
				// point is therefore 1.0 / (# prev. clusters + theta)

				double logPrior = Math.log(1.0) - Math.log(prevIDs.size() + theta);

				// but we have to compute the impact on future clusters -- of
				// course, we're only resampling up to lim

				for (int dp=(d+1); dp<lim; dp++) {

					int dpID = clusterAssignments[r][dp].ID;

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

		assert !emptyClusterStack[r].empty();

		// pop a lucky empty cluster from the stack

		Cluster empty = (Cluster) emptyClusterStack[r].pop();

		logDist[empty.ID] = empty.getLogProb(item);

		if (priorType.equals("UP")) {

			// make a list of all the clusters used so far

			TIntArrayList prevIDs = (TIntArrayList) trainIDs[r].clone();

			for (int dp=0; dp<d; dp++) {

				int dpID = clusterAssignments[r][dp].ID;

				if (!prevIDs.contains(dpID))
					prevIDs.add(dpID);
			}

			double logPrior = Math.log(theta) - Math.log(prevIDs.size() + theta);

			prevIDs.add(empty.ID);

			// the first term is now computed... now to compute the second
			// term -- this involves looking at all future items

			for (int dp=(d+1); dp<lim; dp++) {

				int dpID = clusterAssignments[r][dp].ID;

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

		Cluster newCluster = clusters[r][c];

		// if the empty cluster wasn't selected, pop it back on the stack
		// for future use

		if (newCluster != empty) {

			emptyClusterStack[r].push(empty);
			assert empty.ID != c;
		}
		else
			assert empty.ID == c;

		// add item to feature counts

		newCluster.add(item);

		for (int f: itemCounts.keys())
			if (f < F)
				featureScore[r].incrementCounts(f, d, c, itemCounts.get(f));

		// update the clusterAssignments array list so that the entry for
		// this item points to the newly selected cluster

		clusterAssignments[r][d] = newCluster;
	}

	public double logProbSingleDoc(int r, int d) {

		Item item = items[d]; assert item.ID == d;

		// pull out the item

		TIntIntHashMap itemCounts = item.counts;

		// check that we only assigned clusters for the previous items

		for (int dp=d; dp<D; dp++)
			assert docs.getDocument(dp).getCluster() == -1;

		// loop over all possible clusters

		double[] logDist = new double[C];

		double priorNorm = theta;

		if (priorType.equals("UP")) {
			for (int c=0; c<C; c++)
				if (!clusters[r][c].isEmpty())
					priorNorm++;

			assert priorNorm >= trainIDs[r].size();
		}
		else
			priorNorm += trainD + d;

		for (int c=0; c<C; c++) {

			Cluster cluster = clusters[r][c];

			// if this cluster is empty, ignore it

			if (cluster.isEmpty()) {
				logDist[c] = Double.NEGATIVE_INFINITY;
				continue;
			}

			logDist[c] = cluster.getLogProb(item);

			if (priorType.equals("UP"))
				logDist[c] += Math.log(1.0) - Math.log(priorNorm);
			else
				logDist[c] += Math.log(cluster.clusterSize) - Math.log(priorNorm);
		}

		// compute probability of picking a new cluster

		assert !emptyClusterStack[r].empty();

		// pop a lucky empty cluster from the stack

		Cluster empty = (Cluster) emptyClusterStack[r].pop();

		logDist[empty.ID] = empty.getLogProb(item);
		logDist[empty.ID] += Math.log(theta) - Math.log(priorNorm);

		// we now have the log probs of adding each cluster

		double lp = Double.NEGATIVE_INFINITY;

		for (int c=0; c<C; c++)
			lp = Maths.sumLogProb(lp, logDist[c]);

		// pop the empty cluster back on the stack for future use

		emptyClusterStack[r].push(empty);

		return lp;
	}

	public double particleFilter() {

		double logProbTotal = 0.0;

		for (int d=0; d<D; d++) {

			System.out.print(d + ": ");

			double logProb = Double.NEGATIVE_INFINITY; // log(p_d) from my thesis

			Item item = items[d]; assert item.ID == d;

			// pull out the item

			TIntIntHashMap itemCounts = item.counts;

			// assert that we haven't processed this item yet

			assert docs.getDocument(d).getCluster() == -1;

			for (int r=0; r<R; r++) {

				// need to resample clusters for all documents up to this one...

				sampleClusters(true, r, d);

				// compute sum_c P(w_d, c_d=c | w_{<d}, c_{<d})

				logProb = Maths.sumLogProb(logProb, logProbSingleDoc(r, d));

				// sample a cluster...

				sampleSingleCluster(false, r, d, d);
			}

			logProb -= Math.log(R);

			logProbTotal += logProb;

			System.out.println(logProb);
		}

		return logProbTotal;
	}

	public void printClusterAssignments(String fileName, int r) {

		try {

			PrintWriter pw = new PrintWriter(fileName);

			for (int d=0; d<D; d++) {

				String docName = docs.getDocument(d).getSource();

				pw.println(docName + " " + clusterAssignments[r][d].ID);
			}

			pw.close();
		}
		catch (IOException e) {
			System.out.println(e);
		}
	}

	public void printClusterFeatures(String fileName, int r) {

		featureScore[r].printClusterFeatures(0.0, -1, fileName);
	}

	class Cluster {

		public int r, ID;

		private int clusterSize; // number of items assigned to this cluster

		// list of items assigned to this cluster (with no duplicates) --
		// note that we can just say items.remove(item) and this will
		// quickly remove item from the set

		private HashSet<Item> clusterItems;

		public Cluster (int r, int ID) { // cluster ID, particle

			this.r = r;
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

				if (f < F) {

					int nfd = itemCounts.get(f);

					for (int i=0; i<nfd; i++) {

						logProb += Math.log(featureScore[r].getScore(f, item.ID, this.ID));

						featureScore[r].incrementCounts(f, item.ID, this.ID);
					}
				}
			}

			for (int f: itemCounts.keys()) {

				if (f < F) {

					int nfd = itemCounts.get(f);

					featureScore[r].decrementCounts(f, item.ID, this.ID, nfd);
				}
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
