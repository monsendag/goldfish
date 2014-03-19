package edu.ntnu.idi.goldfish;

import edu.ntnu.idi.goldfish.mahout.SMDataModel;
import edu.ntnu.idi.goldfish.preprocessors.Preprocessor;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.GenericBooleanPrefDataModel;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.model.DataModel;

import java.io.File;
import java.io.IOException;

public enum DataSet {

    yow10kratings, yow10kprocessed, Netflix100M, Movielens1M, Movielens50k, Movielens1Mbinary, Movielens50kbinary, MovielensSynthesized1M, MovielensSynthesized200k, MovielensSynthesized50k, VTT36k, food, claypool2k, claypool2kprocessed;

    public DataModel getModel() throws IOException, TasteException {
        DataModel model;
        switch (this) {
            // yow userstudy
            case yow10kratings:
                return new FileDataModel(new File("datasets/yow-userstudy/ratings-fixed.csv"));
            case yow10kprocessed:
                return Preprocessor.getPreprocessedDataModel("datasets/yow-userstudy/like-timeonpage-timeonmouse.csv");
            
            // claypool userstudy
            case claypool2k:
            	return new SMDataModel(new File("datasets/claypool/cbdata-explicit.csv"));
            case claypool2kprocessed:
            	return Preprocessor.getPreprocessedDataModel("datasets/claypool/cbdata-feedback-anon.csv");

            // regular models
            case Netflix100M:
                return new FileDataModel(new File("datasets/netflix-100m/ratings.tsv.gz"));
            case Movielens1M:
                return new FileDataModel(new File("datasets/movielens-1m/ratings.tsv.gz"));
            case Movielens50k:
                return new FileDataModel(new File("datasets/movielens-1m/ratings-50k.tsv.gz"));

            // synthesized models (initialized with SMDataModel
            case MovielensSynthesized1M:
                return new SMDataModel(new File("datasets/movielens-synthesized/ratings-synthesized.tsv"));
            case MovielensSynthesized200k:
                return new SMDataModel(new File("datasets/movielens-synthesized/ratings-200k.tsv"));
            case MovielensSynthesized50k:
                return new SMDataModel(new File("datasets/movielens-synthesized/ratings-50k.tsv"));
            case food:
                return new SMDataModel(new File("datasets/FOOD_Dataset/food-ettellerannet.csv"));

            // binary models
            case VTT36k:
                model = new FileDataModel(new File("datasets/vtt-36k/VTT_I_data.csv"));
                return new GenericBooleanPrefDataModel(GenericBooleanPrefDataModel.toDataMap(model));
            case Movielens1Mbinary:
                model = new FileDataModel(new File("datasets/movielens-1m/ratings-binary.csv"));
                return new GenericBooleanPrefDataModel(GenericBooleanPrefDataModel.toDataMap(model));
            case Movielens50kbinary:
                model = new FileDataModel(new File("datasets/movielens-1m/ratings-binary-50k.csv"));
                return new GenericBooleanPrefDataModel(GenericBooleanPrefDataModel.toDataMap(model));
        }
        return null;

    }
}
