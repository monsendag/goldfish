package edu.ntnu.idi.goldfish;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.IRStatistics;
import org.apache.mahout.cf.taste.eval.RecommenderEvaluator;
import org.apache.mahout.cf.taste.eval.RecommenderIRStatsEvaluator;
import org.apache.mahout.cf.taste.impl.eval.AverageAbsoluteDifferenceRecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.eval.GenericRecommenderIRStatsEvaluator;
import org.apache.mahout.cf.taste.impl.eval.RMSRecommenderEvaluator;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.Recommender;

import edu.ntnu.idi.goldfish.MemoryBased.Similarity;

public class Evaluator extends ArrayList<RecommenderWrapper> {
	
	private static final long serialVersionUID = -167230272254792689L;

	
	double relevanceThreshold = GenericRecommenderIRStatsEvaluator.CHOOSE_THRESHOLD;
	
	public Evaluator() {
	}
	
	public EvaluationResults evaluateAll(DataModel dataModel, double training, double test, int topN) throws IOException, TasteException {
		
		// pick a random user
		
		Iterator<Long> it = dataModel.getUserIDs();
		it.hasNext();
		long userOne = it.next();
		long userTwo = it.next();
		long userThree = it.next();
		
		RecommenderEvaluator RMSE = new RMSRecommenderEvaluator();
		RecommenderEvaluator AAD = new AverageAbsoluteDifferenceRecommenderEvaluator();
		RecommenderIRStatsEvaluator irStats = new GenericRecommenderIRStatsEvaluator();
		
		EvaluationResults results = new EvaluationResults();	
		
		double rmse;
		double aad;
		IRStatistics stats;
		Recommender rec;
		
		for(RecommenderWrapper recommender : this) {
			rmse = RMSE.evaluate(recommender.getBuilder(), null, dataModel, training, test);
			aad = AAD.evaluate(recommender.getBuilder(), null, dataModel, training, test);
			stats = irStats.evaluate(recommender.getBuilder(), null, dataModel, null, topN, relevanceThreshold, test);

			long startTime = System.currentTimeMillis();
			rec = recommender.getBuilder().buildRecommender(dataModel);
			long buildTime = System.currentTimeMillis() - startTime;
			
			long recTime = getRecommendationTiming(rec, 20, 10, userThree);
			
			
			results.add(new Result(recommender, topN,  rmse, aad, stats, buildTime, recTime));
		}
		
		
		
		return results;
	}
	
	
	public long getRecommendationTiming(org.apache.mahout.cf.taste.recommender.Recommender rec, int N, int howMany, long userID) throws TasteException {
		long totalDuration = 0;
		for(int i=0; i< N; i++) {
			long startTime = System.currentTimeMillis();
			rec.recommend(userID, howMany);
			long endTime = System.currentTimeMillis();
			totalDuration += (endTime - startTime);
		}
		return totalDuration / N;
		
	}
	
	/*
	 * MEMORY-based evaluation 
	 * 
	 * Evaluate KNN and Threshold based neighborhood models.
	 * Try all different similarity metrics found in ModelBased.Similarity.
	 */
	public static EvaluationResults evaluateMemoryBased(DataModel dataModel) throws IOException, TasteException {
		Evaluator evaluator = new Evaluator();
		
		
		
		for(Similarity similarity : Similarity.values()) {
			
			double lowT = 0.05;
			double highT = 0.30;
			double incrT = 0.05;
			
			int lowN = 3;
			int highN = 9;
			int incrN = 2;
			
			
			switch(similarity) {
				case PearsonCorrelation: 
					continue;
				case EuclideanDistance: 
					continue;
				case SpearmanCorrelation:
					continue;
				case TanimotoCoefficient: 
					
				case LogLikelihood: 
					
			
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

		 return evaluator.evaluateAll(dataModel, 0.90, 0.10, 100);
	}
	
	
}
