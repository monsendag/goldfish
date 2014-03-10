package edu.ntnu.idi.goldfish;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.correlation.*;
import org.apache.mahout.cf.taste.impl.common.FullRunningAverage;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.Preference;


public class Preprocessor {

	private final int THRESHOLD = 5;
	private Map<Integer, List<Preprocessor.Pref>> prefByUser = new HashMap<Integer, List<Preprocessor.Pref>>();
	private Map<Integer, List<Preprocessor.Pref>> prefByItem = new HashMap<Integer, List<Preprocessor.Pref>>();
	private List<Pref> prefs = new ArrayList<Pref>();
	
	private static Set<String> pseudoRatings = new HashSet<String>();

	public static void main(String[] args) {
		Preprocessor pre = new Preprocessor();
		pre.readFile("datasets/yow-userstudy/ratings-and-timings.csv");

//		pre.calculateRMSE();

		 pre.writeToCsv("datasets/yow-userstudy/processed.csv");
		 
	}
	
	public Preprocessor(){
		
	}
	
	public static boolean isPseudoPreference(Preference pref) {
		return pseudoRatings.contains(String.format("%d_%d", pref.getUserID(), pref.getItemID()));
	}
	
	public DataModel getPreprocessedRatings() throws IOException{
		readFile("datasets/yow-userstudy/ratings-and-timings.csv");
		writeToCsv("datasets/yow-userstudy/processed.csv");
		
		return new FileDataModel(new File("datasets/yow-userstudy/processed.csv")); 
	}

	public void calculateRMSE() {
		FullRunningAverage average = new FullRunningAverage();
		for (Pref p : prefs) {
			boolean hasExplicit = p.expl >= 1;
			boolean hasImplicit = p.impl > 0;

			if (hasExplicit && hasImplicit) {
				// have implicit
				// find out if we have enough explicit-implicit rating pars
				List<Pref> ps = get(p.itemId);
				if (ps.size() >= THRESHOLD) {
					// check if abs(correlation) > 0.5
					double correlation = getCorrelation(ps);
					if (Math.abs(correlation) > 0.5) {
						// calculate pseudorating, and store as explicit rating
						ps.remove(p);
						int pseudo = getPseudoRatingClosestNeighbor(p, ps);
//						double error = getPseudoRatingEqualBins(p, correlation, ps);
						double error = Math.abs(p.expl - pseudo);
						average.addDatum(error);
					}
				}
			}
		}
		System.out.println(average.getAverage());
	}

	public void writeToCsv(String name) {
		for (Pref p : prefs) {
			boolean hasExplicit = p.expl >= 1;
			boolean hasImplicit = p.impl > 0;

			if (!hasExplicit && hasImplicit) {
				// have implicit
				// find out if we have enough explicit-implicit rating pars
				List<Pref> ps = get(p.itemId);
				if (ps.size() >= THRESHOLD) {
					// check if abs(correlation) > 0.5
					double correlation = getCorrelation(ps);
					if (Math.abs(correlation) > 0.5) {
						// calculate pseudorating, and store as explicit rating
						// win
						p.expl = getPseudoRatingClosestNeighbor(p, ps);
						pseudoRatings.add(String.format("%d_%d", p.userId, p.itemId));
					}
				}
			}
		}

		try {
			FileWriter writer = new FileWriter(name);
			for (Pref p : prefs) {
				writer.write(String.format("%d,%d,%d\n", p.userId, p.itemId, p.expl));
			}
			writer.close();
		} catch (IOException e) {
		}
	}

	private int getPseudoRatingClosestNeighbor(Pref p, List<Pref> ps){
		int diff = Integer.MAX_VALUE;
		Pref closestPref = null;
		
		for (Pref pref : ps) {
			int tempDiff = Math.abs(p.impl - pref.impl);
			if(tempDiff < diff){
				diff = tempDiff;
				closestPref = pref;
			}
		}
		
		return closestPref.expl;
	}
	
	private int getPseudoRatingEqualBins(Pref p, double correlation, List<Pref> ps) {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		int pseudoRating;
		
		for (Pref pref : ps) {
			if (pref.impl < min) {
				min = pref.impl;
			}
			if (pref.impl > max) {
				max = pref.impl;
			}
		}
		
		if (min == max) {
			return 3;
		}

		if(p.impl < min){
			pseudoRating = 1;
		} else if(p.impl > max) {
			pseudoRating = 5;
		} else{
			int binSize = (max-min)/5;
			int dif = p.impl - min;
			pseudoRating = (int) (1 + Math.floor(dif/binSize));
		}
		
		if (correlation < 0) {
			pseudoRating = 6 - pseudoRating;
		}

		return pseudoRating;
	}

	private List<Pref> get(int itemId) {
		List<Pref> prefs = new ArrayList<Pref>();
		for (Pref pref : prefByItem.get(itemId)) {
			if (pref.expl > 0 && pref.impl > 0) {
				prefs.add(pref);
			}
		}
		return prefs;
	}

	public void readFile(String csvFile) {

		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";

		try {

			br = new BufferedReader(new FileReader(csvFile));
			br.readLine();
			while ((line = br.readLine()) != null) {

				// use comma as separator
				String[] row = line.split(cvsSplitBy);
				// need 4 values
				if (row.length != 4) {
					continue;
				}

				int userId = Integer.parseInt(row[0]);
				int articleId = Integer.parseInt(row[1]);
				int expl = Integer.parseInt(row[2]);

				int impl = Integer.parseInt(row[3]);

				Pref p = new Pref(userId, articleId, impl, expl);

				if (prefByItem.containsKey(articleId)) {
					List<Preprocessor.Pref> prefs = prefByItem.get(articleId);
					prefs.add(p);
				} else {
					List<Preprocessor.Pref> prefs = new ArrayList<Preprocessor.Pref>();
					prefs.add(p);
					prefByItem.put(articleId, prefs);
				}

				if (prefByUser.containsKey(userId)) {
					List<Preprocessor.Pref> prefs = prefByUser.get(userId);
					prefs.add(p);
				} else {
					List<Preprocessor.Pref> prefs = new ArrayList<Preprocessor.Pref>();
					prefs.add(p);
					prefByUser.put(userId, prefs);
				}

				prefs.add(p);

			}
			br.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public double getCorrelation(List<Pref> prefs) {
		PearsonsCorrelation pc = new PearsonsCorrelation();
		double[] expl = new double[prefs.size()];
		double[] impl = new double[prefs.size()];
		for (int i = 0; i < prefs.size(); i++) {
			Pref p = prefs.get(i);
			expl[i] = p.expl;
			impl[i] = p.impl;
		}
		return pc.correlation(expl, impl);
	}

	public class Pref {
		int userId;
		int itemId;
		public int impl;
		public int expl;

		public Pref(int userId, int itemId, int impl, int expl) {
			this.userId = userId;
			this.itemId = itemId;
			this.impl = impl;
			this.expl = expl;
		}
	}

}
