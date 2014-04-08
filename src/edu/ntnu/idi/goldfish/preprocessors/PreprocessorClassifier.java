package edu.ntnu.idi.goldfish.preprocessors;

import edu.ntnu.idi.goldfish.StopWatch;
import edu.ntnu.idi.goldfish.configurations.Config;
import edu.ntnu.idi.goldfish.mahout.DBModel;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.model.DataModel;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public abstract class PreprocessorClassifier extends Preprocessor {


    public DataModel classify(Config config, Classifier classifier, Instances dataset) throws Exception {
        DBModel model = config.get("model");

        StopWatch.start("classifier-build");
        classifier.buildClassifier(dataset);
        config.set("time:preprocess-build", StopWatch.get("classifier-build"));

        List<DBModel.DBRow> results = model.getFeedbackRows().stream().filter(row -> row.rating == 0).collect(Collectors.toList());
        for (DBModel.DBRow row : results) {
            Instance un = new Instance(1, new double[]{0, row.timeonpage, row.timeonmouse});
            un.setDataset(dataset);
            double rating = classifier.classifyInstance(un);
//            System.out.format("classify: u: %d  i: %6d  estimate: %.2f\n", row.userid, row.itemid, rating);
            model.setPreference(row.userid, row.itemid, (float) Math.round(rating));
            pseudoRatings.add(String.format("%d_%d", row.userid, row.itemid));
        }


        String tempPath = String.format("/tmp/preprocessor-classifier-remove-invalid-%s.csv", Thread.currentThread().hashCode());
        model.DBModelToCsv(model, tempPath);
        return new FileDataModel(new File(tempPath));
    }

    public abstract DataModel preprocess(Config config) throws Exception;

}