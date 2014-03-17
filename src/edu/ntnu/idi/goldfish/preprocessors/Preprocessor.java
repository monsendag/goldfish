package edu.ntnu.idi.goldfish.preprocessors;

import edu.ntnu.idi.goldfish.mahout.SMDataModel;
import edu.ntnu.idi.goldfish.mahout.SMPreference;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.mahout.cf.taste.common.NoSuchItemException;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Preprocessor {

	private final int THRESHOLD = 3;
	private Map<String, Float> correlations = new HashMap<String, Float>();
	private static Set<String> pseudoRatings = new HashSet<String>();

	public static DataModel getPreprocessedDataModel(String path) throws TasteException, IOException {
		SMDataModel model;
		model = new SMDataModel(new File(path));
		Preprocessor pre = new Preprocessor();
		pre.preprocess(model);
		return model;
	}

	public Preprocessor() {

	}

	public static boolean isPseudoPreference(Preference pref) {
		return pseudoRatings.contains(String.format("%d_%d", pref.getUserID(), pref.getItemID()));
	}

	/**
	 * For each missing explicit value (when implicit feedback exist), check if enough explicit-implicit
	 * rating pairs exists, if so, create a pseudo rating and add it to the dataset  
	 * @param model
	 * 		the model to preprocess
	 * @throws TasteException
	 */
	public void preprocess(SMDataModel model) throws TasteException {
		// iterate through all items
		LongPrimitiveIterator it = model.getItemIDs();
		while (it.hasNext()) {
			long itemID = it.next();

			// iterate through all prefs for item
			PreferenceArray prefs = model.getPreferencesForItem(itemID);
//			System.out.println(String.format("Number of ratings,  %d, for each item %d", prefs.length(), itemID));
			for (Preference p : prefs) {
				SMPreference pref = (SMPreference) p;
				boolean hasExplicit = pref.getValue(0) >= 1;

				if (!hasExplicit) {

					
					boolean hasImplicit = false;
					float[] vals = pref.getValues();
					
					
					
					for (int i = 1; i < vals.length; i++) {
						if (vals[i] >= 1) {
							hasImplicit = true;
							break;
						}
					}
					// iterate through implicit values and use the one with
					// highest correlation

					// find out if we have enough explicit-implicit rating
					// pars
					double[] explRatings = getRatings(model, itemID, 0);
					if (hasImplicit && explRatings.length >= THRESHOLD) {

						int bestCorrelated = -1;
						for (int i = 1; i < vals.length; i++) {
							if (bestCorrelated == -1
									|| getCorrelation(model, itemID, i) > getCorrelation(model, itemID, bestCorrelated)) {
								bestCorrelated = i;
							}
						}

						
						// check if |correlation| > 0.5
						double correlation = getCorrelation(model, itemID, bestCorrelated);
						if (Math.abs(correlation) > 0.5) {
							// we have now ensured that a relationship between the implicit and explicit feedback
							// exist and will continue to find pseudoRatings
//								System.out.println(String.format("ItemID: %d", itemID));
							
							// I: get pseudoRating based on linear regression
							double[] implRatings = getRatings(model, itemID, bestCorrelated); 
							TrendLine t = new PolyTrendLine(1);
							t.setValues(explRatings, implRatings);
							float pseudoRating = (float) Math.round(t.predict(pref.getValue(bestCorrelated)));
							
							// II: get pseudoRating based on closest neighbor
//							float pseudoRating = getPseudoRatingClosestNeighbor(prefs, pref, bestCorrelated);
							
							// III: get pseudoRating based on rating bins
//							float pseudoRating = getPseudoRatingEqualBins(pref, correlation, prefs, bestCorrelated);
							
							// set explicit value as pseudoRating
							pref.setValue(pseudoRating, 0); 
							
							// remember the pseudoRatings to ensure they are only used in the training set
							pseudoRatings.add(String.format("%d_%d", pref.getUserID(), pref.getItemID())); 
						}
					} else if(vals[1] > 17500){
						// according to CEO Tony Haile at Chartbeat people that spends more than 
						// 15 seconds on an article like the article
						// source: http://time.com/12933/what-you-think-you-know-about-the-web-is-wrong/
						// Morita and Shinoda (1994) concluded that the most effective threshold concerning
						// reading time is 20 seconds, which yielded 30% recall and 70% precision
						pref.setValue(4, 0);
						pseudoRatings.add(String.format("%d_%d", pref.getUserID(), pref.getItemID()));
//						System.out.println(String.format("User spent more than 15 seconds on item: %d, "
//								+ "lets give it 4", itemID));
					} else if (vals[1] < 17500){
						pref.setValue(2, 0);
						pseudoRatings.add(String.format("%d_%d", pref.getUserID(), pref.getItemID()));
					} else if(vals[2] > 700){
						// with time on mouse = 700, most users give an article rating 4
						pref.setValue(4,0);
//						System.out.println(String.format("User has used the mouse more than 7 seconds on item: %d, "
//								+ "lets give it 4", itemID));
					}
				}
			}
		}
	}
	
	/**
	 * Finds the pseudo rating given an article without explicit rating by using the closest neighbor,
	 * that is, the neighbor with implicit feedback that resembles the given article most
	 * @param prefs
	 * 		all preferences for a given article
	 * @param currentPref
	 * 		the preference without explicit value and hence will get a pseudo rating
	 * @param bestCorrelated
	 * 		the implicit feedback index with highest correlation
	 * @return
	 */
	private float getPseudoRatingClosestNeighbor(PreferenceArray prefs, SMPreference currentPref, int bestCorrelated) {
		float diff = Float.MAX_VALUE;
		SMPreference closestPref = null;

		for (Preference pref : prefs) {
			SMPreference p = (SMPreference) pref;

			float tempDiff = Math.abs(currentPref.getValue(1) - p.getValue(bestCorrelated));
			if (tempDiff < diff) {
				diff = tempDiff;
				closestPref = p;
			}
		}

		return closestPref.getValue(0);
	}
	
	/**
	 * Finds the pseudo rating based on equal size bins, requires at least two ratings, which becomes the
	 * biggest and smallest bin.
	 * @param currentPref
	 * 		the preference without explicit value and hence will get a pseudo rating
	 * @param correlation
	 * 		if correlation is negative, pseudo rating is 6 - correlation
	 * @param prefs
	 * 		all preferences for a given article
	 * @param bestCorrelated
	 * 		the implicit feedback index with highest correlation
	 * @return
	 */
	private int getPseudoRatingEqualBins(SMPreference currentPref, double correlation, PreferenceArray prefs, int bestCorrelated) {
		float min = Integer.MAX_VALUE;
		float max = Integer.MIN_VALUE;
		int pseudoRating;
		 		
		for (Preference pref : prefs) {
			SMPreference smp = (SMPreference) pref;
			float implicit = smp.getValue(bestCorrelated);
			if (implicit < min) {
			min = implicit;
			}
		 	if (implicit > max) {
		 		max = implicit;
		 	}
		}
		 		
		if (min == max) {
			return 3;
		}
		
		if(currentPref.getValue(bestCorrelated) < min){
			pseudoRating = 1;
		} else if(currentPref.getValue(bestCorrelated) > max) {
			pseudoRating = 5;
		} else{
			float binSize = (max-min)/5;
			float dif = currentPref.getValue(bestCorrelated) - min;
			pseudoRating = (int) (1 + Math.floor(dif/binSize));
		}
		 			 		
		if (correlation < 0) {
			pseudoRating = 6 - pseudoRating;
		}
		 			 
		return pseudoRating;
	}

	public double getCorrelation(SMDataModel model, long itemID, int implicitIndex) throws NoSuchItemException {
			double[] expl = getRatings(model, itemID, 0);
			double[] impl = getRatings(model, itemID, implicitIndex);
			
			PearsonsCorrelation pc = new PearsonsCorrelation();
			return pc.correlation(expl, impl);
	}
	
	/**
	 * 
	 * @param model
	 * @param itemID
	 * @param index
	 * 		0 = explicit, 1,2...n = implicit
	 * @return
	 * @throws NoSuchItemException
	 */
	public double[] getRatings(SMDataModel model, long itemID, int index) throws NoSuchItemException{
		PreferenceArray prefs = model.getPreferencesForItem(itemID);
		
		// create a list of ratings since we don't know how many rating-pairs exist
		List<Double> tempRatings = new ArrayList<Double>();
		for (int i = 0; i < prefs.length(); i++) {
			SMPreference p = (SMPreference) prefs.get(i);
            // ensure that we have explicit value
            if(p.getValue(0) <= 0) {
                continue;
            }
			tempRatings.add((double) p.getValue(index));
		}
		
		// create the array of ratings with size of ratings-pairs
		double[] ratings = new double[tempRatings.size()];
		for (int i = 0; i < ratings.length; i++) {
			ratings[i] = tempRatings.get(i);
		}
		return ratings;
	}

}
