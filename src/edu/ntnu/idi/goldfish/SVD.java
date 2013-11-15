package edu.ntnu.idi.goldfish;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;

import org.apache.mahout.cf.taste.impl.recommender.svd.SVDRecommender;
import org.apache.mahout.cf.taste.impl.recommender.svd.ALSWRFactorizer;
import org.apache.mahout.cf.taste.model.DataModel;

public class SVD extends Evaluation {
	
	double lambda = 0.05;
	int iterations = 10;
	
	public SVD(int topN, int numFeatures, double lambda, int iterations) {
		this.topN = topN;
		this.KTL = numFeatures;
		this.lambda = lambda;
		this.iterations = iterations;
	}

	public RecommenderBuilder getBuilder() {
		return new RecommenderBuilder() {
			public org.apache.mahout.cf.taste.recommender.Recommender buildRecommender(DataModel dataModel) throws TasteException {	
				return new SVDRecommender(dataModel, new ALSWRFactorizer(dataModel, (int) KTL, lambda, iterations));
			}
		};
	}
	
	public String toString() {
		return "SVD";
	}
}
