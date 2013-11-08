package edu.ntnu.idi.goldfish;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.IRStatistics;
import org.apache.mahout.cf.taste.eval.RecommenderEvaluator;
import org.apache.mahout.cf.taste.eval.RecommenderIRStatsEvaluator;
import org.apache.mahout.cf.taste.impl.eval.AverageAbsoluteDifferenceRecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.eval.GenericRecommenderIRStatsEvaluator;
import org.apache.mahout.cf.taste.impl.eval.RMSRecommenderEvaluator;
import org.apache.mahout.cf.taste.model.DataModel;

public class Evaluator extends ArrayList<Recommender> {
	
	private static final long serialVersionUID = -167230272254792689L;

	
	double relevanceThreshold = GenericRecommenderIRStatsEvaluator.CHOOSE_THRESHOLD;
	
	public Evaluator() {
	}
	
	public EvaluationResults evaluateAll(DataModel dataModel, double training, double test, int topN) throws IOException, TasteException {
		
		RecommenderEvaluator RMSE = new RMSRecommenderEvaluator();
		RecommenderEvaluator AAD = new AverageAbsoluteDifferenceRecommenderEvaluator();
		RecommenderIRStatsEvaluator irStats = new GenericRecommenderIRStatsEvaluator();
		
		EvaluationResults results = new EvaluationResults();	
		
		double rmse;
		double aad;
		IRStatistics stats;
		for(Recommender recommender : this) {
			rmse = RMSE.evaluate(recommender.getBuilder(), null, dataModel, training, test);

//			System.out.print(recommender); 
//			System.out.println(rmse);
			aad = AAD.evaluate(recommender.getBuilder(), null, dataModel, training, test);
			stats = irStats.evaluate(recommender.getBuilder(), null, dataModel, null, topN, relevanceThreshold, test);
			results.add(new Result(recommender.toString(), rmse, aad, stats));
		}
		return results;
	}
}
