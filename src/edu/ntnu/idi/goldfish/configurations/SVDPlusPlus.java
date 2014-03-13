package edu.ntnu.idi.goldfish.configurations;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.recommender.svd.Factorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDPlusPlusFactorizer;
import org.apache.mahout.cf.taste.model.DataModel;

public class SVDPlusPlus extends MatrixFactorization {

	private double learningRate;
	private double preventOverfitting;
	private double randomNoise;
	private double learningRateDecay;

	public SVDPlusPlus(int topN, int numFeatures, int numIterations, double learningRate, double preventOverfitting, double randomNoise, double learningRateDecay) {
		super(topN, numFeatures, numIterations);
		this.learningRate = learningRate;
		this.preventOverfitting = preventOverfitting;
		this.randomNoise = randomNoise;
		this.learningRateDecay = learningRateDecay;;
	}

	@Override
	public Factorizer getFactorizer(DataModel dataModel) throws TasteException {
		return new SVDPlusPlusFactorizer(dataModel, (int) KTL, learningRate, preventOverfitting, randomNoise, numIterations, learningRateDecay);
	}
	
	public String toString() {
		return String.format("SVDPlusPlus");
	}
}
