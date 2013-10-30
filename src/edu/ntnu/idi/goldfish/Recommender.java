package edu.ntnu.idi.goldfish;

import org.apache.mahout.cf.taste.eval.RecommenderBuilder;

public interface Recommender {
	
	
	public RecommenderBuilder getBuilder();
}
