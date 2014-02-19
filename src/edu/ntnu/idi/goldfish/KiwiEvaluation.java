package edu.ntnu.idi.goldfish;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.Recommender;

import edu.ntnu.idi.goldfish.mahout.KiwiRecommender;

public class KiwiEvaluation extends Evaluation{

	
	public KiwiEvaluation(int topN, int numFeatures) {
		super(topN, numFeatures);
	}

	public RecommenderBuilder getBuilder() {
		return new RecommenderBuilder() {
			public Recommender buildRecommender(DataModel dataModel) throws TasteException {	
				return new KiwiRecommender();			
			}
		};
	}
		
	public String toString() {
		return "Kiwi";
	}
}
