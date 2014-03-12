package edu.ntnu.idi.goldfish;

import edu.ntnu.idi.goldfish.mahout.KiwiRecommender;
import edu.ntnu.idi.goldfish.mahout.SMDataModel;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.DataModelBuilder;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.recommender.Recommender;

public class KiwiEvaluation extends Evaluation{

	private double[] weights;
	private double[] latentFactors;
	
	public KiwiEvaluation(int topN, double[] weights, double[] latentFactors) {
		super(topN, 0);
		this.weights = weights;
		this.latentFactors = latentFactors;
	}

	public RecommenderBuilder getRecommenderBuilder() {
		return new RecommenderBuilder() {
			public Recommender buildRecommender(DataModel dataModel) throws TasteException {	
				return new KiwiRecommender(dataModel, weights, latentFactors);			
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
