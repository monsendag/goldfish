package edu.ntnu.idi.goldfish.preprocessors;

import edu.ntnu.idi.goldfish.configurations.Config;
import edu.ntnu.idi.goldfish.mahout.DBModel;
import edu.ntnu.idi.goldfish.mahout.DBModel.DBRow;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.mahout.cf.taste.common.TasteException;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class PreprocessorStat extends Preprocessor{

	private final int THRESHOLD = 3;
	
	public static enum PredictionMethod { LinearRegression, ClosestNeighbor, EqualBins }
	
	public DBModel getProcessedModel(Config config) throws TasteException, IOException {
        DBModel model = config.get("model");
        PredictionMethod predictionMethod = config.get("predictionMethod");
        int minTimeOnPage = config.get("minTimeOnPage");
        double correlationLimit = config.get("correlationLimit");
        int rating = config.get("rating");
        
		List<DBModel.DBRow> allResults = model.getFeedbackRows();
		List<DBModel.DBRow> results = allResults.stream().filter(row -> row.rating == 0).collect(Collectors.toList());
		for (DBRow r : results) {
			long itemID = r.itemid;
			
			List<DBModel.DBRow> feedbackForItemID = allResults.stream()
					.filter(row -> row.itemid == itemID)
					.filter(row -> row.rating > 0)
					.collect(Collectors.toList());
			
			if(hasImplicit(r.implicitfeedback) && enoughImplicitFeedback(feedbackForItemID)){
				
				double[] dependentVariables = new double[feedbackForItemID.size()]; // the explicit ratings to infer
				double[][] independentVariables = new double[feedbackForItemID.size()][]; // the implicit feedback
				double[] implicitFeedback = null;
				int index = 0;
				
				for (DBRow f : feedbackForItemID) {
					dependentVariables[index] = f.rating;
					
					implicitFeedback = new double[f.implicitfeedback.length];
					for (int i = 0; i < implicitFeedback.length; i++) {
						implicitFeedback[i] = f.implicitfeedback[i];
					}
					independentVariables[index] = implicitFeedback;
					index++;
				}
				
				
				int bestCorrelated = getBestCorrelated(dependentVariables, independentVariables);
				double correlation = getCorrelation(dependentVariables, independentVariables, bestCorrelated);
				
				if(correlation > correlationLimit){
					float pseudoRating = 0;
					switch (predictionMethod) {
					case LinearRegression:
						pseudoRating = getPseudoRatingLinearRegression(dependentVariables, independentVariables, bestCorrelated, r.implicitfeedback);
						break;
					case ClosestNeighbor:
						pseudoRating = getPseudoRatingClosestNeighbor(dependentVariables, independentVariables, bestCorrelated, r.implicitfeedback);
						break;
					case EqualBins:
						pseudoRating = getPseudoRatingEqualBins(independentVariables, bestCorrelated, r.implicitfeedback, correlation);
						break;
					default:
						pseudoRating = getPseudoRatingLinearRegression(dependentVariables, independentVariables, bestCorrelated, r.implicitfeedback);
						break;
					}
					
					model.setPreference(r.userid, r.itemid, (float) Math.round(pseudoRating));
					addPseudoPref(r.userid, r.itemid);
					
//					System.out.println(String.format("%d, %d, %.0f", r.userid, r.itemid, pseudoRating));
				}
			}
			else if(timeOnPageFeedback(r.implicitfeedback, minTimeOnPage, 120000)){
				model.setPreference(r.userid, r.itemid, rating);
				addPseudoPref(r.userid, r.itemid);
			}
		}
		return model;
	}
	
	public boolean hasImplicit(float[] implicitfeedback){
		for (int i = 0; i < implicitfeedback.length; i++) {
			if(implicitfeedback[i] > 0) return true;
		}
		return false;
	}
	
	public boolean enoughImplicitFeedback(List<DBModel.DBRow> feedbackForItemID ){
		int[] feedbackCount = new int[feedbackForItemID.get(0).implicitfeedback.length];
		for (DBRow row : feedbackForItemID) {
			for (int i = 0; i < row.implicitfeedback.length; i++) {
				if(row.implicitfeedback[i] > 0) {
					feedbackCount[i] += 1;
				}
			}
		}
		
		for (int i = 0; i < feedbackCount.length; i++) {
			if(feedbackCount[i] >= THRESHOLD) return true;
		}
		return false;
	}
	
	public double getCorrelation(double[] dv, double[][] iv, int feedbackIndex){
		double[] itemFeedback = new double[iv.length];
		for (int i = 0; i < iv.length; i++) {
			itemFeedback[i] = iv[i][feedbackIndex];
		}

		PearsonsCorrelation pc = new PearsonsCorrelation();
		return Math.abs(pc.correlation(dv, itemFeedback));
	}
	
	public int getBestCorrelated(double[] dv, double[][] iv){
		int bestCorrelated = 0;
		double maxCorr = 0;
		double tempCorr = 0;
		
		for (int i = 0; i < iv[0].length; i++) {
			tempCorr = getCorrelation(dv, iv, i);
			if(tempCorr > maxCorr){
				maxCorr = tempCorr;
				bestCorrelated = i;
			}
		}

		return bestCorrelated;
	}
	
	private float getPseudoRatingLinearRegression(double[] dv, double[][] iv, int bestCorrelated, float[] implicitfeedback){
		double[] itemFeedback = new double[iv.length];
		for (int i = 0; i < iv.length; i++) {
			itemFeedback[i] = iv[i][bestCorrelated];
		}
		
		TrendLine t = new PolyTrendLine(1);
		t.setValues(dv, itemFeedback);
		
		return (float) Math.round(t.predict(implicitfeedback[bestCorrelated]));
	}
	
	private float getPseudoRatingClosestNeighbor(double[] dv, double[][] iv, int bestCorrelated, float[] implicitFeedback) {
		float diff = Float.MAX_VALUE;
		float closestPref = 0;
		
		for (int i = 0; i < iv.length; i++) {
			float tempDiff = (float) Math.abs(implicitFeedback[bestCorrelated] - iv[i][bestCorrelated]);
			if(tempDiff < diff){
				diff = tempDiff;
				closestPref = (float) dv[i];
			}
		}

		return closestPref;
	}
	
	private float getPseudoRatingEqualBins(double[][] iv, int bestCorrelated, float[] implicitFeedback, double correlation) {
		float min = Integer.MAX_VALUE;
		float max = Integer.MIN_VALUE;
		float pseudoRating;
		
		for (int i = 0; i < iv.length; i++) {
			if(iv[i][bestCorrelated] < min){
				min = (float) iv[i][bestCorrelated];
			}
			if(iv[i][bestCorrelated] > max){
				max = (float) iv[i][bestCorrelated];
			}
		}
		 		
		if (min == max) {
			return 3;
		}
		
		if(implicitFeedback[bestCorrelated] < min){
			pseudoRating = 1;
		} else if(implicitFeedback[bestCorrelated] > max) {
			pseudoRating = 5;
		} else{
			float binSize = (max-min)/5;
			float dif = implicitFeedback[bestCorrelated] - min;
			pseudoRating = (float) (1 + Math.floor(dif/binSize));
		}
		 			 		
		if (correlation < 0) {
			pseudoRating = 6 - pseudoRating;
		}
		 			 
		return pseudoRating;
	}
	
	public boolean timeOnPageFeedback(float[] feedback, int min, int max){
		return feedback[0] > min && feedback[0] < max;
	}

}
