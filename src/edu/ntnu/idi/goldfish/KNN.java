package edu.ntnu.idi.goldfish;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

public class KNN extends MemoryBased {

	public KNN(int topN, Similarity similarity, int neighborhoodSize) {
		this.topN = topN;
		this.similarity = similarity;
		this.KTL = neighborhoodSize;
		
	}
	
	public UserNeighborhood getNeighborhood(UserSimilarity similarityObject, DataModel dataModel) {
		try {
			return new NearestNUserNeighborhood((int) KTL, similarityObject, dataModel);
		} catch (TasteException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String toString() {
		return "KNN";
	}
}