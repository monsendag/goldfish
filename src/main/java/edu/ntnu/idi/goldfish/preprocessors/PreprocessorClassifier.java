package edu.ntnu.idi.goldfish.preprocessors;

import edu.ntnu.idi.goldfish.StopWatch;
import edu.ntnu.idi.goldfish.configurations.Config;
import edu.ntnu.idi.goldfish.mahout.DBModel;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;

import java.util.List;
import java.util.stream.Collectors;

public abstract class PreprocessorClassifier extends Preprocessor {


    public DBModel classify(Config config, Classifier classifier, Instances dataset) throws Exception {
        DBModel model = config.get("model");
        double threshold = config.get("threshold");

        StopWatch.start("classifier-build");
        classifier.buildClassifier(dataset);
        config.set("time:preprocess-build", StopWatch.get("classifier-build"));

        List<DBModel.DBRow> results = model.getFeedbackRows().stream().filter(row -> row.rating == 0).collect(Collectors.toList());
        for (DBModel.DBRow row : results) {
            Instance un = new Instance(1, new double[]{1, row.timeonpage, row.timeonmouse});
            un.setDataset(dataset);

            double rating;
            double index = classifier.classifyInstance(un);
            double[] distributions = classifier.distributionForInstance(un);

            if(dataset.classAttribute().isNominal()) {
                // ensure we have a certain threshold
                if(distributions[(int)index] < threshold) {
                    continue;
                }

                // get rating value of classification
                double[] values = new double[]{1.0, 2.0, 3.0, 4.0, 5.0};

//                System.out.format("%s u:%d i:%6d res: %.2f  probs:%s\n", prep.getSimpleName(), row.userid, row.itemid, rating, Arrays.toString(distributions));
                rating = values[(int)index];
            }

            // rating is not an index
            else {
                rating = index;
            }

            model.setPreference(row.userid, row.itemid, (float) Math.round(rating));
            addPseudoPref(row.userid, row.itemid);
        }
        return model;
    }
}