package edu.ntnu.idi.goldfish;

import edu.ntnu.idi.goldfish.configurations.Config;
import edu.ntnu.idi.goldfish.mahout.SMRMSEevaluator;
import edu.ntnu.idi.goldfish.preprocessors.KMeansWrapper;
import edu.ntnu.idi.goldfish.preprocessors.Preprocessor;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.IRStatistics;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.eval.RecommenderIRStatsEvaluator;
import org.apache.mahout.cf.taste.impl.eval.AbstractDifferenceRecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.eval.AverageAbsoluteDifferenceRecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.eval.GenericRecommenderIRStatsEvaluator;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.common.RandomUtils;
import org.apache.mahout.common.RandomWrapper;
import org.apache.mahout.common.distance.DistanceMeasure;

import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

public class Evaluator {

	public static void evaluate(List<Config> configs, ResultList results) {
        configs.parallelStream().forEach(config -> {
            try {
                if(config.containsKey("preprocessor")) {
                    // do preprocessing
                    Class<Preprocessor> pre = config.get("preprocessor");
                    Preprocessor p = pre.newInstance();
                    DataModel model = p.preprocess(config);
                    config.set("model", model);
                }
                System.out.println((String) config.get("name"));

                boolean doAverage = config.containsKey("average");
                results.add(doAverage ? Evaluator.evaluateAverage(config) : Evaluator.evaluateOne(config));
            }

            catch(Exception e) {
                e.printStackTrace();
            }
        });
	}

    /**
     * Calculate the average of a set of iterations of each config
     */
    public static Result evaluateAverage(Config config) {
          ResultList each = new ResultList();
          int numIterations = config.get("average");
          IntStream.range(0, numIterations).parallel().forEach(i -> {
              each.add(Evaluator.evaluateOne(config));
          });
          return each.getAverage().set("config", config).remove("name");
    }

    public static void evaluateClustered(DataModel dataModel, int numClusters, DistanceMeasure measure, List<Config> configurations, ResultList results) throws Exception {
        DataModel[] dataModels = KMeansWrapper.clusterUsers(dataModel, numClusters, measure, 0.5, 10, true, 0.0, true);

        StopWatch.start("totaleval");
        ResultList clusterResults;
        for(Config config : configurations) {
            clusterResults = new ResultList();
            StopWatch.start("cluster-configuration");
            for(int i=0; i<dataModels.length; i++) {
                clusterResults.add(evaluateOne(config));
            }
            Result average = clusterResults.getAverage();
            System.err.format(average+" (in %s) \n", StopWatch.str("cluster-configuration"));
            results.add(average);
        }
        System.out.format("Evaluated %d clusters with %d configurations in %s\n", numClusters, configurations.size(), StopWatch.str("totaleval"));
    }


	public static Result evaluateOne(Config config) {
        // we can't throw here because the method is called in a stream lambda
        try {
            StopWatch.start("evalTime");

            // load common configuration settings
            DataModel model = config.get("model");
            double train = config.get("trainingPercentage");
            double eval = config.get("evaluationPercentage");
            RecommenderBuilder recBuilder = config.get("recommenderBuilder");

            Result result = new Result().set("config", config);

            if ((boolean) config.get("getRMSE")) {
                // initialize evaluators
                AbstractDifferenceRecommenderEvaluator RMSE = new SMRMSEevaluator();
                double rmse = RMSE.evaluate(recBuilder, null, model, train, eval);
                result.set("RMSE", rmse);
            }

            if ((boolean) config.get("getAAD")) {
                AverageAbsoluteDifferenceRecommenderEvaluator AAD = new AverageAbsoluteDifferenceRecommenderEvaluator();
                double aad = AAD.evaluate(recBuilder, null, model, train, eval);
                result.set("AAD", aad);
            }

            if ((boolean) config.get("getIrStats")) {
                RecommenderIRStatsEvaluator irEvaluator = new GenericRecommenderIRStatsEvaluator();
                IRStatistics stats = irEvaluator.evaluate(recBuilder, null, model, null, config.get("topN"), config.get("relevanceThreshold"), eval);
                double precision = stats.getPrecision();
                double recall = stats.getRecall();

                result.set("precision", precision)
                      .set("recall", recall);
            }

            if ((boolean) config.get("getBuildTime")) {
                // calculate build time
                StopWatch.start("buildTime");
                recBuilder.buildRecommender(model);
                long buildTime = StopWatch.get("buildTime");
                result.set("buildTime", (double)buildTime);
            }

            if ((boolean) config.get("getRecTime")) {
                // calculate average recommendation time for x iterations
                int iterations = config.get("getRecTimeIterations");
                long recTime = getRecommendationTiming(recBuilder.buildRecommender(model), model, iterations, 10);
                result.set("recTime", (double)recTime);
            }

            // get time of total configuration
            result.set("evalTime", (double)StopWatch.get("evalTime"));

            if ((boolean) config.get("showProgress")) {
                // print each result to show progress
                System.out.println(result);
            }
            return result;
        }
        catch(Exception e) {
            e.printStackTrace();
            return null;
        }
	}

	private static long getRecommendationTiming(Recommender recommender, DataModel model, int iterations, int topN) throws TasteException {
		long totalDuration = 0;
		for(int i=0; i< iterations; i++) {
			StopWatch.start("recommend");
            // get a random user
            long userID = getRandomUser(model);
			recommender.recommend(userID, topN);
			totalDuration += StopWatch.get("recommend");
		}
		return totalDuration / iterations;
	}

    private static long getRandomUser(DataModel dataModel) throws TasteException {
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
}