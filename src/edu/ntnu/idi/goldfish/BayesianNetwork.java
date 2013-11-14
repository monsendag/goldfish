package edu.ntnu.idi.goldfish;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;

import org.apache.mahout.cf.taste.impl.recommender.svd.SVDRecommender;
import org.apache.mahout.cf.taste.impl.recommender.svd.ALSWRFactorizer;
import org.apache.mahout.cf.taste.model.DataModel;

public class BayesianNetwork extends ModelBased {

	public RecommenderBuilder getBuilder() {
		return new RecommenderBuilder() {
			public org.apache.mahout.cf.taste.recommender.Recommender buildRecommender(DataModel dataModel) throws TasteException {	

				//return new BayesianNetwork();
				//return new SVDRecommender(dataModel, new ALSWRFactorizer(dataModel, 10, 0.05, 10));

				return null;			
			}
		};
	}

	public String toString(boolean min) {
		if(!min) return toString();
		// TODO Auto-generated method stub
		return "BN";
	}

	public String toString() {
		return "BayesianNetwork";
	}
}
