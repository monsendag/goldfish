package edu.ntnu.idi.goldfish;

import edu.ntnu.idi.goldfish.configurations.Config;
import edu.ntnu.idi.goldfish.configurations.Lynx;
import edu.ntnu.idi.goldfish.preprocessors.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
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
		List<Config> configurations = new ArrayList<>();
		ResultList results = new ResultList();

        Config baseLine = new Lynx()
                .set("name", "baseline")
                .set("model", yowBaseline.getModel())
                .set("average", 100);

        Config stat = new Lynx()
                .set("name", "stat")
                .set("model", yowImplicit.getModel())
                .set("predictionMethod", PreprocessorStat.PredictionMethod.LinearRegression)
                .set("minTimeOnPage", 20000)
                .set("correlationLimit", 0.5)
                .set("preprocessor", PreprocessorStat.class)
                .set("average", 100);

        Config puddis = new Lynx()
                .set("name", "puddis")
                .set("model", yowSMImplicit.getModel())
                .set("predictionMethod", PreprocessorPuddis.PredMethod.LinearRegression)
                .set("minTimeOnPage", 20000)
                .set("correlationLimit", 0.5)
                .set("preprocessor", PreprocessorPuddis.class)
                .set("average", 100);


        Config classifiers = new Lynx()
                .set("name", "classifier")
                .set("model", yowImplicit.getModel())
                .set("preprocessor", PreprocessorClassifier.class)
                .set("average", 10);

        Config clustering = new Lynx()
                .set("name", "clustering")
                .set("model", yowImplicit.getModel())
                .set("preprocessor", PreprocessorClustering.class);

        Config mlr = new Lynx()
                .set("name", "mlr")
                .set("model", yowImplicit.getModel())
                .set("numberOfIndependentVariables", 1)
                .set("preprocessor", PreprocessorMLR.class);


        configurations.add(baseLine);
        configurations.add(stat);
        configurations.add(puddis);
//        configurations.add(classifiers);
//        configurations.add(clustering);
//        configurations.add(mlr);

		StopWatch.start("total evaluation");
//        System.out.format("Starting evaluation of %d configurations (%d users, %d items) \n", configurations.size(), dbModel.getNumUsers(), dbModel.getNumItems());

        Evaluator.evaluate(configurations, results);
        results.setColumns("name", "average", "RMSE", "evalTime");

        results.print();
        System.out.println(StringUtils.repeat("=", 190));
//        results.printSummary();
        results.save();

//        System.out.format("Evaluated %d configurations (%d users, %d items) in %s \n", configurations.size(), dbModel.getNumUsers(), dbModel.getNumItems(), StopWatch.str("total evaluation"));
	}
}
