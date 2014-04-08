package edu.ntnu.idi.goldfish;

import edu.ntnu.idi.goldfish.configurations.Config;
import edu.ntnu.idi.goldfish.configurations.Lynx;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorClassifier;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorClustering;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorMLR;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorPuddis;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorPuddis.PredMethod;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorStat;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorStat.PredictionMethod;

import org.apache.commons.lang3.StringUtils;

import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.SMOreg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static edu.ntnu.idi.goldfish.DataSet.*;

public class Main {

    public static DataSet set;
	
	// disable Mahout logging output
	static {
		 System.setProperty("org.apache.commons.logging.Log",
		 "org.apache.commons.logging.impl.NoOpLog");
	}

	public static void main(String[] args) throws Exception {

        List<Config> configs = new ArrayList<>();
        ResultList results = new ResultList();

        /***********************************************************************************/
        // Baseline


        Config baseLine = new Lynx()
                .set("name", "baseline")
                .set("model", yowBaseline.getModel())
                .set("average", 100);

        configs.add(baseLine);
        /***********************************************************************************/
        // PreprocessorPuddis

        {
            Config puddis = new Lynx()
                    .set("name", "puddis")
                    .set("model", yowSMImplicit.getModel())
                    .set("preprocessor", PreprocessorPuddis.class)
                    .set("average", 10000);

            Config conf;
            for (int minT = 15000; minT <= 30000; minT += 5000) {
                for (double corrLimit = 0.4; corrLimit <= 0.8; corrLimit += 0.1) {
                    for (PredMethod method : PredMethod.values()) {
                        conf = puddis.clone()
                                .set("minTimeOnPage", minT)
                                .set("correlationLimit", corrLimit)
                                .set("predictionMethod", method);

//                        configs.add(conf);
                    }
                }
            }
            
            Config stat = new Lynx()
            .set("name", "STAT")
            .set("model", yowImplicit.getModel())
            .set("preprocessor", PreprocessorStat.class)
            .set("average", 1000)
            .set("minTimeOnPage", 20000)
            .set("correlationLimit", 0.5)
            .set("predictionMethod", PredictionMethod.LinearRegression);
//            configs.add(stat);
            
            Config pudd = new Lynx()
            .set("name", "PUDD")
            .set("model", yowSMImplicit.getModel())
            .set("preprocessor", PreprocessorPuddis.class)
            .set("average", 1000)
            .set("minTimeOnPage", 20000)
            .set("correlationLimit", 0.5)
            .set("predictionMethod", PredMethod.LinearRegression);
//            configs.add(pudd);
        }

        /***********************************************************************************/
        // PreprocessorClassifier

        Config classifiers = new Lynx()
                .set("name", "classifier")
                .set("model", yowImplicit.getModel())
                .set("preprocessor", PreprocessorClassifier.class)
                .set("average", 10);

        List<Class> classes = Arrays.asList(NaiveBayes.class, SMOreg.class);
//        classes.stream().forEach(c ->
//            configs.add(classifiers.clone().set("name", c.getSimpleName()).set("classifier", c))
//        );

        /***********************************************************************************/
        // PreprocessorClustering

        {

            Config clustering = new Lynx()
                    .set("name", "clustering")
                    .set("model", yowImplicit.getModel())
                    .set("preprocessor", PreprocessorClustering.class)
                    .set("average", 100);

            Config conf;
            for(PreprocessorClustering.Clusterer clusterer : PreprocessorClustering.Clusterer.values()) {
                for(PreprocessorClustering.ClusterDataset dataset : PreprocessorClustering.ClusterDataset.values()) {
                    conf = clustering.clone()
                            .set("clusterer", clusterer)
                            .set("clusterDataset", dataset);
//                    configs.add(conf);
                }
            }
        }

        /***********************************************************************************/
        // PreprocessorMLR

        {
            Config mlr = new Lynx()
                    .set("name", "MLR")
                    .set("model", yowImplicit.getModel())
                    .set("preprocessor", PreprocessorMLR.class)
                    .set("average", 1000);

            Config conf;
            for (int i = 1; i <= 3; i++) {
                conf = mlr.clone()
                        .set("numberOfIndependentVariables", i);
                configs.add(conf);
            }
        }

        /***********************************************************************************/

        Columns columns = Columns.getPrintFormats("name", "average", "RMSE", "evalTime", "minTimeOnPage", "correlationLimit", "predictionMethod", "numberOfIndependentVariables");
        results.setColumns(columns);

		StopWatch.start("total evaluation");
        System.out.format("Starting evaluation of %d configurations \n", configs.size());
        Evaluator.evaluate(configs, results, res -> System.out.println(res.toString(columns)));

//        results.print();
        System.out.println(StringUtils.repeat("=", 190));
        results.printSummary();

        System.out.format("Evaluated %d configurations in %s \n", configs.size(), StopWatch.str("total evaluation"));
        results.save(Columns.getSaveFormats("name", "average", "RMSE", "evalTime", "minTimeOnPage", "correlationLimit", "predictionMethod", "numberOfIndependentVariables"));

    }
}
