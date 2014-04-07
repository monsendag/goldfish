package edu.ntnu.idi.goldfish.preprocessors;

import edu.ntnu.idi.goldfish.configurations.Config;
import edu.ntnu.idi.goldfish.mahout.SMDataModel;
import edu.ntnu.idi.goldfish.mahout.SMPreference;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
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
import java.util.List;
import java.util.Map;

/**
 * This class is used to map implicit feedback to explicit rating where it is possible.
 * @author Patrick Romstad and Dag Einar Monsen
 *
 */
public class PreprocessorPuddis extends Preprocessor {

	private final int THRESHOLD = 3;
	
	private Map<String, Float> correlations = new HashMap<String, Float>();
	
	public boolean checkIfPreferenceHasImplicitFeedback(float[] feedback){
		// start with index 1 because index 0 is explicit rating
		for (int i = 1; i < feedback.length; i++) {
			if (feedback[i] >= 1) {
				return true;
			}
		}
		return false;
	}
	
	public int getBestCorrelatedFeedback(PreferenceArray prefs, float[] feedback) throws NoSuchItemException{
		int bestCorrelated = -1;
		
		// iterate through implicit values and use the one with highest correlation
		for (int i = 1; i < feedback.length; i++) {
			if (bestCorrelated == -1
					|| getCorrelation(prefs, i) > getCorrelation(prefs, bestCorrelated)) {
				bestCorrelated = i;
			}
		}
		
		return bestCorrelated;
	}

	/**
	 * For each missing explicit value (when implicit feedback exist), check if enough explicit-implicit
	 * rating pairs exists, if so, create a pseudo rating and add it to the dataset  
	 * 		the model to preprocess
	 * @throws TasteException
	 */
	public DataModel preprocess(Config config) throws TasteException {
        SMDataModel model = config.get("model");
		
		double[] beta = globalLR(model, 3);
		boolean useGlobalLR = false	;
		int numberOfPseudoRatings = 0;
		
		// iterate through all items
		LongPrimitiveIterator it = model.getItemIDs();
		while (it.hasNext()) {
			
			long itemID = it.next();
			PreferenceArray prefs = model.getPreferencesForItem(itemID);
			
			// iterate through all prefs for item
			for (Preference p : prefs) {

				SMPreference pref = (SMPreference) p;
				boolean hasExplicit = pref.getValue(RATING_INDEX) >= 1;

				if (!hasExplicit) {
					float[] feedback = pref.getValues();
					boolean hasImplicit = checkIfPreferenceHasImplicitFeedback(feedback);
					int ratingPairs = getNumberOfRatingPairs(prefs);
					
					if(useGlobalLR){
						float pseudoRating = (float) beta[0];
						for (int i = 1; i < beta.length; i++) {
							pseudoRating += beta[i]*feedback[i];
						}
						
						// the beta0 is 3, have to manually set the lowest ratings
						pseudoRating = feedback[TIME_ON_PAGE_INDEX] < 25000 ? 2 : pseudoRating;
						pseudoRating = feedback[TIME_ON_PAGE_INDEX] < 10000 ? 1 : pseudoRating;
						
						// is pseudorating > 5, then outlier feedback has been used
						pseudoRating = pseudoRating > 5 ? -1 : pseudoRating;
						
						pref.setValue(Math.round(pseudoRating), RATING_INDEX);
						pseudoRatings.add(String.format("%d_%d", pref.getUserID(), pref.getItemID()));
						
						System.out.print(String.format("\nUser %d gave item %d a rating of %d. "
								+ "Page: %.0f, mouse: %.0f %d", pref.getUserID(), pref.getItemID(), 
								Math.round(pseudoRating), feedback[TIME_ON_PAGE_INDEX], feedback[TIME_ON_MOUSE_INDEX], 
								++numberOfPseudoRatings));
						
					}
					// do we have enough pairs of explicit and implicit feedback in order to map
					// from implicit feedback to an explicit rating (pseudo rating) ?
					else if (hasImplicit && ratingPairs >= THRESHOLD) {

						int bestCorrelated = getBestCorrelatedFeedback(prefs, feedback);
						double correlation = getCorrelation(prefs, bestCorrelated);
						
						if (Math.abs(correlation) > 0.5) {
							// we have now ensured that a relationship between the implicit and explicit feedback
							// exist and will continue to find pseudoRatings
							
							// I: get pseudoRating based on linear regression
							float pseudoRating = getPseudoRatingLinearRegression(prefs, pref, bestCorrelated, ratingPairs);
							
							// II: get pseudoRating based on closest neighbor
//							float pseudoRating = getPseudoRatingClosestNeighbor(prefs, pref, bestCorrelated);
							
							// III: get pseudoRating based on rating bins
//							float pseudoRating = getPseudoRatingEqualBins(pref, correlation, prefs, bestCorrelated);
							
							// set explicit value as pseudoRating
							pref.setValue(pseudoRating, 0); 
							
							// remember the pseudoRatings to ensure they are only used in the training set
							pseudoRatings.add(String.format("%d_%d", pref.getUserID(), pref.getItemID()));
							
							System.out.println(String.format("User %d rated item %d with pseudorating: %.0f and correlation: %.2f", 
									pref.getUserID(), pref.getItemID(), pseudoRating, correlation));
						}
					} 
//					else if(timeOnPageAndTimeOnMouseCombined(feedback)){
//						// according to CEO Tony Haile at Chartbeat people that spends more than 
//						// 15 seconds on an article like the article
//						// source: http://time.com/12933/what-you-think-you-know-about-the-web-is-wrong/
//						// Morita and Shinoda (1994) concluded that the most effective threshold concerning
//						// reading time is 20 seconds, which yielded 30% recall and 70% precision
//						pref.setValue(4, 0);
//						pseudoRatings.add(String.format("%d_%d", pref.getUserID(), pref.getItemID()));
////						System.out.println(String.format("User spent more than 15 seconds on item: %d, "
////								+ "lets give it 4", itemID));
//					} 
					else if(timeOnPageFeedback(feedback, 20000, 120000)){
						// according to CEO Tony Haile at Chartbeat people that spends more than 
						// 15 seconds on an article like the article
						// source: http://time.com/12933/what-you-think-you-know-about-the-web-is-wrong/
						// Morita and Shinoda (1994) concluded that the most effective threshold concerning
						// reading time is 20 seconds, which yielded 30% recall and 70% precision
						pref.setValue(4, 0);
						pseudoRatings.add(String.format("%d_%d", pref.getUserID(), pref.getItemID()));
//						System.out.println(String.format("User spent more than 15 seconds on item: %d, "
//								+ "lets give it 4", itemID));
					}
//					else if(timeOnPageFeedback(feedback, 30000, 50000)){
//						pref.setValue(4, 0);
//						pseudoRatings.add(String.format("%d_%d", pref.getUserID(), pref.getItemID()));
//					}
//					else if(timeOnMouseFeedback(feedback)){
//						pref.setValue(4, 0);
//						pseudoRatings.add(String.format("%d_%d", pref.getUserID(), pref.getItemID()));
////						System.out.println(String.format("User spent more than 15 seconds on item: %d, "
////								+ "lets give it 4", itemID));
//					}
//					else if (vals[1] < 17500 && vals[1] > 1000){
//						pref.setValue(2, 0);
//						pseudoRatings.add(String.format("%d_%d", pref.getUserID(), pref.getItemID()));
//					} 
//					else if(vals[2] > 700){
//						// with time on mouse = 700, most users give an article rating 4
//						pref.setValue(4,0);
//						System.out.println(String.format("User has used the mouse more than 7 seconds on item: %d, "
//								+ "lets give it 4", itemID));
//					}
				}
			}
		}
		System.out.println("");
        return model;
	}
	
