package edu.ntnu.idi.goldfish;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.recommender.svd.Factorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.ParallelSGDFactorizer;
import org.apache.mahout.cf.taste.model.DataModel;

public class ParallelSGD extends MatrixFactorization {

	double lambda;
	private double mu0;
	private double decayFactor;
	private int stepOffset;
	private double forgettingExponent;
	private double biasMuRatio;
	private double biasLambdaRatio;
	
	public ParallelSGD(int topN, int numFeatures, int numIterations, double lambda, double mu0, double decayFactor, int stepOffset, double forgettingExponent, double biasMuRatio, double biasLambdaRatio) {
		super(topN, numFeatures, numIterations);
		this.lambda = lambda;
		this.mu0 = mu0;
		this.decayFactor = decayFactor;
		this.stepOffset = stepOffset;
		this.forgettingExponent = forgettingExponent;
		this.biasMuRatio = biasMuRatio;
		this.biasLambdaRatio = biasLambdaRatio;
	}

	@Override
	public Factorizer getFactorizer(DataModel dataModel) throws TasteException {
		return new ParallelSGDFactorizer(dataModel, (int) KTL, lambda, numIterations, mu0, decayFactor, stepOffset, forgettingExponent, biasMuRatio, biasLambdaRatio);
	}
	
	public String toString() {
		return String.format("ParallelSGD");
	}

}
