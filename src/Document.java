package edu.umass.cs.wallach.cluster;

import gnu.trove.*;

public class Document {

	private String source;
	private TIntArrayList tokenList;
	private int[] tokens;
	private int cluster;

	public Document(String source) {

		this.source = source;

		this.tokenList = new TIntArrayList();

		this.cluster = -1;
	}

	public void add(int[] tokens) {

		tokenList.add(tokens);
	}

	public void lock() {

		tokens = tokenList.toNativeArray();
		tokenList = null;
	}

	public void unLock() {

		tokenList = new TIntArrayList();
		tokenList.add(tokens);

		tokens = null;
	}

	public String getSource() {

		return source;
	}

	public int getLength() {

		return tokens.length;
	}

	public int getToken(int i) {

		return tokens[i];
	}

	public int[] getTokens() {

		return tokens;
	}

	public void setCluster(int cluster) {

		this.cluster = cluster;
	}

	public int getCluster() {

		return this.cluster;
	}
}
