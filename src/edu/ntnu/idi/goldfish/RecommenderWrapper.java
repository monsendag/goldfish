package edu.ntnu.idi.goldfish;

import org.apache.mahout.cf.taste.eval.RecommenderBuilder;

public interface RecommenderWrapper {
	
	
	public RecommenderBuilder getBuilder();
	
	public String toString(boolean min);
	
	public double getKTL();
}
