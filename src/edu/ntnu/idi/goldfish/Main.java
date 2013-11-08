package edu.ntnu.idi.goldfish;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.example.grouplens.GroupLensDataModel;
import org.apache.mahout.cf.taste.impl.similarity.EuclideanDistanceSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.LogLikelihoodSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.SpearmanCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.TanimotoCoefficientSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.similarity.precompute.SimilarItem;

import edu.ntnu.idi.goldfish.EvaluationResults.SortOption;
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
	public static EvaluationResults evaluateMemoryBased(DataModel dataModel) throws IOException, TasteException {
		Evaluator evaluator = new Evaluator();
		
		
		
		for(Similarity similarity : Similarity.values()) {
			
			double lowT = 0.10;
			double highT = 0.70;
			double incrT = 0.05;
			
			int lowN = 3;
			int highN = 9;
			int incrN = 2;
			
			
			switch(similarity) {
				case PearsonCorrelation: 
					
					
				case EuclideanDistance: 
					
				case SpearmanCorrelation:
					
				case TanimotoCoefficient: 
					
				case LogLikelihood: 
					
			
			}
			
			if(similarity == Similarity.TanimotoCoefficient) {
				continue;
			}
			
			// KNN: try different neighborhood sizes (odd numbers are preferable)
            for(int K = lowN; K <= highN; K += incrN) {
                evaluator.add(new KNN(similarity, K));                        
            }
			
			// THRESHOLD: try different thresholds
			for(double T = lowT; T <= highT; T += incrT) {
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
		
//		DataModel dataModel = new GroupLensDataModel(new File("datasets/movielens-1m/ratings.dat.gz"));
		DataModel dataModel = new GroupLensDataModel(new File("datasets/sample100/ratings.dat.gz"));
		//DataModel dataModel = new FileDataModel(new File("datasets/vtt-clustered/cluster0.csv"));

		EvaluationResults results = evaluateMemoryBased(dataModel);
		
		results.print(SortOption.RMSE);
		
		results.save();
		
		
//		
		
		
	
		/**
		 * MODEL-based evaluation
		 */
		
		// clustering models (KMeans ... EM?)
//		
//		DataModel[] dataModels = UserClusterer.clusterUsers(dataModel, 5, new EuclideanDistanceMeasure());
//		
//		for(DataModel model : dataModels) {
//			
//		}
//		
//		for(EvaluationResult re : results) {
//			System.out.println(re);
//		}
//		
//		
		/*List<EvaluationResult> res;
		for(DataModel clusteredModel : dataModels) {
			res = evaluateMemoryBased(clusteredModel);
		}
		*/
		// latent semantic models (Matrix Factorizations, etc..)
		
		
		
	}
}
