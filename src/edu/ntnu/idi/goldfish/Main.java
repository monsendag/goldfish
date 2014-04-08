package edu.ntnu.idi.goldfish;

import edu.ntnu.idi.goldfish.configurations.Config;
import edu.ntnu.idi.goldfish.configurations.Lynx;
import edu.ntnu.idi.goldfish.preprocessors.*;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorPuddis.PredMethod;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorStat.PredictionMethod;
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

        List<Config> configs = new ArrayList<>();
        ResultList results = new ResultList();
        ArrayList<String> cols = new ArrayList<>();

        Config conf;

        /***********************************************************************************/
        // Baseline


        Config baseLine = new Lynx()
                .set("name", "baseline")
                .set("model", yowBaseline.getModel())
                .set("average", 10000);

//        configs.add(baseLine);
        /***********************************************************************************/
        // PreprocessorPuddis

//        if(false)
        {
            Config puddis = new Lynx()
                    .set("name", "puddis")
                    .set("model", yowSMImplicit.getModel())
                    .set("preprocessor", PreprocessorPuddis.class)
                    .set("average", 10000);

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

        }
        
        /***********************************************************************************/
        // PreprocessorStat
        
//        if(false)
        {
        	Config stat = new Lynx()
		    	.set("name", "stat")
		    	.set("model", yowImplicit.getModel())
		    	.set("preprocessor", PreprocessorStat.class)
		    	.set("average", 10000);

            for (int minT = 15000; minT <= 30000; minT += 5000) {
                for (double corrLimit = 0.4; corrLimit <= 0.8; corrLimit += 0.1) {
                    for (PredictionMethod method : PredictionMethod.values()) {
                        conf = stat.clone()
                                .set("minTimeOnPage", minT)
                                .set("correlationLimit", corrLimit)
                                .set("predictionMethod", method);

//                        configs.add(conf);
                    }
                }
            }

        }

        /***********************************************************************************/
        // PreprocessorSMOreg

//        if(false)
        {
            Config smoreg = new Lynx()
                    .set("name", "smoreg")
                    .set("model", yowImplicit.getModel())
                    .set("preprocessor", PreprocessorSMOreg.class)
                    .set("average", 10);

            for (PreprocessorSMOreg.Kernel kernel : PreprocessorSMOreg.Kernel.values()) {
                conf = smoreg.clone().set("kernel", kernel);
                configs.add(conf);
            }

        }

        /***********************************************************************************/
        // PreprocessorANN
//        if(false)
        {
            Config ann = new Lynx()
                    .set("name", "ann")
                    .set("model", yowImplicit.getModel())
                    .set("preprocessor", PreprocessorANN.class)
                    .set("average", 10);

            configs.add(ann);
        }


        /***********************************************************************************/
        // PreprocessorIBK
//        if(false)
        {

            Config ibk = new Lynx()
                    .set("name", "ibk")
                    .set("model", yowImplicit.getModel())
                    .set("preprocessor", PreprocessorIBK.class)
                    .set("average", 10);

            configs.add(ibk);
        }
        /***********************************************************************************/
        // PreprocessorNaiveBayes
//        if(false)
        {
            Config naivebayes = new Lynx()
                    .set("name", "naivebayes")
                    .set("model", yowImplicit.getModel())
                    .set("preprocessor", PreprocessorNaiveBayes.class)
                    .set("average", 10);


            configs.add(naivebayes);
        }
        /***********************************************************************************/
        // PreprocessorClustering

//        if(false)
        {
            Config clustering = new Lynx()
                    .set("name", "clustering")
                    .set("model", yowImplicit.getModel())
                    .set("preprocessor", PreprocessorClustering.class)
                    .set("average", 10000);

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
                    .set("average", 10000);

            for (int i = 1; i <= 3; i++) {
                conf = mlr.clone()
                        .set("numberOfIndependentVariables", i);
//                configs.add(conf);
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
