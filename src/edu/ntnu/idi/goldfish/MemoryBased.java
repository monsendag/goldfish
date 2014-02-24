package edu.ntnu.idi.goldfish;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.model.GenericBooleanPrefDataModel;
import org.apache.mahout.cf.taste.impl.recommender.GenericBooleanPrefUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.EuclideanDistanceSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.LogLikelihoodSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.SpearmanCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.TanimotoCoefficientSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

public abstract class MemoryBased extends Evaluation {

	public Similarity similarity;
	
	public MemoryBased(int topN, double KTL, Similarity similarity) {
		super(topN, KTL);
		this.similarity = similarity;
	}

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

	public RecommenderBuilder getRecommenderBuilder() {
		return new RecommenderBuilder() {
			public Recommender buildRecommender(DataModel dataModel) throws TasteException {
				UserSimilarity similarityObject = getSimilarityObject(similarity, dataModel);
				UserNeighborhood neighborhood = getNeighborhood(similarityObject, dataModel);
				
				if(dataModel instanceof GenericBooleanPrefDataModel) {
					return new GenericBooleanPrefUserBasedRecommender(dataModel, neighborhood, similarityObject);	
				}
				return new GenericUserBasedRecommender(dataModel, neighborhood, similarityObject);
				
			}
		};
	}
}
