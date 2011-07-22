package edu.umass.cs.wallach.cluster;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import gnu.trove.*;

import cc.mallet.util.Maths;

public class ClusterFeature {

  // observed counts

  private ClusterFeatureScore featureScore;

  private int F, D, C; // constants

  // default parameter(s) for the prior over clusters

  private double[] param;

  private String priorType;

  private Corpus docs; // actual documents

  private Item[] items; // list of items
  private Cluster[] clusters; // list of clusters
  private Cluster[] clusterAssignments; // item-cluster mapping

  private TIntHashSet activeClusters; // list of active cluster IDs
  private Stack<Cluster> emptyClusterStack; // stack of empty clusters

  private LogRandoms rng;

  private boolean initialized = false;

  public Cluster[] getClusterAssignments() {

    return clusterAssignments;
  }

  public double[] getParam() {

    return param;
  }

  public int getNumClusters() {

    return (C - emptyClusterStack.size());
  }

  public ClusterFeature() {

    this.rng = new LogRandoms();
  }

  // sample cluster assignments

  public void initialize(double[] param, String priorType, int maxClusters, double[] alpha, int F, TIntIntHashMap[] counts, TIntIntHashMap unseenCounts, boolean useDocCounts, Corpus docs) {

    this.param = param;

    this.priorType = priorType;

    if (priorType.equals("PYP"))
      assert param.length == 2;
    else
      assert param.length == 1;

    // keep track of documents

    this.docs = docs;

    this.D = docs.size();

    this.F = F;

    // there can never be more than D clusters (where D is the number
    // of items), therefore our list of all possible clusters should
    // be of length D -- initializing this now means that all the
    // memory we might need is allocated immediately

    C = maxClusters;

    clusters = new Cluster[C]; // initialize the list of all clusters that could possibly be used

    for (int c=0; c<C; c++)
      clusters[c] = new Cluster(c);

    featureScore = new ClusterFeatureScore(F, D, C, alpha, unseenCounts, "minimal", useDocCounts);

    // clusterAssignments is an array of D clusters -- remember that
    // because objects are pointers in Java, this is an array of N
    // pointers -- e.g., if item d1 and d2 are in the same cluster c
    // then clusterAssignments[d1] = c and clusterAssignmentsn[d2] =
    // c, where c is a single Cluster object

    clusterAssignments = new Cluster[D];

    activeClusters = new TIntHashSet();

    // have to initialize set of items, randomly assign items to
    // clusters and initialize feature counts

    this.items = new Item[D];

    for (int d=0; d<D; d++) {

      Item item = new Item(counts[d], d);

      items[d] = item;

      // get the old cluster ID for this document

      int c = docs.getDocument(d).getCluster();

      assert c != -1;

      Cluster cluster = clusters[c];

      // add the item to that cluster

      cluster.add(item);

      // add the cluster to list of active clusters

      activeClusters.add(c);

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

    initialized = true;
  }

  public void estimate(boolean sampleParam, int S, String clustersFileName, String numClustersFileName, String paramFileName, String logProbFileName) {

    estimate(sampleParam, S);

    printClusterAssignments(clustersFileName);
    printNumClusters(numClustersFileName);
    printParam(paramFileName);

    printLogProb(logProbFileName);
  }

  public void estimate(boolean sampleParam, int S) {

    assert initialized == true;

    /*
    emptyClusterStack = new Stack<Cluster>();

    for (Cluster cluster: clusters)
      if (cluster.isEmpty())
        emptyClusterStack.push(cluster);
    */

    for (int s=1; s<=S; s++) {

      sampleClusters(true);

      // sample concentration parameter based on previous clustering

      if (sampleParam)
        sampleParam(5, 1.0);
    }
  }

  public double[] sampleAlpha(int numItns, String alphaFileName) {

    double[] alpha = sampleAlpha(numItns);

    printAlpha(alpha, alphaFileName);

    return alpha;
  }

  public double[] sampleAlpha(int numItns) {

    featureScore.sampleAlpha(items, clusterAssignments, rng, numItns, 1.0);

    return featureScore.getAlpha();
  }

  // sample cluster assignments

  public void sampleClusters(boolean initialized) {

    // extract hyperparameters

    double theta = param[0];
    double eps = (param.length == 2) ? param[1] : 0.0;

    // keep a list of all the clusters used so far

    TIntHashSet prevCached = null;

    if (priorType.equals("UP"))
      prevCached = new TIntHashSet();

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

        if (oldCluster.isEmpty()) {
          activeClusters.remove(oldID);
          emptyClusterStack.push(oldCluster);
        }

        // remove item from feature counts

        for (int f: itemCounts.keys())
          featureScore.decrementCounts(f, d, oldID, itemCounts.get(f));
      }
      else
        assert docs.getDocument(d).getCluster() == -1;

      assert !emptyClusterStack.empty();

      // pop a lucky empty cluster from the stack

      Cluster empty = emptyClusterStack.pop();

      activeClusters.add(empty.ID);

      int[] idx = activeClusters.toArray();
      Arrays.sort(idx);

      // loop over all possible clusters

      double[] logDist = new double[idx.length];

      for (int i=0; i<idx.length; i++) {

        int c = idx[i];

        Cluster cluster = clusters[c];

        // if this cluster is empty, handle it appropriately

        if (cluster.isEmpty()) {

          assert cluster == empty;

          logDist[i] = empty.getLogProb(item);

          if (priorType.equals("UP")) {

            TIntHashSet prev = (TIntHashSet) prevCached.clone();

            double logPrior = Math.log(theta) - Math.log(prev.size() + theta);

            prev.add(empty.ID);

            // the 1st term is now computed... now compute the 2nd
            // term -- this involves looking at all future items

            for (int dp=(d+1); dp<D; dp++) {

              int dpID = clusterAssignments[dp].ID;

              if (prev.contains(dpID))
                logPrior += Math.log(1.0) - Math.log(prev.size() + theta);
              else {
                logPrior += Math.log(theta) - Math.log(prev.size() + theta);
                prev.add(dpID);
              }
            }

            logDist[i] += logPrior;
          }
          else if (priorType.equals("PYP"))
            logDist[i] += Math.log(theta + eps * (activeClusters.size() - 1));
          else
            logDist[i] += Math.log(theta);

          continue;
        }

        // otherwise, compute P(c_d = c | f_d, c_{\d}, f_{\d}) -- if
        // we were to normalize the prior we'd use (for a DP prior)
        // (theta + items.size() - 1), however this is a constant from
        // the perspective of the dist. we're sampling from

        logDist[i] = cluster.getLogProb(item);

        if (priorType.equals("UP")) {

          // we're not actually computing the probability of this
          // cluster assignment given all cluster assignments here,
          // we're actually going to compute the JOINT probability of
          // all cluster assignments, since the conditional
          // probability is proportional to this...

          // we're on item d -- so we need to process items 0...d-1
          // and see how many unique clusters there are for those
          // items... we can do this by...

          TIntHashSet prev = (TIntHashSet) prevCached.clone();

          // the probability of adding item d to cluster c at this
          // point is therefore 1.0 / (# prev. clusters + theta)

          double logPrior = Math.log(1.0) - Math.log(prev.size() + theta);

          // but we have to compute the impact on future clusters

          for (int dp=(d+1); dp<D; dp++) {

            int dpID = clusterAssignments[dp].ID;

            if (prev.contains(dpID))
              logPrior += Math.log(1.0) - Math.log(prev.size() + theta);
            else {
              logPrior += Math.log(theta) - Math.log(prev.size() + theta);
              prev.add(dpID);
            }
          }

          logDist[i] += logPrior;
        }
        else if (priorType.equals("PYP"))
          logDist[i] += Math.log(cluster.clusterSize - eps);
        else
          logDist[i] += Math.log(cluster.clusterSize);
      }

      // draw a new cluster for this item

      int c = idx[rng.nextDiscreteLogDist(logDist)];

      Cluster newCluster = clusters[c];

      // if the empty cluster wasn't selected, pop it back on the
      // stack for future use

      if (newCluster != empty) {

        activeClusters.remove(empty.ID);
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

      // update the list of clusters used up to this point

      if (priorType.equals("UP"))
        prevCached.add(c);
    }
  }

