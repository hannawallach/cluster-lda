package edu.umass.cs.wallach.cluster;

import java.io.*;
import java.util.*;

import gnu.trove.*;

import cc.mallet.types.*;

public class Corpus {

	private Alphabet wordDict;
	private TIntIntHashMap unseenCounts;
	private ArrayList<Document> documents;

	public Corpus(Alphabet wordDict, TIntIntHashMap unseenCounts) {

		this.wordDict = wordDict;

		this.unseenCounts = unseenCounts;

		this.documents = new ArrayList<Document>();
	}

	public void permute() {

		Collections.shuffle(documents);
	}

	public void add(Document d) {

		documents.add(d);
	}

	public int size() {

		return documents.size();
	}

	public Document getDocument(int d) {

		return documents.get(d);
	}

	public ArrayList<Document> getDocuments() {

		return documents;
	}

	public Alphabet getWordDict() {

		return wordDict;
	}

	public void setWordDict(Alphabet wordDict) {

		this.wordDict = wordDict;
	}

	public TIntIntHashMap getUnseenCounts() {

		return this.unseenCounts;
	}

	public int getUnseenCount(int index) {

		return this.unseenCounts.get(index);
	}

	public void printAssignments(int[][] z, int N, int offset, String fileName) {

		try {

			PrintWriter pw = new PrintWriter(fileName);

			pw.println("#doc pos typeindex type topic");

			for (int d=0; d<documents.size(); d++) {

				int[] fs = documents.get(d).getTokens();

				int nd = fs.length;

				if (N != -1)
					nd = Math.min(nd - offset, N);

				for (int i=0; i<nd; i++) {

					int w = fs[i + offset];

					pw.print(d); pw.print(" ");
					pw.print(i); pw.print(" ");
					pw.print(w); pw.print(" ");
					pw.print(wordDict.lookupObject(w)); pw.print(" ");
					pw.print(z[d][i]); pw.println();
				}
			}

			pw.close();
		}
		catch (IOException e) {
			System.out.println(e);
		}
	}
}