	private double[] globalLR(SMDataModel model, int numberOfIndependentVariables) throws TasteException {
		
		if(numberOfIndependentVariables == 0) throw new NumberIsTooSmallException(numberOfIndependentVariables, 1, true);
		
		List<Double> tempExpl = new ArrayList<Double>();
		List<Double[]> tempImpl = new ArrayList<Double[]>();
		int implSize = numberOfIndependentVariables;
		Double[] impl = new Double[implSize];
		
		LongPrimitiveIterator it = model.getItemIDs();
		while (it.hasNext()) {
			
			long itemID = it.next();
			PreferenceArray prefs = model.getPreferencesForItem(itemID);
			SMPreference checkIVs = (SMPreference) prefs.get(0);
			if(checkIVs.getValues().length-1 < implSize){
				implSize = checkIVs.getValues().length-1;
			}
			
			for (int i = 0; i < prefs.length(); i++) {
				SMPreference p = (SMPreference) prefs.get(i);
				// ensure that we have explicit value
				if(p.getValue(RATING_INDEX) <= 0 || p.getValue(TIME_ON_PAGE_INDEX) <= 0 ||
						p.getValue(TIME_ON_PAGE_INDEX) > 140000) {
					continue;
				}
				
				tempExpl.add((double) p.getValue(RATING_INDEX));
				impl = new Double[implSize];
				for (int j = 0; j < impl.length; j++) {
					impl[j] = (double) p.getValue(j+1);
				}
				tempImpl.add(impl);
			}
		}
		
		if(tempExpl.size() != tempImpl.size()) throw new NumberFormatException("LR must have equal IV and DV sizes");
	
		double[] explRatings = new double[tempExpl.size()];
		double[][] implRatings = new double[tempImpl.size()][];
		for (int i = 0; i < explRatings.length; i++) {
			explRatings[i] = tempExpl.get(i);
			Double[] tempRatings = tempImpl.get(i);
			double[] ratings = new double[tempRatings.length];
			for (int j = 0; j < tempRatings.length; j++) {
				ratings[j] = tempRatings[j];
			}
			implRatings[i] = ratings;
		}
		
		OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
		regression.newSampleData(explRatings, implRatings);

		double[] beta = regression.estimateRegressionParameters();      
		
		System.out.println("Regression parameters:");
		for (int i = 0; i < beta.length; i++) {
				System.out.print("B"+i+": " + beta[i] + ", ");
		}
		System.out.println("");
		
		return beta;
	}

