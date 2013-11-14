package edu.ntnu.idi.goldfish;

import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

public class Threshold extends MemoryBased {
		
        double threshold = 1;

	public Threshold(Similarity similarity, double threshold) {
		this.similarity = similarity;
		this.threshold = threshold;
	}
	
	public UserNeighborhood getNeighborhood(UserSimilarity similarityObject, DataModel dataModel) {
		return new ThresholdUserNeighborhood(threshold, similarityObject, dataModel);
	}
	
	public String toString(boolean min) {
		if(!min) return this.toString();
		// get uppercase letters in similarity
		String similarityIntitials = this.similarity.toString().replaceAll("[a-z]", "");
		return String.format("%.2fTh/%s", threshold, similarityIntitials);
	}
	
	public String toString() {
		return String.format("%.2f Threshold (%s)", threshold, this.similarity.toString());
	}
}

