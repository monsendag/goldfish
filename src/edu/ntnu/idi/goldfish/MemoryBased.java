package edu.ntnu.idi.goldfish;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.EuclideanDistanceSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.LogLikelihoodSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.SpearmanCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.TanimotoCoefficientSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

public abstract class MemoryBased implements Recommender {

	Similarity similarity;

	public static enum Similarity {
		PearsonCorrelation, EuclideanDistance, SpearmanCorrelation, TanimotoCoefficient, LogLikelihood
	}

	public abstract UserNeighborhood getNeighborhood(UserSimilarity similarityObject, DataModel dataModel);

	UserSimilarity getSimilarityObject(Similarity similarity, DataModel dataModel) {
		try {
			switch (similarity) {
			case PearsonCorrelation: return new PearsonCorrelationSimilarity(dataModel);
			case EuclideanDistance: return new EuclideanDistanceSimilarity(dataModel);
			case SpearmanCorrelation: return new SpearmanCorrelationSimilarity(dataModel);
			case TanimotoCoefficient: return new TanimotoCoefficientSimilarity(dataModel);
			case LogLikelihood: return new LogLikelihoodSimilarity(dataModel);
			}
		} catch (TasteException e) {
			e.printStackTrace();
		}
		return null;
	}

	public RecommenderBuilder getBuilder() {
		return new RecommenderBuilder() {
			public org.apache.mahout.cf.taste.recommender.Recommender buildRecommender(DataModel dataModel) throws TasteException {
				UserSimilarity similarityObject = getSimilarityObject(similarity, dataModel);
				UserNeighborhood neighborhood = getNeighborhood(similarityObject, dataModel);
				return new GenericUserBasedRecommender(dataModel, neighborhood, similarityObject);
			}
		};
	}
}
