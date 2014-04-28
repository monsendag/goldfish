package edu.ntnu.idi.goldfish.preprocessors;

import edu.ntnu.idi.goldfish.configurations.Config;
import edu.ntnu.idi.goldfish.mahout.DBModel;
import edu.ntnu.idi.goldfish.mahout.DBModel.DBRow;
import org.apache.mahout.cf.taste.common.TasteException;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class PreprocessorTime extends Preprocessor{

	public static enum PredictionMethod { LinearRegression, ClosestNeighbor, EqualBins }
	
	public DBModel getProcessedModel(Config config) throws TasteException, IOException {
        DBModel model = config.get("model");
        int minTimeOnPage = config.get("minTimeOnPage");
        int rating = config.get("rating");
        
		List<DBModel.DBRow> allResults = model.getFeedbackRows();
		List<DBModel.DBRow> results = allResults.stream().filter(row -> row.rating == 0).collect(Collectors.toList());
		for (DBRow r : results) {

			if(timeOnPageFeedback(r.implicitfeedback, minTimeOnPage, 120000)){
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
	
	public boolean timeOnPageFeedback(float[] feedback, int min, int max){
		return feedback[0] > min && feedback[0] < max;
	}

}
