package edu.ntnu.idi.goldfish.preprocessors;

import edu.ntnu.idi.goldfish.mahout.DBModel;
import edu.ntnu.idi.goldfish.mahout.DBModel.DBRow;
import edu.ntnu.idi.goldfish.mahout.SMPreference;

import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;

import weka.core.Instance;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class is used to map implicit feedback to explicit rating where it is possible.
 * @author Patrick Romstad and Dag Einar Monsen
 *
 */
public class PreprocessorMLR extends Preprocessor {

	private final int TIME_ON_PAGE_INDEX = 1;
	private final int TIME_ON_MOUSE_INDEX = 2;
	private final int RATING_INDEX = 0;
	
	private Map<String, Float> correlations = new HashMap<String, Float>();

	public static void main(String[] args) throws Exception {
		DataModel model = PreprocessorMLR.getPreprocessedDataModel("datasets/yow-userstudy/python/yow-smart-sample-implicit-2.csv");
	}

	public static DataModel getPreprocessedDataModel(String path) throws Exception {
		DBModel model;
		model = new DBModel(new File(path));
		PreprocessorMLR pre = new PreprocessorMLR();
		pre.preprocess(model, 2);
		return model;
	}


	public DataModel preprocess(DBModel model, int numberOfIndependentVariables) throws TasteException {
		
		double[] beta = globalLR(model, numberOfIndependentVariables);
		
		List<DBModel.DBRow> results = model.getFeedbackRows().stream().filter(row -> row.rating == 0).collect(Collectors.toList());
		for(DBModel.DBRow row : results) {
			
			float pseudoRating = (float) beta[RATING_INDEX];
			pseudoRating += beta[TIME_ON_PAGE_INDEX] * row.timeonpage;
			pseudoRating += beta[TIME_ON_MOUSE_INDEX] * row.timeonmouse;
			
			// the beta0 is 3, have to manually set the lowest ratings
			pseudoRating = row.timeonpage < 25000 ? 2 : pseudoRating;
			pseudoRating = row.timeonpage < 10000 ? 1 : pseudoRating;
			
			// is pseudorating > 5, then outlier feedback has been used
			pseudoRating = pseudoRating > 5 ? -1 : pseudoRating;
			
			System.out.format("linear regression: u: %d  i: %6d  estimate: %d\n", row.userid, row.itemid, Math.round(pseudoRating));
			
			model.setPreference(row.userid, row.itemid, (float) Math.round(pseudoRating));
			pseudoRatings.add(String.format("%d_%d", row.userid, row.itemid));
		}
		
		return model;
	}
	
	private double[] globalLR(DBModel model, int numberOfIndependentVariables) throws TasteException {
		
		if(numberOfIndependentVariables == 0) throw new NumberIsTooSmallException(numberOfIndependentVariables, 1, true);
		
		List<DBModel.DBRow> results = model.getFeedbackRows().stream().filter(row -> row.rating > 0).collect(Collectors.toList());
		double[] dependentVariables = new double[results.size()]; // the explicit ratings to infer
		double[][] independentVariables = new double[results.size()][]; // the implicit feedback
		double[] implicitFeedback = null;
		int index = 0;
		
		for (DBRow row : results) {
			dependentVariables[index] = row.rating;
			
			implicitFeedback = new double[2];
			implicitFeedback[0] = row.timeonpage;
			implicitFeedback[1] = row.timeonmouse;
			independentVariables[index] = implicitFeedback;
			index++;
		}
		
		OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
		regression.newSampleData(dependentVariables, independentVariables);

		double[] beta = regression.estimateRegressionParameters();      
		
		System.out.println("Regression parameters:");
		for (int i = 0; i < beta.length; i++) {
				System.out.print("B"+i+": " + beta[i] + ", ");
		}
		System.out.println("");
		
		return beta;
	}


	@Override
	protected DataModel preprocess(DBModel model) throws Exception {
		// time on page is default
		return preprocess(model, 1); 
	}

	

}
