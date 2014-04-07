package edu.ntnu.idi.goldfish;

import edu.ntnu.idi.goldfish.configurations.Config;
import edu.ntnu.idi.goldfish.configurations.Lynx;
import edu.ntnu.idi.goldfish.preprocessors.*;
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
                .set("model", yowImplicit.getModel())
                .set("preprocessor", PreprocessorClassifier.class)
                .set("average", 5000);

        List<Class> classes = Arrays.asList(NaiveBayes.class, SMOreg.class);
        classes.stream().forEach(c ->
            configs.add(classifiers.clone().set("name", c.getSimpleName()).set("classifier", c))
        );


        Config clustering = new Lynx()
                .set("name", "clustering")
                .set("model", yowImplicit.getModel())
                .set("preprocessor", PreprocessorClustering.class);

        Config mlr = new Lynx()
                .set("name", "mlr")
                .set("model", yowImplicit.getModel())
                .set("numberOfIndependentVariables", 1)
                .set("preprocessor", PreprocessorMLR.class);


//        configurations.add(baseLine);
//        configurations.add(stat);
//        configurations.add(puddis);
        configs.add(classifiers);
//        configurations.add(clustering);
//        configurations.add(mlr);

        Columns columns = Columns.getPrintFormats("name", "average", "RMSE", "evalTime");
        results.setColumns(columns);

		StopWatch.start("total evaluation");
        System.out.format("Starting evaluation of %d configurations \n", configs.size());
        Evaluator.evaluate(configs, results, res -> System.out.println(res.toString(columns)));

//        results.print();
        System.out.println(StringUtils.repeat("=", 190));
//        results.printSummary();

        System.out.format("Evaluated %d configurations in %s \n", configs.size(), StopWatch.str("total evaluation"));
        results.save();

    }
}