	/**
	 * Finds the pseudo rating given an article without explicit rating by using linear regression on the
	 * implicit feedback that correlates best with the ratings given by users.
	 * @param prefs
	 * 		all preferences for a given article
	 * @param currentPref
	 * 		the preference without explicit rating and hence will get a pseudo rating
	 * @param bestCorrelated
	 * 		index of the best correlated implicit feedback
	 * @param ratingPairs
	 * 		number of explicit and implicit feedback that is used in linear expression
	 * @return
	 * 		a pseudo rating based on linear regression
	 */
	private float getPseudoRatingLinearRegression(PreferenceArray prefs, SMPreference currentPref, int bestCorrelated, int ratingPairs){
		double[] explRatings = new double[ratingPairs];
		double[] implRatings = new double[ratingPairs];
		try {
			explRatings = getRatings(prefs,0);
			implRatings = getRatings(prefs, bestCorrelated); 
		} catch (NoSuchItemException e) {
			e.printStackTrace();
			System.err.println("Linear regression could not retrive explicit and/or implicit ratings");
		}
		
		TrendLine t = new PolyTrendLine(1);
		t.setValues(explRatings, implRatings);
		
		return (float) Math.round(t.predict(currentPref.getValue(bestCorrelated)));
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
	 * 		a pseudo rating based on closest neighbor
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
	 * 		a pseudo rating based on equal bins
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

	public double getCorrelation(PreferenceArray prefs, int implicitIndex) throws NoSuchItemException {
			double[] expl = getRatings(prefs, 0);
			double[] impl = getRatings(prefs, implicitIndex);
			
			PearsonsCorrelation pc = new PearsonsCorrelation();
			return pc.correlation(expl, impl);
	}
	
	/**
	 * 
	 * @param index
	 * 		0 = explicit, 1,2...n = implicit
	 * @return
	 * 		Returns all ratings for a given type of feedback given by the index parameter.
	 * 		Also ensure that every rating returned has an explicit rating.
	 * @throws NoSuchItemException
	 */
	public double[] getRatings(PreferenceArray prefs, int index) throws NoSuchItemException{
		
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
	
	public int getNumberOfRatingPairs(PreferenceArray prefs) {
		int pairs = 0;
		for (int i = 0; i < prefs.length(); i++) {
			SMPreference p = (SMPreference) prefs.get(i);
            // ensure that we have explicit value
            if(p.getValue(0) >= 1) {
                pairs++;
            }
		}
		
		return pairs;
	}
	
	public boolean timeOnPageAndTimeOnMouseCombined(float[] feedback, int pageMin, int pageMax, 
			int mouseMin, int mouseMax){
		return feedback[TIME_ON_PAGE_INDEX] > pageMin && feedback[TIME_ON_PAGE_INDEX] < pageMax && 
				feedback[TIME_ON_MOUSE_INDEX] > mouseMin && feedback[TIME_ON_MOUSE_INDEX] < mouseMax;
	}
	
	public boolean timeOnPageFeedback(float[] feedback, int min, int max){
		return feedback[TIME_ON_PAGE_INDEX] > min && feedback[TIME_ON_PAGE_INDEX] < max;
	}
	
	public boolean timeOnMouseFeedback(float[] feedback, int min, int max){
		return feedback[TIME_ON_MOUSE_INDEX] > min && feedback[TIME_ON_MOUSE_INDEX] < max;
	}
	
	//TODO: fix a better way of finding which implicit feedback we can use to create pseudo ratings
	public List<double[]> getImplicitFeedbackWithEnoughRatingPairs(PreferenceArray prefs, int feedbackLength){
		List<double[]> feedbacks = new ArrayList<double[]>();
		
		// create a list of ratings since we don't know how many rating-pairs exist
		for (int j = 1; j < feedbackLength; j++) {
			List<Double> tempExplRatings = new ArrayList<Double>();
			List<Double> tempImplRatings = new ArrayList<Double>();
			for (int i = 0; i < prefs.length(); i++) {
				SMPreference p = (SMPreference) prefs.get(i);
				// ensure that we have explicit value
				if(p.getValue(0) >= 0 && p.getValue(j) >= 0) {
					tempExplRatings.add((double) p.getValue(RATING_INDEX));
					tempImplRatings.add((double) p.getValue(j));
				}
			}
			if(tempExplRatings.size() >= THRESHOLD && tempImplRatings.size() >= THRESHOLD){
				double[] explRatings = new double[tempExplRatings.size()];
				double[] implRatings = new double[tempImplRatings.size()];
				
				for (int i = 0; i < implRatings.length; i++) {
					explRatings[i] = tempExplRatings.get(i);
					implRatings[i] = tempImplRatings.get(i);
				}
			}
		}
		return null;
	}


}
