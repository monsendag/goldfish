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

public class Evaluator extends ArrayList<ModelBased> {
	
	private static final long serialVersionUID = -167230272254792689L;
	DataModel dataModel;
	double trainingPercentage = 0.70;
	double testPercentage = 0.10;
	int topN = 10;
	double relevanceThreshold = GenericRecommenderIRStatsEvaluator.CHOOSE_THRESHOLD;
	
	public Evaluator(DataModel dataModel) {
		this.dataModel = dataModel;
	}
	
	public ArrayList<EvaluationResult> evaluateAll() throws IOException, TasteException {
		
		RecommenderEvaluator RMSE = new RMSRecommenderEvaluator();
		RecommenderEvaluator AAD = new AverageAbsoluteDifferenceRecommenderEvaluator();
		RecommenderIRStatsEvaluator irStats = new GenericRecommenderIRStatsEvaluator();
		
		ArrayList<EvaluationResult> results = new ArrayList<EvaluationResult>();
		
		double rmse;
		double aad;
		IRStatistics stats;
		for(ModelBased recommender : this) {
			rmse = RMSE.evaluate(recommender.getBuilder(), null, dataModel, trainingPercentage, testPercentage);
			aad = AAD.evaluate(recommender.getBuilder(), null, dataModel, trainingPercentage, testPercentage);
			stats = irStats.evaluate(recommender.getBuilder(), null, dataModel, null, topN, relevanceThreshold, testPercentage);
			results.add(new EvaluationResult(recommender.toString(), rmse, aad, stats));
		}
		return results;
	}
}
