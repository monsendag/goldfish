package edu.ntnu.idi.goldfish.preprocessors;

import edu.ntnu.idi.goldfish.configurations.Config;
import edu.ntnu.idi.goldfish.mahout.DBModel;
import weka.classifiers.Classifier;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ConverterUtils;

public class PreprocessorANN extends PreprocessorClassifier {

    @Override
    public DBModel getProcessedModel(Config config) throws Exception {
        Classifier classifier = new MultilayerPerceptron();

        double learningRate = config.get("learningRate", 0.3);
        double momentum = config.get("momentum", 0.2);
        int epochs = config.get("epochs", 500);
        String neurons = config.get("neurons", "a");

        String options = String.format("-L %s -M %s -N %s -V 0 -S 0 -E 20 -H %s", learningRate, momentum, epochs, neurons);

        // set options on classifier
        classifier.setOptions(Utils.splitOptions(options));

        return classify(config, classifier, getDataset());
    }

    public static Instances getDataset() throws Exception {
        String file = "real/yow-preprocess-clustering-timeonpage-timeonmouse.arff";
        Instances dataset = new ConverterUtils.DataSource("datasets/yow-userstudy/arff/" + file).getDataSet();
        dataset.setClassIndex(0);
        return dataset;
    }
}