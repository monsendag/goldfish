package edu.ntnu.idi.goldfish;

import org.apache.mahout.cf.taste.eval.RecommenderBuilder;

public abstract class Evaluation {

	public int topN;
	public double KTL;
	
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
