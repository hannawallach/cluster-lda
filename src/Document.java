package edu.umass.cs.wallach.cluster;

public class Document {

  private String source = null;
  private int[] tokens = null;

  private int cluster = -1;

  public Document(String source) {

    this.source = source;
  }

  public void setTokens(int[] tokens) {

    this.tokens = tokens;
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

    return cluster;
  }
}
