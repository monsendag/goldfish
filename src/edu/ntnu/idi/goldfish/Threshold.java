package edu.ntnu.idi.goldfish;

import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

public class Threshold extends MemoryBased {

	public Threshold(int topN, Similarity similarity, double threshold) {
		super(topN, threshold, similarity);
	}
	
	public UserNeighborhood getNeighborhood(UserSimilarity similarityObject, DataModel dataModel) {
		return new ThresholdUserNeighborhood(KTL, similarityObject, dataModel);
	}
	
	public String toString(boolean min) {
		if(!min) return this.toString();
		// get uppercase letters in similarity
		String similarityIntitials = this.similarity.toString().replaceAll("[a-z]", "");
		return String.format("%.2fTh/%s", KTL, similarityIntitials);
	}
	
	public String toString() {
		return "Threshold";
	}

}

