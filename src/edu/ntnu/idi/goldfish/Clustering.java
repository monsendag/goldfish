package edu.ntnu.idi.goldfish;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.model.DataModel;

public class Clustering extends ModelBased {

	public RecommenderBuilder getBuilder() {
		return new RecommenderBuilder() {
			public org.apache.mahout.cf.taste.recommender.Recommender buildRecommender(DataModel dataModel) throws TasteException {
				
				// build clustering based recommender
				return null;
			}
		};
	}

}
