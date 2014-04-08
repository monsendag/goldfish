package edu.ntnu.idi.goldfish.preprocessors;

import edu.ntnu.idi.goldfish.configurations.Config;
import org.apache.mahout.cf.taste.model.DataModel;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ConverterUtils;

public class PreprocessorNaiveBayes extends PreprocessorClassifier {



    @Override
    public DataModel preprocess(Config config) throws Exception {
        Classifier classifier = new NaiveBayes();

        String options = "";

        // set options on classifier
        classifier.setOptions(Utils.splitOptions(options));

        return classify(config, classifier, getDataset());
    }

    public static Instances getDataset() throws Exception {
        String file = "yow-preprocess-clustering-timeonpage-timeonmouse.arff";
        Instances dataset = new ConverterUtils.DataSource("datasets/yow-userstudy/arff/" + file).getDataSet();
        dataset.setClassIndex(0);
        return dataset;
    }
}