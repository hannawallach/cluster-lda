package edu.umass.cs.wallach.cluster;

import java.io.*;
import java.util.*;
import gnu.trove.*;

public class ItemLoader {

  public static int load(String fileName, TIntIntHashMap[] z) {

		int currentDoc = -1;
		int currentPosition = 0;
		int maxFeature = 0;

		for (int d=0; d<z.length; d++)
			z[d] = null;

		try {

			BufferedReader in = new BufferedReader(new FileReader(fileName));

			String line = null;

			while ((line = in.readLine()) != null) {
				if (line.startsWith("#"))
					continue;

				String[] fields = line.split("\\s+");

				// each line consists of doc X X X featureindex

				if (fields.length != 5)
					continue;

				int docIndex = Integer.parseInt(fields[0]);
				int feature = Integer.parseInt(fields[4]);

				if (feature > maxFeature)
					maxFeature = feature;

				if (docIndex != currentDoc) {
					currentDoc = docIndex;
					currentPosition = 0;

					z[currentDoc] = new TIntIntHashMap();
				}

				TIntIntHashMap item = z[currentDoc];

				if (item.containsKey(feature))
					item.put(feature, item.get(feature) + 1);
				else
					item.put(feature, 1);

				currentPosition++;
			}

			in.close();
		}
		catch (IOException e) {
			System.out.println(e);
		}

		for (int d=0; d<z.length; d++) {
			if (z[d] == null)
				System.out.println(d);

			assert z[d] != null;
		}

		return maxFeature + 1;
	}
}
