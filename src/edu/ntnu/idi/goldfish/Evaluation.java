package edu.ntnu.idi.goldfish;

import org.apache.mahout.cf.taste.eval.RecommenderBuilder;

public abstract class Evaluation {

	/**
	 * Used for calculating Precision and Recall metrics
	 */
	public int topN;
	/**
	 * K as in kNN
	 * T as in Threshold 
	 * L as in Latent factors
	 */
	public double KTL;
	
	public Evaluation(int topN, double KTL) {
		this.topN = topN;
		this.KTL = KTL;
	}
	
	public abstract RecommenderBuilder getBuilder();
	
	public double getKTL() {
		return KTL;
	}
	
	public int getTopN() {
		return topN;
	}
	
	public String getSimilarity() {
		return this instanceof MemoryBased ? ((MemoryBased) this).similarity.toString() : "";
	}
}
