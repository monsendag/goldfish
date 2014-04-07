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
                .set("model", yowBaseline.getModel());

        Config stat = new Lynx()
                .set("name", "stat")
                .set("model", yowImplicit.getModel())
                .set("predictionMethod", PreprocessorStat.PredictionMethod.LinearRegression)
                .set("preprocessor", PreprocessorStat.class);

        Config puddis = new Lynx()
                .set("name", "puddis")
                .set("model", yowSMImplicit.getModel())
                .set("preprocessor", PreprocessorPuddis.class);

        Config classifiers = new Lynx()
                .set("name", "classifier")
                .set("model", yowImplicit.getModel())
                .set("preprocessor", PreprocessorClassifier.class);

        Config clustering = new Lynx()
                .set("name", "clustering")
                .set("model", yowImplicit.getModel())
                .set("preprocessor", PreprocessorClustering.class);

        Config mlr = new Lynx()
                .set("name", "mlr")
                .set("model", yowImplicit.getModel())
                .set("preprocessor", PreprocessorMLR.class);


        configurations.add(baseLine);
        configurations.add(stat);
        configurations.add(puddis);
//        configurations.add(classifiers);
//        configurations.add(clustering);
//        configurations.add(mlr);

        results.setColumns("name", "RMSE", "evalTime");

		StopWatch.start("total evaluation");
//        System.out.format("Starting evaluation of %d configurations (%d users, %d items) \n", configurations.size(), dbModel.getNumUsers(), dbModel.getNumItems());

        Evaluator.evaluate(configurations, results);

        results.print();
        System.out.println(StringUtils.repeat("=", 190));
        results.printSummary();
        results.save();

//        System.out.format("Evaluated %d configurations (%d users, %d items) in %s \n", configurations.size(), dbModel.getNumUsers(), dbModel.getNumItems(), StopWatch.str("total evaluation"));
	}
}
