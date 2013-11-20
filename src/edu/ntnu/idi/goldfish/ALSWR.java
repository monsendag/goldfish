package edu.ntnu.idi.goldfish;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.recommender.svd.ALSWRFactorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.Factorizer;
import org.apache.mahout.cf.taste.model.DataModel;

public class ALSWR extends MatrixFactorization {

	private double lambda;
	private boolean usesImplicitFeedback;
	private double alpha;

	public ALSWR(int topN, int numFeatures, int numIterations, double lambda, boolean usesImplicitFeedback, double alpha) {
		super(topN, numFeatures, numIterations);
		this.lambda = lambda;
		this.usesImplicitFeedback = usesImplicitFeedback;
		this.alpha = alpha;
	}

	@Override
	public Factorizer getFactorizer(DataModel dataModel) throws TasteException {
		return new ALSWRFactorizer(dataModel, (int) KTL, lambda, numIterations, usesImplicitFeedback, alpha);
	}
	
	public String toString() {
		return String.format("ALSWR");
	}
	

}
