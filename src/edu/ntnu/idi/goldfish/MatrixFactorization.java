package edu.ntnu.idi.goldfish;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;

import org.apache.mahout.cf.taste.impl.recommender.svd.Factorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDRecommender;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.Recommender;

public abstract class MatrixFactorization extends Evaluation {

	int numIterations;
	
	public MatrixFactorization(int topN, int numFeatures, int numIterations) {
		super(topN, numFeatures);
		this.numIterations = numIterations;
	}

	public RecommenderBuilder getBuilder() {
		return new RecommenderBuilder() {
			public Recommender buildRecommender(DataModel dataModel) throws TasteException {	
				return new SVDRecommender(dataModel, getFactorizer(dataModel));			
			}
		};
	}
	
	public abstract Factorizer getFactorizer(DataModel dataModel) throws TasteException;
}
