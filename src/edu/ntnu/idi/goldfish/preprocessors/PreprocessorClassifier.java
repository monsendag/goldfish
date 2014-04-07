package edu.ntnu.idi.goldfish.preprocessors;

import edu.ntnu.idi.goldfish.StopWatch;
import edu.ntnu.idi.goldfish.configurations.Config;
import edu.ntnu.idi.goldfish.mahout.DBModel;
import org.apache.mahout.cf.taste.model.DataModel;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.lazy.IBk;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import java.util.List;
import java.util.stream.Collectors;

public class PreprocessorClassifier extends Preprocessor {

    public static void main(String[] args) throws Exception {
//        Classifier classifier = new SMOreg();
        Classifier classifier = new IBk();

        classifier.setDebug(true);

        String path = "datasets/yow-userstudy/arff/exdupes-exinvalid-like-timeonpage-timeonmouse.arff";
        String testpath = "datasets/yow-userstudy/arff/exdupes-exinvalid-like-timeonpage-timeonmouse-testset.arff";
        Instances dataset = new ConverterUtils.DataSource(path).getDataSet();
        Instances testset = new ConverterUtils.DataSource(testpath).getDataSet();
        dataset.setClassIndex(0);
        testset.setClassIndex(0);

        System.out.format("Building classifier from %s\n", path);
        StopWatch.start("build-classifier");
        classifier.buildClassifier(dataset);
        StopWatch.print("build-classifier");


        Evaluation eval = new Evaluation(dataset);
        eval.evaluateModel(classifier, testset);

        System.out.print("      > " + eval.toSummaryString().trim().replace("\n", "\n      > "));

//        System.out.format("class: %.2f\n", classifier.classifyInstance(new Instance(1, new double[]{0, -0, 421421414})));
//        System.out.format("class: %.2f\n", classifier.classifyInstance(new Instance(1, new double[]{0, 0012121, 21})));
//        System.out.format("class: %.2f\n", classifier.classifyInstance(new Instance(1, new double[]{0, 99919, -2101222})));
//        System.out.format("class: %.2f\n", classifier.classifyInstance(new Instance(1, new double[]{0, -10004, 001212})));

    }

