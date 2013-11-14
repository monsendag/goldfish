package edu.ntnu.idi.goldfish;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

public class KNN extends MemoryBased {
	
	int neighborhoodSize = 1;

	public KNN(Similarity similarity, int neighborhoodSize) {
		this.similarity = similarity;
		this.neighborhoodSize = neighborhoodSize;
		
	}
	
	public UserNeighborhood getNeighborhood(UserSimilarity similarityObject, DataModel dataModel) {
		try {
			return new NearestNUserNeighborhood(neighborhoodSize, similarityObject, dataModel);
		} catch (TasteException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public String toString(boolean min) {
		if(!min) return this.toString();
		// get uppercase letters in similarity
		String similarityIntitials = this.similarity.toString().replaceAll("[a-z]", "");
		return String.format("%dNN/%s", neighborhoodSize, similarityIntitials);
	}
	
	public String toString() {
		return String.format("%d Nearest Neighbor (%s)", neighborhoodSize, this.similarity.toString());
	}
}