package edu.ntnu.idi.goldfish;

import edu.ntnu.idi.goldfish.mahout.SMDataModel;
import org.apache.mahout.cf.taste.common.TasteException;

import java.io.File;
import java.io.IOException;

public class Test {

	public static void main(String[] args) throws IOException, TasteException {
		
		SMDataModel model = new SMDataModel(new File("datasets/movielens-synthesized/ratings.csv"));
		model.writeDatasetToFile("/tmp/ratings-synthesized.csv");
		System.out.println(String.format("users: %d, items: %d", model.getNumUsers(), model.getNumItems()));
		
	}
	
}
