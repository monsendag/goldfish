package edu.ntnu.idi.goldfish;

import edu.ntnu.idi.goldfish.configurations.Config;
import edu.ntnu.idi.goldfish.configurations.Lynx;
import edu.ntnu.idi.goldfish.preprocessors.*;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorPuddis.PredMethod;

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

        Config puddis2 = new Lynx()
	        .set("name", "puddis2")
	        .set("model", yowSMImplicit.getModel())
	        .set("average", 100)
	        .set("minTimeOnPage", 20000)
			.set("correlationLimit", 0.5)
			.set("predictionMethod", PredMethod.LinearRegression);
        configs.add(puddis2);
        
        Config puddis = new Lynx()
                .set("name", "puddis")
                .set("model", yowSMImplicit.getModel())
                .set("average", 100);

        Config conf;
        for(int minT = 15000; minT <= 30000; minT+=5000) {
	    	for(double corrLimit = 0.4; corrLimit <= 0.8; corrLimit += 0.1) {
	    		for(PredMethod method : PredMethod.values()){
	    			conf = puddis.clone()
	    				.set("minTimeOnPage", minT)
	    				.set("correlationLimit", corrLimit)
	    				.set("predictionMethod", method);
	    			
	    			//configs.add(conf);   	
	    		}
	    	}
        }
        

        Config classifiers = new Lynx()
                .set("model", yowImplicit.getModel())
                .set("preprocessor", PreprocessorClassifier.class)
                .set("average", 5000);

        List<Class> classes = Arrays.asList(NaiveBayes.class, SMOreg.class);
////        classes.stream().forEach(c ->
////            configs.add(classifiers.clone().set("name", c.getSimpleName()).set("classifier", c))
//        );


        Config clustering = new Lynx()
                .set("name", "clustering")
                .set("model", yowImplicit.getModel())
                .set("preprocessor", PreprocessorClustering.class);

        Config mlr = new Lynx()
                .set("name", "mlr")
                .set("model", yowImplicit.getModel())
                .set("numberOfIndependentVariables", 1)
                .set("preprocessor", PreprocessorMLR.class);


        configs.add(baseLine);
//        configurations.add(stat);
//        configurations.add(puddis);
//        configs.add(classifiers);
//        configurations.add(clustering);
//        configurations.add(mlr);

        Columns columns = Columns.getPrintFormats("name", "average", "RMSE", "evalTime", "minTimeOnPage", "correlationLimit", "predictionMethod");
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
