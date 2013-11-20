package edu.ntnu.idi.goldfish;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.recommender.svd.Factorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.RatingSGDFactorizer;
import org.apache.mahout.cf.taste.model.DataModel;

public class RatingSGD extends MatrixFactorization {

	private double learningRate;
	private double preventOverfitting;
	private double randomNoise;
	private double learningRateDecay;

	public RatingSGD(int topN, int numFeatures, int iterations, double learningRate, double preventOverfitting, double randomNoise, double learningRateDecay) {
		super(topN, numFeatures, iterations);
		this.learningRate = learningRate;
		this.preventOverfitting = preventOverfitting;
		this.randomNoise = randomNoise;
		this.learningRateDecay = learningRateDecay;
	}

	@Override
	public Factorizer getFactorizer(DataModel dataModel) throws TasteException {
		return new RatingSGDFactorizer(dataModel, (int) KTL, learningRate, preventOverfitting, randomNoise, numIterations, learningRateDecay);
	}

	public String toString() {
		return String.format("RatingSGD");
	}
}