  public void printClusterAssignments(String fileName) {

    try {

      PrintStream pw = new PrintStream(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(new File(fileName)))));

      pw.println("#doc source cluster");

      for (int d=0; d<D; d++) {

        pw.print(d); pw.print(" ");
        pw.print(docs.getDocument(d).getSource()); pw.print(" ");
        pw.print(clusterAssignments[d].ID); pw.println();
      }

      pw.close();
    }
    catch (IOException e) {
      System.out.println(e);
    }
  }

  public void printParam(String fileName) {

    try {

      PrintWriter pw = new PrintWriter(new FileWriter(fileName, true));

      pw.print(param[0]);

      if (param.length == 2)
        pw.print(" " + param[1]);

      pw.println();

      pw.close();
    }
    catch (IOException e) {
      System.out.println(e);
    }
  }

  public void printLogProb(String fileName) {

    try {

      PrintWriter pw = new PrintWriter(new FileWriter(fileName, true));

      pw.print(getLogLikelihood()); pw.print(" ");
      pw.print(getLogPrior()); pw.println();

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

      PrintWriter pw = new PrintWriter(new FileWriter(fileName, true));

      pw.println((C - emptyClusterStack.size()));

      pw.close();
    }
    catch (IOException e) {
      System.out.println(e);
    }
  }

  public void printClusterFeatures(String fileName) {

    featureScore.print(0.0, -1, fileName);
  }

  public double getLogLikelihood() {

    return featureScore.logProb(items, clusterAssignments);
  }

  public double getLogPrior(double[] newRawParam) {

    double[] oldParam = new double[param.length];
    System.arraycopy(param, 0, oldParam, 0, param.length);

    param[0] = Math.exp(newRawParam[0]);

    if (param.length == 2) {
      param[1] = Math.exp(newRawParam[1]);
      param[1] /= 1.0 + param[1];
    }

    double logProb = getLogPrior();

    System.arraycopy(oldParam, 0, param, 0, param.length);

    return logProb;
  }

  public double getLogPrior() {

    double theta = param[0];
    double eps = (param.length == 2) ? param[1] : 0.0;

    double logProb = 0.0;

    double logTheta = Math.log(theta);

    int[] clusterCounts = new int[C];

    int numActiveClusters = 0;

    for (int d=0; d<D; d++) {

      int c = clusterAssignments[d].ID;

      int nc = clusterCounts[c];

      if (priorType.equals("UP")) {
        logProb -= Math.log(theta + numActiveClusters);
        logProb += (nc == 0) ? logTheta : Math.log(1.0);
      }
      else if (priorType.equals("PYP")) {
        logProb -= Math.log(theta + d);
        logProb += (nc == 0) ? Math.log(theta + eps * numActiveClusters) : Math.log(nc - eps);
      }
      else {
        logProb -= Math.log(theta + d);
        logProb += (nc == 0) ? logTheta : Math.log(nc);
      }

      clusterCounts[c]++;

      if (nc == 0)
        numActiveClusters++;
    }

    /*
    int num = 0;

    for (int c=0; c<C; c++) {
      assert clusterCounts[c] == clusters[c].clusterSize;

      if (clusterCounts[c] != 0)
        num++;
    }

    assert numActiveClusters == num;
    */

    return logProb;
  }

  public void sampleParam(int numSamples, double stepSize) {

    int I = param.length;

    double[] rawParam = new double[I];
    double rawParamSum = 0.0;

    rawParam[0] = Math.log(param[0]);

    if (I == 2)
      rawParam[1] = Math.log(param[1] / (1.0 - param[1]));

    for (int i=0; i<I; i++) {
      rawParamSum += rawParam[i];
    }

    if (I == 2)
      rawParamSum -= 2.0 * Math.log(1.0 + Math.exp(rawParam[1]));

    double[] l = new double[I];
    double[] r = new double[I];

    for (int s=0; s<numSamples; s++) {

      double lp = getLogPrior(rawParam) + rawParamSum;
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

        if (I == 2)
          rawParamNewSum -= 2.0 * Math.log(1.0 + Math.exp(rawParamNew[1]));

        if (getLogPrior(rawParamNew) + rawParamNewSum > lpNew)
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

    param[0] = Math.exp(rawParam[0]);

    if (I == 2) {
      param[1] = Math.exp(rawParam[1]);
      param[1] /= 1.0 + param[1];

      assert ((param[1] >= 0.0) && (param[1] <= 1.0));
    }
  }

  class Cluster {

    public int ID;

    private int clusterSize; // number of items assigned to this cluster

    // list of items assigned to this cluster (with no duplicates) --
    // note that we can just say items.remove(item) and this will
    // quickly remove item from the set

    private HashSet<Item> clusterItems;

    public Cluster (int ID) { // cluster ID

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

  class Item {

    public TIntIntHashMap counts;

    public int ID;

    public Item(TIntIntHashMap counts, int ID) {

      this.counts = counts;

      this.ID = ID;
    }
  }
}
