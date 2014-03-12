package edu.ntnu.idi.goldfish;

import edu.ntnu.idi.goldfish.mahout.SMRMSEevaluator;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.IRStatistics;
import org.apache.mahout.cf.taste.eval.RecommenderIRStatsEvaluator;
import org.apache.mahout.cf.taste.impl.eval.AbstractDifferenceRecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.eval.AverageAbsoluteDifferenceRecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.eval.GenericRecommenderIRStatsEvaluator;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.common.RandomUtils;
import org.apache.mahout.common.RandomWrapper;
import org.apache.mahout.common.distance.DistanceMeasure;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class Evaluator {

	static long getRandomUser(DataModel dataModel) throws TasteException {
		RandomWrapper random = RandomUtils.getRandom();
		Iterator<Long> it = dataModel.getUserIDs();
		while (it.hasNext()) {
			if (random.nextDouble() >= 0.1) {
				continue;
			}
			return it.next();
		}
		return 0;
	}
	
	double relevanceThreshold = GenericRecommenderIRStatsEvaluator.CHOOSE_THRESHOLD;
	
	public Evaluator() {	
	}
	
	public void evaluateClustered(int numClusters, DistanceMeasure measure, List<Evaluation> evaluations, EvaluationResults results, DataModel dataModel, double test) throws TasteException, IOException, InterruptedException, ClassNotFoundException {

		DataModel[] dataModels = KMeansWrapper.clusterUsers(dataModel, numClusters, measure, 0.5, 10, true, 0.0, true);
		
		StopWatch.start("totaleval");
		EvaluationResults clusterResults;
		for(Evaluation evaluation : evaluations) {
			long user = getRandomUser(dataModel);
			clusterResults = new EvaluationResults(dataModel);
			StopWatch.start("cluster-evaluation");
			for(int i=0; i<dataModels.length; i++) {
				clusterResults.add(evaluate(evaluation, dataModel, test, user));
			}
			Result average = Result.getAverage(clusterResults);
			System.err.format(average+" (in %s) \n", StopWatch.str("cluster-evaluation"));
			results.add(average);
		}
		System.out.format("Evaluated %d clusters with %d configurations in %s\n", numClusters, evaluations.size(), StopWatch.str("totaleval"));
	}
	
	public void evaluateUnclustered(List<Evaluation> evaluations, EvaluationResults results, DataModel dataModel, double test) throws IOException, TasteException {
		System.out.format("Starting evaluation of %d configurations (%d users, %d items) \n", evaluations.size(), dataModel.getNumUsers(), dataModel.getNumItems());
		StopWatch.start("totaleval");
		for(Evaluation evaluation : evaluations) {
			results.add(evaluate(evaluation, dataModel, test, getRandomUser(dataModel)));
		}
		System.out.println("==================================================================================================================================================================================");
		System.out.println(Result.getTotal(results));
		System.out.println(Result.getAverage(results));
		System.out.format("Evaluated %d configurations (%d users, %d items) in %s \n", evaluations.size(), dataModel.getNumUsers(), dataModel.getNumItems(), StopWatch.str("totaleval"));
	}
	
	Result evaluate(Evaluation evaluation, DataModel dataModel, double testFrac, long userID) throws TasteException {
		StopWatch.start("evalTime");
		
		// initialize variables
		double rmse = 0;
		double aad = 0;
		double precision = 0;
		double recall = 0;

		// initialize evaluators
		AbstractDifferenceRecommenderEvaluator RMSE = new SMRMSEevaluator();
		AverageAbsoluteDifferenceRecommenderEvaluator AAD = new AverageAbsoluteDifferenceRecommenderEvaluator();
        RecommenderIRStatsEvaluator irEvaluator = new GenericRecommenderIRStatsEvaluator();
		
        
        // do evaluations
        // NOTE: when a result is not needed, the respective line may be commented out here for increased evaluation speed
		rmse = RMSE.evaluate(evaluation.getRecommenderBuilder(), null, dataModel, 0.9, testFrac);
		aad = AAD.evaluate(evaluation.getRecommenderBuilder(), null, dataModel, 0.9, testFrac);
        IRStatistics stats = irEvaluator.evaluate(evaluation.getRecommenderBuilder(), evaluation.getModelBuilder(), dataModel, null, evaluation.getTopN(), relevanceThreshold, testFrac);
        precision = stats.getPrecision();
        recall = stats.getRecall();
        
        // calculate build time
		StopWatch.start("buildTime");
		evaluation.getRecommenderBuilder().buildRecommender(dataModel);
		long buildTime = StopWatch.get("buildTime");
		
		// calculate recommendation time
		long recTime = 0; //getRecommendationTiming(recommender, 20, 10, userID);

		// get time of total evaluation
        long evalTime = StopWatch.get("evalTime");
		Result result = new Result(evaluation, rmse, aad, precision, recall, buildTime, recTime, evalTime);

		// print each result to show progress
		System.out.format("%s \n", result);
		return result;
	}
	
	public long getRecommendationTiming(org.apache.mahout.cf.taste.recommender.Recommender rec, int N, int howMany, long userID) throws TasteException {
		long totalDuration = 0;
		for(int i=0; i< N; i++) {
			StopWatch.start("recommend");
			rec.recommend(userID, howMany);
			totalDuration += StopWatch.get("recommend");
		}
		return totalDuration / N;
	}
	
}
