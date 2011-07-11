package edu.umass.cs.wallach.cluster;

import java.io.*;
import java.util.*;

public class HyperparamLoader {

  public static void load(int N, String fileName, double[] hyperparam) {

		int currentPosition = 0;

		assert hyperparam.length == N;

		try {

			BufferedReader in = new BufferedReader(new FileReader(fileName));

			String line = null;

			while ((line = in.readLine()) != null) {

				double value = Double.parseDouble(line.trim());

				hyperparam[currentPosition] = value;

				currentPosition++;
			}

			in.close();
		}
		catch (IOException e) {
			System.out.println(e);
		}

		assert currentPosition == N;
	}
}
