package edu.ntnu.idi.goldfish;

import edu.ntnu.idi.goldfish.configurations.Config;
import edu.ntnu.idi.goldfish.configurations.Lynx;
import edu.ntnu.idi.goldfish.mahout.DBModel;
import edu.ntnu.idi.goldfish.preprocessors.Preprocessor;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorStat;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.Recommender;

public class Timing {

    public int i = 1;

    public static DataSet set;

	// disable Mahout logging output
	static {
		 System.setProperty("org.apache.commons.logging.Log",
		 "org.apache.commons.logging.impl.NoOpLog");
	}

	public static void main(String[] args) throws Exception {
        new Timing(args);
    }

    public Timing(String[] args) throws Exception {
        // start timing of rebuild
        StopWatch.start("totalRebuild");

        // load dataset from database into DataModel
        StopWatch.start("getModel");

        DBModel.startServer();
        DataModel model = DataSet.MovielensMock1M.getModel();

//
        System.out.format("%s (%d users, %d items)\n", StopWatch.str("getModel"), model.getNumUsers(), model.getNumItems());

        // set configuration parameters and preprocessor
        Config config = new Lynx()
                .set("model", model)
                .set("minTimeOnPage", 25000)
                .set("correlationLimit", 0.3)
                .set("rating", 4)
                .set("predictionMethod", PreprocessorStat.PredictionMethod.ClosestNeighbor);

        // preprocess dataModel
        StopWatch.start("preprocess");
        Preprocessor preprocessor = new PreprocessorStat();
        model = preprocessor.preprocess(config);
        System.out.format("%s (%d users, %d items)\n", StopWatch.str("preprocess"), model.getNumUsers(), model.getNumItems());

        // build Recommendation model
        StopWatch.start("buildRecommender");
        Recommender newRecommender = ((Lynx) config).getBuilder().buildRecommender(model);
        StopWatch.print("buildRecommender");


    }
}
