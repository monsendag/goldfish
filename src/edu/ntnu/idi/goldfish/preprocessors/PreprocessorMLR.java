package edu.ntnu.idi.goldfish.preprocessors;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to map implicit feedback to explicit rating where it is possible.
 * @author Patrick Romstad and Dag Einar Monsen
 *
 */
public class PreprocessorMLR extends Preprocessor {

	private final int THRESHOLD = 3;
	private final int TIME_ON_PAGE_INDEX = 1;
	private final int TIME_ON_MOUSE_INDEX = 2;
	private final int RATING_INDEX = 0;
	
	private Map<String, Float> correlations = new HashMap<String, Float>();


	public static DataModel getPreprocessedDataModel(String path) throws Exception {
		YowModel model;
		model = new YowModel(new File(path));
		PreprocessorMLR pre = new PreprocessorMLR();
		pre.preprocess(model);
		return model;
	}


	public DataModel preprocess(YowModel model) {
		try {
			
			double[] beta = globalLR(model, 2);
			
			LongPrimitiveIterator it = model.getItemIDs();
			while(it.hasNext()){
				long itemID = it.next();
				PreferenceArray prefs = model.getPreferencesForItem(itemID);
				
				for (Preference p : prefs) {
					
					SMPreference pref = (SMPreference) p;
					boolean hasExplicit = pref.getValue(RATING_INDEX) >= 1;
					if(hasExplicit){
						// No need to add a pseudo rating
						continue;
					}
					
					float[] feedback = pref.getValues();
					
					float pseudoRating = (float) beta[0];
					for (int i = 1; i < beta.length; i++) {
						pseudoRating += beta[i]*feedback[i];
					}
					pref.setValue(Math.round(pseudoRating), RATING_INDEX);
					pseudoRatings.add(String.format("%d_%d", pref.getUserID(), pref.getItemID()));
				}
			}
		} catch (TasteException e) {
			e.printStackTrace();
			System.err.println("Could not get all items from the yow model");
		}
        return null;
		
	}
	
	private double[] globalLR(YowModel model, int numberOfIndependentVariables) throws TasteException {
		
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
				if(p.getValue(RATING_INDEX) <= 0 || p.getValue(TIME_ON_PAGE_INDEX) <= 0) {
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

	

}