    @Override
    public DataModel preprocess(Config config) throws Exception {
        DBModel model = config.get("model");

        Class<Classifier> classifierClass = config.get("classifier");
        Classifier classifier = classifierClass.newInstance();

        Instances dataset = new ConverterUtils.DataSource("datasets/yow-userstudy/arff/yow-preprocess-clustering-timeonpage-timeonmouse.arff").getDataSet();
        dataset.setClassIndex(0);

        StopWatch.start("build-classifier");
        classifier.buildClassifier(dataset);
        StopWatch.print("build-classifier");

        StopWatch.start("classify");
        List<DBModel.DBRow> results = model.getFeedbackRows().stream().filter(row -> row.rating == 0).collect(Collectors.toList());
        for(DBModel.DBRow row : results) {
            Instance un = new Instance(1, new double[]{0, row.timeonpage, row.timeonmouse});
            un.setDataset(dataset);
            double rating = classifier.classifyInstance(un);
//            System.out.format("classify: u: %d  i: %6d  estimate: %.2f\n", row.userid, row.itemid, rating);
            model.setPreference(row.userid, row.itemid, (float) Math.round(rating));
            pseudoRatings.add(String.format("%d_%d", row.userid, row.itemid));
        }
        StopWatch.print("classify");
        return model;
    }

//    public void iris() throws IOException {
//        // this test trains a 3-way classifier on the famous Iris dataset.
//        // a similar exercise can be accomplished in R using this code:
//        //    library(nnet)
//        //    correct = rep(0,100)
//        //    for (j in 1:100) {
//        //      i = order(runif(150))
//        //      train = iris[i[1:100],]
//        //      test = iris[i[101:150],]
//        //      m = multinom(Species ~ Sepal.Length + Sepal.Width + Petal.Length + Petal.Width, train)
//        //      correct[j] = mean(predict(m, newdata=test) == test$Species)
//        //    }
//        //    hist(correct)
//        //
//        // Note that depending on the training/test split, performance can be better or worse.
//        // There is about a 5% chance of getting accuracy < 90% and about 20% chance of getting accuracy
//        // of 100%
//        //
//        // This test uses a deterministic split that is neither outstandingly good nor bad
//
//
//        RandomUtils.useTestSeed();
//        Splitter onComma = Splitter.on(",");
//
//        // read the data
//        List<String> raw = Resources.readLines(Resources.getResource("iris.csv"), Charsets.UTF_8);
//
//        // holds features
//        List<Vector> data = Lists.newArrayList();
//
//        // holds target variable
//        List<Integer> target = Lists.newArrayList();
//
//        // for decoding target values
//        Dictionary dict = new Dictionary();
//
//        // for permuting data later
//        List<Integer> order = Lists.newArrayList();
//
//        for (String line : raw.subList(1, raw.size())) {
//            // order gets a list of indexes
//            order.add(order.size());
//
//            // parse the predictor variables
//            Vector v = new DenseVector(5);
//            v.set(0, 1);
//            int i = 1;
//            Iterable<String> values = onComma.split(line);
//            for (String value : Iterables.limit(values, 4)) {
//                v.set(i++, Double.parseDouble(value));
//            }
//            data.add(v);
//
//            // and the target
//            target.add(dict.intern(Iterables.get(values, 4)));
//        }
//
//        // randomize the order ... original data has each species all together
//        // note that this randomization is deterministic
//        Random random = RandomUtils.getRandom();
//        Collections.shuffle(order, random);
//
//        // select training and test data
//        List<Integer> train = order.subList(0, 100);
//        List<Integer> test = order.subList(100, 150);
//
//        // now train many times and collect information on accuracy each time
//        int[] correct = new int[test.size() + 1];
//        for (int run = 0; run < 200; run++) {
//            OnlineLogisticRegression lr = new OnlineLogisticRegression(3, 5, new L2(1));
//            // 30 training passes should converge to > 95% accuracy nearly always but never to 100%
//            for (int pass = 0; pass < 30; pass++) {
//                Collections.shuffle(train, random);
//                for (int k : train) {
//                    lr.train(target.get(k), data.get(k));
//                }
//            }
//
//            // check the accuracy on held out data
//            int x = 0;
//            int[] count = new int[3];
//            for (Integer k : test) {
//                int r = lr.classifyFull(data.get(k)).maxValueIndex();
//                count[r]++;
//                x += r == target.get(k) ? 1 : 0;
//            }
//            correct[x]++;
//        }
//
//        // verify we never saw worse than 95% correct,
//        for (int i = 0; i < Math.floor(0.95 * test.size()); i++) {
//            assertEquals(String.format("%d trials had unacceptable accuracy of only %.0f%%: ", correct[i], 100.0 * i / test.size()), 0, correct[i]);
//        }
//        // nor perfect
//        assertEquals(String.format("%d trials had unrealistic accuracy of 100%%", correct[test.size() - 1]), 0, correct[test.size()]);
//    }
//
//    public void testTrain() throws Exception {
//        Vector target = readStandardData();
//
//
//        // lambda here needs to be relatively small to avoid swamping the actual signal, but can be
//        // larger than usual because the data are dense.  The learning rate doesn't matter too much
//        // for this example, but should generally be < 1
//        // --passes 1 --rate 50 --lambda 0.001 --input sgd-y.csv --features 21 --output model --noBias
//        //   --target y --categories 2 --predictors  V2 V3 V4 V5 V6 V7 --types n
//        OnlineLogisticRegression lr = new OnlineLogisticRegression(2, 8, new L1())
//                .lambda(1 * 1.0e-3)
//                .learningRate(50);
//
//        train(getInput(), target, lr);
//        test(getInput(), target, lr, 0.05, 0.3);
//    }
}