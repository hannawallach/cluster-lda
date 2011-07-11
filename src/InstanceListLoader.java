package edu.umass.cs.wallach.cluster;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import gnu.trove.*;

import cc.mallet.types.*;

public class InstanceListLoader {

  public static void load(String inputFile, int offset, int N, Corpus docs) {

		InstanceList instances = InstanceList.load(new File(inputFile));

		Alphabet wordDict = docs.getWordDict();

		// read in data

		Alphabet instanceDict = instances.getDataAlphabet();

		for (int d=0; d<instances.size(); d++) {

			Instance instance = instances.get(d);

			FeatureSequence fs = (FeatureSequence) instance.getData();

			int nd = fs.getLength();

			if (N != -1)
				nd = Math.min(nd - offset, N);

			if (nd > 0) {

				Document document = new Document(instance.getSource().toString());

				int[] tokens = new int[nd];

				for (int i=0; i<nd; i++) {

					String word = ((String) instanceDict.lookupObject(fs.getIndexAtPosition(i + offset))).toLowerCase();

					tokens[i] = wordDict.lookupIndex(word);
				}

				document.add(tokens);

				document.lock();
				docs.add(document);
			}
		}
	}
}
