package edu.ntnu.idi.goldfish;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

public class KNN extends MemoryBased {
	
	int K = 1;

	public KNN(Similarity similarity, int neighborhoodSize) {
		this.similarity = similarity;
		this.K = neighborhoodSize;
		
	}
	
	public UserNeighborhood getNeighborhood(UserSimilarity similarityObject, DataModel dataModel) {
		try {
			return new NearestNUserNeighborhood(K, similarityObject, dataModel);
		} catch (TasteException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public String toString(boolean min) {
		if(!min) return this.toString();
		// get uppercase letters in similarity
		String similarityIntitials = this.similarity.toString().replaceAll("[a-z]", "");
		return String.format("%dNN/%s", K, similarityIntitials);
	}
	
	public String toString() {
		return String.format("%d Nearest Neighbor (%s)", K, this.similarity.toString());
	}

	public double getKTL() {
		// TODO Auto-generated method stub
		return K;
	}
}