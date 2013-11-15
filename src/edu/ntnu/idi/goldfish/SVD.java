package edu.ntnu.idi.goldfish;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;

import org.apache.mahout.cf.taste.impl.recommender.svd.SVDRecommender;
import org.apache.mahout.cf.taste.impl.recommender.svd.ALSWRFactorizer;
import org.apache.mahout.cf.taste.model.DataModel;

public class SVD extends ModelBased {
	
	int numFeatures = 10;
	double lambda = 0.05;
	int iterations = 10;
	
	public SVD(int numFeatures, double lambda, int iterations) {
		this.numFeatures = numFeatures;
		this.lambda = lambda;
		this.iterations = iterations;
	}

	public RecommenderBuilder getBuilder() {
		return new RecommenderBuilder() {
			public org.apache.mahout.cf.taste.recommender.Recommender buildRecommender(DataModel dataModel) throws TasteException {	

				// num features, 
				// lambda
				// numiterations
				return new SVDRecommender(dataModel, new ALSWRFactorizer(dataModel, numFeatures, lambda, iterations));
			
			}
		};
	}

	public String toString(boolean min) {
		return "SVD";
	}
	
	public String toString() {
		return "SVD";
		
	}

	public double getKTL() {
		// TODO Auto-generated method stub
		return numFeatures;
	}

}
