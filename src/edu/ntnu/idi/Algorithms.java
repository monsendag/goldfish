package edu.ntnu.idi;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.example.grouplens.GroupLensDataModel;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.CachingRecommender;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.EuclideanDistanceSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.LogLikelihoodSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.SpearmanCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.TanimotoCoefficientSimilarity;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

public class Algorithms {
	
	public static String newLine = System.getProperty("line.separator");
	
	public static GenericUserBasedRecommender userBasedRecommender(String ratings, int neighborhoodSize, String similarityMetric) 
			throws TasteException, IOException {
		FileDataModel movielens = new GroupLensDataModel(new File(ratings));
		
		UserSimilarity userSimilarity = null;
		
		if(similarityMetric.equals("pearson")){
			userSimilarity = new PearsonCorrelationSimilarity(movielens);			
		} else if (similarityMetric.equals("euclidean")) {
			userSimilarity = new EuclideanDistanceSimilarity(movielens);
		} else if (similarityMetric.equals("cosine")) {
			userSimilarity = new PearsonCorrelationSimilarity(movielens);
		} else if (similarityMetric.equals("spearman")) {
			userSimilarity = new SpearmanCorrelationSimilarity(movielens);
		} else if (similarityMetric.equals("tanimoto")) {
			userSimilarity = new TanimotoCoefficientSimilarity(movielens);
		} else if (similarityMetric.equals("loglikelihood")) {
			userSimilarity = new LogLikelihoodSimilarity(movielens);
		} else { 
			// Default: Pearson correlation similarity
			userSimilarity = new PearsonCorrelationSimilarity(movielens);
		}
		
		UserNeighborhood neighborhood = new NearestNUserNeighborhood(neighborhoodSize, userSimilarity, movielens);
		
		System.out.format("\n A user-based recommendation algorithm was created. \n Similarity metric is %s. \n Size of neighborhood is %d \n", similarityMetric, neighborhoodSize);
		
		return new GenericUserBasedRecommender(movielens, neighborhood, userSimilarity);
	}

	public static List<RecommendedItem> getRecommendations(int numberOfRatings, int userId, Recommender recommender) throws TasteException{
		Recommender cachingRecommender = new CachingRecommender(recommender);
		
		List<RecommendedItem> recommendations = cachingRecommender.recommend(userId, numberOfRatings);
	
		return recommendations;
	}
}
