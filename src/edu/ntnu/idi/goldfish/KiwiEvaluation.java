package edu.ntnu.idi.goldfish;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.DataModelBuilder;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.recommender.Recommender;

import edu.ntnu.idi.goldfish.mahout.KiwiRecommender;
import edu.ntnu.idi.goldfish.mahout.SMDataModel;

public class KiwiEvaluation extends Evaluation{

	
	public KiwiEvaluation(int topN, int numFeatures) {
		super(topN, numFeatures);
	}

	public RecommenderBuilder getRecommenderBuilder() {
		return new RecommenderBuilder() {
			public Recommender buildRecommender(DataModel dataModel) throws TasteException {	
				return new KiwiRecommender(dataModel);			
			}
		};
	}
	
	public DataModelBuilder getModelBuilder() {
		return new DataModelBuilder() {
			public DataModel buildDataModel(FastByIDMap<PreferenceArray> trainingData) {
				return new SMDataModel(trainingData);
			}
		};
	}
		
	public String toString() {
		return "Kiwi";
	}
}
