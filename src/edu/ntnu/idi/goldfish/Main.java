package edu.ntnu.idi.goldfish;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.lucene.benchmark.quality.QualityStats.RecallPoint;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.example.grouplens.GroupLensDataModel;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.common.distance.TanimotoDistanceMeasure;

import edu.ntnu.idi.goldfish.EvaluationResult.SortOption;
import edu.ntnu.idi.goldfish.MemoryBased.Similarity;


public class Main {
	
	// disable Mahout logging output
	static { System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog"); }
	
		
	/*
	 * MEMORY-based evaluation 
	 * 
	 * Evaluate KNN and Threshold based neighborhood models.
	 * Try all different similarity metrics found in ModelBased.Similarity.
	 */
	public static List<EvaluationResult> evaluateMemoryBased(DataModel dataModel) throws IOException, TasteException {
		Evaluator evaluator = new Evaluator();
		
		
			for(Similarity similarity : Similarity.values()) {
			
			// KNN: try different neighborhood sizes (odd numbers are preferable)
			for(int K = 15; K >= 1; K -= 2) {
				evaluator.add(new KNN(similarity, K));			
			}
			
			// THRESHOLD: try different thresholds
			for(double T = 0.70; T <= 1.00; T += 0.05) {
				evaluator.add(new Threshold(similarity, T));
			}
		}

		 return evaluator.evaluateAll(dataModel, 0.90, 0.10, 10);
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws TasteException 
	 * @throws ClassNotFoundException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, TasteException, InterruptedException, ClassNotFoundException {
		
		//DataModel model = new GroupLensDataModel(new File("data/movielens-1m/ratings.dat.gz"));
		DataModel dataModel = new GroupLensDataModel(new File("datasets/sample100/ratings.dat.gz"));
		//DataModel dataModel = new FileDataModel(new File("datasets/vtt-clustered/cluster0.csv"));
		System.out.println(dataModel.getNumUsers());
		//List<EvaluationResult> results = evaluateMemoryBased(dataModel);
		
		/**
		 * MODEL-based evaluation
		 */
		
		// clustering models (KMeans ... EM?)
		
		DataModel[] dataModels = UserClusterer.clusterUsers(dataModel, 5, new TanimotoDistanceMeasure());
		
		for(int i=0;i<dataModels.length; i++) {
			System.out.format("Cluster %d count %d", i, dataModels[i].getNumUsers());
		}
		
		/*List<EvaluationResult> res;
		for(DataModel clusteredModel : dataModels) {
			res = evaluateMemoryBased(clusteredModel);
		}
		*/
		// latent semantic models (Matrix Factorizations, etc..)
		
		
		//EvaluationResult.sortList(results, SortOption.RMSE);
		/*
		for(EvaluationResult re : results) {
			System.out.println(re);
		}
		*/
	}
}
