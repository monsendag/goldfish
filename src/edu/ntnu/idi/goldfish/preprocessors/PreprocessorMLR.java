package edu.ntnu.idi.goldfish.preprocessors;

import edu.ntnu.idi.goldfish.configurations.Config;
import edu.ntnu.idi.goldfish.mahout.DBModel;
import edu.ntnu.idi.goldfish.mahout.DBModel.DBRow;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.DataModel;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class is used to map implicit feedback to explicit rating using multiple linear regression
 * @author Patrick Romstad and Dag Einar Monsen
 *
 */
public class PreprocessorMLR extends Preprocessor {

	public DataModel preprocess(Config config) throws TasteException {
		DBModel model = config.get("model");
		int numberOfIndependentVariables = config.get("numberOfIndependentVariables");
		if(numberOfIndependentVariables == 0) throw new NumberIsTooSmallException(numberOfIndependentVariables, 1, true);
		
		List<DBModel.DBRow> allResults = model.getFeedbackRows();
		List<DBModel.DBRow> results = allResults.stream().filter(row -> row.rating == 0).collect(Collectors.toList());
		
		numberOfIndependentVariables = results.get(0).implicitfeedback.length < numberOfIndependentVariables ? 
				results.get(0).implicitfeedback.length : numberOfIndependentVariables;

		double[] beta = globalLR(allResults, numberOfIndependentVariables);
		
		for(DBModel.DBRow row : results) {
			
			float pseudoRating = (float) beta[0];
			for (int i = 0; i < numberOfIndependentVariables; i++) {
				pseudoRating += beta[i] * row.implicitfeedback[i];
			}
			
			// is pseudo rating > 5, then outlier feedback has been used and we don't want to use this pseudo rating
			if(pseudoRating > 5) continue;
			
//			System.out.format("linear regression: u: %d  i: %6d  estimate: %d\n", row.userid, row.itemid, Math.round(pseudoRating));
			
			model.setPreference(row.userid, row.itemid, (float) Math.round(pseudoRating));
			pseudoRatings.add(String.format("%d_%d", row.userid, row.itemid));
		}
		
		return model;
	}
	
	private double[] globalLR(List<DBModel.DBRow> allResults, int numberOfIndependentVariables) throws TasteException {
		List<DBModel.DBRow> results = allResults.stream().filter(row -> row.rating > 0).collect(Collectors.toList());
		
		double[] dependentVariables = new double[results.size()]; // the explicit ratings to infer
		double[][] independentVariables = new double[results.size()][]; // the implicit feedback
		double[] implicitFeedback = null;
		int index = 0;
		
		for (DBRow row : results) {
			dependentVariables[index] = row.rating;
			
			implicitFeedback = new double[numberOfIndependentVariables];
			for (int i = 0; i < implicitFeedback.length; i++) {
				implicitFeedback[i] = row.implicitfeedback[i];
			}
			independentVariables[index] = implicitFeedback;
			index++;
		}
		
		OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
		regression.newSampleData(dependentVariables, independentVariables);

		double[] beta = regression.estimateRegressionParameters();      
		
		return beta;
	}
}
