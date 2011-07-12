package edu.umass.cs.wallach.cluster;

import java.io.*;
import java.util.*;

import gnu.trove.*;

import cc.mallet.types.*;

public class InstanceListLoader {

  public static void load(String inputFile, Corpus docs) {

    InstanceList instances = InstanceList.load(new File(inputFile));

    Alphabet wordDict = docs.getWordDict();

    // read in data

    Alphabet instanceDict = instances.getDataAlphabet();

    for (int d=0; d<instances.size(); d++) {

      Instance instance = instances.get(d);

      FeatureSequence fs = (FeatureSequence) instance.getData();

      int nd = fs.getLength();

      if (nd > 0) {

        Document document = new Document(instance.getSource().toString());

        TIntArrayList tokenList = new TIntArrayList();

        for (int i=0; i<nd; i++) {

          String word = ((String) instanceDict.lookupObject(fs.getIndexAtPosition(i))).toLowerCase();

          int w = wordDict.lookupIndex(word);

          if (w != -1) // this will only happen if wordDict's growth has been stopped
            tokenList.add(w);
        }

        assert tokenList.size() <= nd;

        document.setTokens(tokenList.toNativeArray());
        docs.add(document);
      }
    }
  }
}
