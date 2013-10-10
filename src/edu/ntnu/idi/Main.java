package edu.ntnu.idi;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.eval.RecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.eval.AverageAbsoluteDifferenceRecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.CachingRecommender;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.apache.mahout.common.RandomUtils;
import org.apache.taglibs.standard.lang.jstl.Evaluator;

public class Main {
	
	public void getRatings(int numberOfRatings, int userId, Recommender recommender) throws TasteException{
		Recommender cachingRecommender = new CachingRecommender(recommender);
		
		List<RecommendedItem> recommendations = cachingRecommender.recommend(userId, numberOfRatings);
		
		for (RecommendedItem recommendedItem : recommendations) {
			System.out.print(recommendedItem.getItemID());
			System.out.print(" : ");
			System.out.println(recommendedItem.getValue());
		}
	}
	
	public GenericUserBasedRecommender createRecommender() throws TasteException, IOException{
		FileDataModel movielens = new FileDataModel(new File("data/movielens-1m/ratings.txt"));
		System.out.println("koko");
		
		UserSimilarity userSimilarity = new PearsonCorrelationSimilarity(movielens);
		
		UserNeighborhood neighborhood = new NearestNUserNeighborhood(100, userSimilarity, movielens);
		
		return new GenericUserBasedRecommender(movielens, neighborhood, userSimilarity);
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws TasteException 
	 */
	public static void main(String[] args) throws IOException, TasteException {
		RandomUtils.useTestSeed();
		DataModel model = new FileDataModel(new File("data/movielens-1m/ratings.txt"));
		
		RecommenderEvaluator evaluator = new AverageAbsoluteDifferenceRecommenderEvaluator();
		
		RecommenderBuilder recommenderBuilder = new RecommenderBuilder() {
			
			public Recommender buildRecommender(DataModel model) throws TasteException {
				UserSimilarity similarity = new PearsonCorrelationSimilarity(model);
				UserNeighborhood neighborhood = new NearestNUserNeighborhood(100, similarity, model);
				return new GenericUserBasedRecommender(model, neighborhood, similarity);
				}
			};
		
		double score = evaluator.evaluate(recommenderBuilder, null, model, 0.8, 0.2);
		System.out.println(score);
		
		}
}
