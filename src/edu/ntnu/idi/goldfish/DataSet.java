package edu.ntnu.idi.goldfish;

import edu.ntnu.idi.goldfish.mahout.DBModel;
import edu.ntnu.idi.goldfish.mahout.SMDataModel;
import org.apache.mahout.cf.taste.impl.model.GenericBooleanPrefDataModel;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.model.DataModel;

import java.io.File;

public enum DataSet {

    yowSMImplicit, yowImplicit, yowBaseline, claypool2k, Netflix100M, Movielens1M, Movielens50k, Movielens1Mbinary, Movielens50kbinary, MovielensSynthesized1M, MovielensSynthesized200k, MovielensSynthesized50k, VTT36k, food;

    public DataModel getModel() throws Exception {
        DataModel model;
        switch (this) {

            // yow userstudy
            case yowBaseline:
                return new FileDataModel(new File("datasets/yow-userstudy/python/yow-smart-sample-explicit-2.csv"));
            // yow userstudy
            case yowImplicit:
                return new DBModel(new File("datasets/yow-userstudy/python/yow-smart-sample-implicit-2.csv"));
            case yowSMImplicit:
                return new SMDataModel(new File("datasets/yow-userstudy/python/yow-smart-sample-implicit-2.csv"));
            // claypool userstudy
            case claypool2k:
            	return new SMDataModel(new File("datasets/claypool/claypool-sample-exdupes-exinvalid-rating.csv"));

            // netflix
            case Netflix100M:
                return new FileDataModel(new File("datasets/netflix-100m/ratings.tsv.gz"));

            // movielens
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
