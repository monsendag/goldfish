package edu.ntnu.idi.goldfish.preprocessors;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.DataModel;

import edu.ntnu.idi.goldfish.mahout.DBModel;
import edu.ntnu.idi.goldfish.mahout.DBModel.DBRow;

public class PreprocessorStat extends Preprocessor{

	private final int THRESHOLD = 3;
	
	public static enum PredictionMethod { LinearRegression, ClosestNeighbor, EqualBins }
	
	public DataModel preprocess(DBModel model, PredictionMethod predictionMethod) throws TasteException {
		
		List<DBModel.DBRow> results = model.getFeedbackRows().stream().filter(row -> row.rating == 0).collect(Collectors.toList());
		for (DBRow r : results) {
			long itemID = r.itemid;
			List<DBModel.DBRow> feedbackForItemID = model.getFeedbackRows().stream()
					.filter(row -> row.rating > 0)
					.filter(row -> row.itemid == itemID)
					.collect(Collectors.toList());
			
			if(feedbackForItemID.size() >= THRESHOLD){
				
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
				
				if(correlation > 0.5){
					float pseudoRating = 0;
					switch (predictionMethod) {
					case LinearRegression:
						pseudoRating = getPseudoRatingLinearRegression(dependentVariables, independentVariables, bestCorrelated, r.implicitfeedback);
						break;
					case ClosestNeighbor:
						pseudoRating = getPseudoRatingClosestNeighbor(independentVariables, bestCorrelated, r.implicitfeedback);
						break;
					case EqualBins:
						pseudoRating = getPseudoRatingEqualBins(independentVariables, bestCorrelated, r.implicitfeedback, correlation);
						break;
					default:
						pseudoRating = getPseudoRatingLinearRegression(dependentVariables, independentVariables, bestCorrelated, r.implicitfeedback);
						break;
					}
					
					model.setPreference(r.userid, r.itemid, (float) Math.round(pseudoRating));
					pseudoRatings.add(String.format("%d_%d", r.userid, r.itemid));
				}
				else if(timeOnPageFeedback(r.implicitfeedback, 20000, 120000)){
					model.setPreference(r.userid, r.itemid, 4);
					pseudoRatings.add(String.format("%d_%d", r.userid, r.itemid));
				}
			}
		}
		
		return model;
	}
	
	@Override
	protected DataModel preprocess(DBModel model) throws Exception {
		// Default: Linear Regression
		return preprocess(model, PredictionMethod.LinearRegression);
	}
	
	public double getCorrelation(double[] dv, double[][] iv, int feedbackIndex){
		TrendLine t = new PolyTrendLine(1);
		double[] itemFeedback = new double[iv.length];
		for (int i = 0; i < iv.length; i++) {
			itemFeedback[i] = iv[i][feedbackIndex];
		}
		t.setValues(dv, itemFeedback);

		PearsonsCorrelation pc = new PearsonsCorrelation();
		return Math.abs(pc.correlation(dv, itemFeedback));
	}
	
	public int getBestCorrelated(double[] dv, double[][] iv){
		int bestCorrelated = -1;
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
	
	private float getPseudoRatingClosestNeighbor(double[][] iv, int bestCorrelated, float[] implicitFeedback) {
		float diff = Float.MAX_VALUE;
		float closestPref = 0;
		
		for (int i = 0; i < iv.length; i++) {
			float tempDiff = (float) Math.abs(implicitFeedback[bestCorrelated] - iv[i][bestCorrelated]);
			if(tempDiff < diff){
				diff = tempDiff;
				closestPref = (float) iv[i][bestCorrelated];
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
