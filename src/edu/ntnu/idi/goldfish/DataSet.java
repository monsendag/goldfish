package edu.ntnu.idi.goldfish;

import edu.ntnu.idi.goldfish.mahout.SMDataModel;
import edu.ntnu.idi.goldfish.preprocessors.*;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorClustering.Clusterer;

import org.apache.mahout.cf.taste.impl.model.GenericBooleanPrefDataModel;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.model.DataModel;

import java.io.File;

public enum DataSet {

    yowExdupesExinvalidLike, yow10kprocessedpuddis, yow10kprocessedmlr, yow10kprocessedmf, yow10kprocessedclassifier, yow10kprocessedclustering, Netflix100M, Movielens1M, Movielens50k, Movielens1Mbinary, Movielens50kbinary, MovielensSynthesized1M, MovielensSynthesized200k, MovielensSynthesized50k, VTT36k, food, claypool2k, claypool2kprocessed;

    public DataModel getModel() throws Exception {
        DataModel model;
        Preprocessor pre;
        switch (this) {
            // yow userstudy
            case yowExdupesExinvalidLike:
                return new FileDataModel(new File("datasets/yow-userstudy/yow-sample-exdupes-exinvalid-like.csv"));
            case yow10kprocessedpuddis:

                model = PreprocessorPuddis.getPreprocessedDataModel("datasets/yow-userstudy/exdupes-like-timeonpage-timeonmouse.csv");
                Preprocessor.writeDatasetToFileExplicit((SMDataModel) model, "/tmp/removing-invalid-ratings.csv");
                model = new FileDataModel(new File("/tmp/removing-invalid-ratings.csv"));
                return model;
            case yow10kprocessedmlr:

                model = PreprocessorMLR.getPreprocessedDataModel("datasets/yow-userstudy/exdupes-like-timeonpage-timeonmouse.csv");
                Preprocessor.writeDatasetToFileExplicit((SMDataModel) model, "/tmp/removing-invalid-ratings.csv");
                model = new FileDataModel(new File("/tmp/removing-invalid-ratings.csv"));
                return model;
            case yow10kprocessedmf:
                
            	model = new YowModel(new File("datasets/yow-userstudy/exdupes-like-timeonpage-timeonmouse.csv"));
                Preprocessor mf = new PreprocessorMF();
                return mf.preprocess((YowModel) model);
            case yow10kprocessedclustering:
            	
            	model = new YowModel(new File("datasets/yow-userstudy/exdupes-like-timeonpage-timeonmouse.csv"));
            	pre = new PreprocessorClustering(Clusterer.XMeans);
                return pre.preprocess((YowModel) model);

                
            // claypool userstudy
            case yow10kprocessedclassifier:
                model = new YowModel(new File("datasets/yow-userstudy/exdupes-like-timeonpage-timeonmouse.csv"));
                pre = new PreprocessorClassifier();
                return pre.preprocess((YowModel) model);
            case claypool2k:
            	return new SMDataModel(new File("datasets/claypool/claypool-sample-exdupes-exinvalid-rating.csv"));
            case claypool2kprocessed:
            	model = PreprocessorPuddis.getPreprocessedDataModel("datasets/claypool/claypool-sample-exdupes-rating-timeonpage-timeonmouse-pagetimesmouse.csv");
            	Preprocessor.writeDatasetToFileExplicit((SMDataModel) model, "/tmp/removing-invalid-ratings.csv");
                model = new FileDataModel(new File("/tmp/removing-invalid-ratings.csv"));
            	return model;

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
