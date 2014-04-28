package edu.ntnu.idi.goldfish.preprocessors;

import edu.ntnu.idi.goldfish.configurations.Config;
import edu.ntnu.idi.goldfish.mahout.DBModel;
import weka.classifiers.Classifier;
import weka.classifiers.lazy.IBk;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ConverterUtils;

public class PreprocessorIBK extends PreprocessorClassifier {

    public enum DistanceWeighting {
        InverseDistance,
        Distance,
        None
    }

    public enum ErrorMinimization {
        MinimizeMeanSquaredError,
        MinimizeMeanAbsoluteError
    }

    public enum NeighborSearchMethod {
        BallTree,
        CoverTree,
        KDTree,
        LinearNN
    }

    private String getMethod(NeighborSearchMethod method) {
        switch (method){
            case BallTree:
                return "weka.core.neighboursearch.BallTree -A \\\"weka.core.EuclideanDistance -R first-last\\\" -C \\\"weka.core.neighboursearch.balltrees.TopDownConstructor -N 40 -S weka.core.neighboursearch.balltrees.PointsClosestToFurthestChildren\\\"";
            case CoverTree:
                return "weka.core.neighboursearch.CoverTree -A \\\"weka.core.EuclideanDistance -R first-last\\\" -B 1.3";
            case KDTree:
                return "weka.core.neighboursearch.KDTree -A \\\"weka.core.EuclideanDistance -R first-last\\\" -S weka.core.neighboursearch.kdtrees.SlidingMidPointOfWidestSide -W 0.01 -L 40 -N";
            case LinearNN:
            default:
                return "weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance -R first-last\\\"";
        }
    }

    @Override
    public DBModel getProcessedModel(Config config) throws Exception {
        Classifier classifier = new IBk();

        DistanceWeighting distanceMeasure = config.get("distanceMeasure", DistanceWeighting.Distance);
        ErrorMinimization minimization = config.get("minimization", ErrorMinimization.MinimizeMeanAbsoluteError);
        NeighborSearchMethod method = config.get("method", NeighborSearchMethod.BallTree);

        int K = config.get("K", 5);
        int window = config.get("window", 0);

        String options = "";
        options += " -K " + K;
        options += " -W " + window;

        if(distanceMeasure == DistanceWeighting.InverseDistance)
            options += " -I";
        else if(distanceMeasure == DistanceWeighting.Distance)
            options += " -F";

        if(minimization == ErrorMinimization.MinimizeMeanSquaredError)
            options += " -E";

        options += " -A \"" + getMethod(method) + "\"";

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