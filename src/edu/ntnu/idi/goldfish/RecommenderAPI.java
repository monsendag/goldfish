package edu.ntnu.idi.goldfish;

import IceBreakRestServer.IceBreakRestServer;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.ntnu.idi.goldfish.configurations.Config;
import edu.ntnu.idi.goldfish.configurations.Lynx;
import edu.ntnu.idi.goldfish.mahout.DBModel;
import edu.ntnu.idi.goldfish.preprocessors.Preprocessor;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorStat;
import org.apache.commons.lang3.StringUtils;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class RecommenderAPI {
    // disable Mahout logging output
    static {
        System.setProperty("org.apache.commons.logging.Log",
                "org.apache.commons.logging.impl.NoOpLog");
    }

    public int counter = 0;
    public Date lastUpdate;
    public Recommender recommender;
    public BiMap<String, Long> userMaps;
    public BiMap<String, Long> itemMaps;

    private String hostname;
    private int port;
    private String username;
    private String password;
    private String database;
    private String collection;


    public static void main(String[] args) throws Exception {
        new RecommenderAPI();
    }

    public RecommenderAPI() throws Exception {
        try {

            // load configuration options from environment
            hostname = System.getenv("SMARTMEDIA_HOSTNAME");
            port = Integer.parseInt(System.getenv("SMARTMEDIA_PORT"));
            username = System.getenv("SMARTMEDIA_USERNAME");
            password = System.getenv("SMARTMEDIA_PASSWORD");
            database = System.getenv("SMARTMEDIA_DATABASE");
            collection = System.getenv("SMARTMEDIA_COLLECTION");

            // initialize rest server and set port
            IceBreakRestServer rest = new IceBreakRestServer();
            rest.setPort(8080);

            /**
             * The userIDs and itemIDs are represented as strings in mongo (i.e 5320767388b38d00075f7b91)
             * We create two biMaps where we store the mappings between these strings and our internal Long values
              */
            userMaps = HashBiMap.create();
            itemMaps = HashBiMap.create();


            // build recommendation model before starting
            rebuild();

            // wait for HTTP requests
            while (true) {
                try {
                    rest.getHttpRequest();
                    // set output content type to JSON
                    rest.setContentType("application/json");

                    // read and parse GET parameters
                    String rawUserID = rest.getQuery("recommendfor");
                    String notify = rest.getQuery("notify");
                    int howMany = Integer.parseInt(rest.getQuery("howMany", "10"));

                    // got ?recommendfor - try to provide recommendations
                    if (rawUserID != null && !rawUserID.isEmpty()) {

                        // get internal Long userID from map
                        long userID = userMaps.get(rawUserID);

                        // get list of recommendations
                        List<RecommendedItem> recommend = recommender.recommend(userID, howMany);

                        // iterate over recomendation items
                        List<String> items = recommend.stream().map(item -> {
                            // convert Long itemID to respective String ID
                            String itemID = String.valueOf(itemMaps.inverse().get(item.getItemID()));
                            // serialize to JSON
                            return String.format("{\"itemid\":\"%s\", \"rating\":\"%.2f\"}", itemID, item.getValue());
                        }).collect(Collectors.toList());

                        // return JSON array to client
                        rest.write(StringUtils.join(items));

                    }
                    // got notification about database change
                    else if (notify != null) {
                        // get number of minutes since last rebuild
                        long minuteDelta = (new Date().getTime() - lastUpdate.getTime()) / (1000 * 60);

                        // determine if we want to rebuild
                        if (counter++ > 10 && minuteDelta > 10) {
                            rebuild();
                        }
                    }
                    // invalid request
                    else {
                        throw new Exception("Invalid request. Expecting ?notify or ?recommendfor={userid}");
                    }

                }
                // catch all exceptions and try to return a parseable error response
                catch (Exception e) {
                    e.printStackTrace();
                    String out = String.format("{\"error\":\"%s: %s\"}", e.getClass().getSimpleName(), e.getMessage());
                    System.out.println(out);
                    rest.setStatus("400");
                    rest.write(out);
                }
            }

        }
        // catch any error and print stack trace
        catch(Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * This method will rebuild the recommendation model
     * @throws Exception
     */
    public void rebuild() throws Exception {
        System.out.println("Rebuilding...");

        // start timing of rebuild
        StopWatch.start("totalRebuild");

        // load dataset from database into DataModel
        StopWatch.start("getModel");
        DataModel model = new DBModel(hostname, port, username, password, database, collection, userMaps, itemMaps);
        StopWatch.print("getModel");

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
        StopWatch.print("preprocess");

        // build Recommendation model
        StopWatch.start("buildRecommender");
        Recommender newRecommender = ((Lynx) config).getBuilder().buildRecommender(model);
        StopWatch.print("buildRecommender");

        // replace recommendation model in memory
        recommender = newRecommender;

        // reset rebuild cycle
        counter = 0;
        lastUpdate = new Date();

        StopWatch.print("totalRebuild");
    }
}