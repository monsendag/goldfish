package edu.ntnu.idi.goldfish.preprocessors;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.impl.recommender.svd.ALSWRFactorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDRecommender;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.vectorizer.encoders.ContinuousValueEncoder;

public class PreprocessorClassifier extends Preprocessor {

    @Override
    public DataModel preprocess(YowModel model) throws TasteException {

        ContinuousValueEncoder encoder = new ContinuousValueEncoder("meh");


        return model;
    }
}
