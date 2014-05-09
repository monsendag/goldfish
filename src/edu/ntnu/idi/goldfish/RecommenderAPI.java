package edu.ntnu.idi.goldfish;

import IceBreakRestServer.IceBreakRestServer;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.ntnu.idi.goldfish.configurations.Config;
import edu.ntnu.idi.goldfish.configurations.Lynx;
import edu.ntnu.idi.goldfish.preprocessors.Preprocessor;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorStat;
import org.apache.commons.lang3.StringUtils;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

// Drop this jar-file into you project
public class RecommenderAPI {
    // disable Mahout logging output
    static {
        System.setProperty("org.apache.commons.logging.Log",
                "org.apache.commons.logging.impl.NoOpLog");
    }

    public int counter = 0;
    public Date lastUpdate;
    public DataModel model;
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
        IceBreakRestServer rest = new IceBreakRestServer();
        rest.setPort(8080);

        userMaps = HashBiMap.create();
        itemMaps = HashBiMap.create();

        rebuild();

        while (true) {
            try {
                // Now wait for any HTTP request
                // the "config.properties" file contains the port we are listening on
                rest.getHttpRequest();
                rest.setContentType("application/json");

                String rawUserID = rest.getQuery("recommendfor");
                String notify = rest.getQuery("notify");
                int howMany = Integer.parseInt(rest.getQuery("howMany", "10"));

                if (rawUserID != null && !rawUserID.isEmpty()) {
                    long userID = Long.parseLong(rawUserID);

                    List<RecommendedItem> recommend = recommender.recommend(userID, howMany);
                    List<String> items = recommend.stream().map(item -> {
                        String itemID =  String.valueOf(item.getItemID());
//                        String itemID = itemMaps.inverse().get(item.getItemID());
                        return String.format("{\"itemid\":\"%s\", \"rating\":\"%.2f\"}", itemID, item.getValue());
                    }).collect(Collectors.toList());

                    rest.write(StringUtils.join(items));

                } else if(notify != null) {
                    long minuteDelta = (new Date().getTime() - lastUpdate.getTime())/ (1000*60);
                    if(counter++ > 10 && minuteDelta > 10) {
                        rebuild();
                    }
                }
                else {
                    throw new Exception("Invalid request. Expecting ?notify or ?recommendfor={userid}");
                }

            } catch (Exception e) {
                e.printStackTrace();
                String out = String.format("{\"error\":\"%s: %s\"}", e.getClass().getSimpleName(), e.getMessage());
                System.out.println(out);
                rest.setStatus("400");
                rest.write(out);
            }
        }
    }

    public void rebuild() throws Exception {

        System.out.println("Rebuilding.. ");
        StopWatch.start("totalRebuild");

        StopWatch.start("getModel");
        DataModel model = new DBModel(hostname, port, username, password, database, collection, userMaps, itemMaps);
        StopWatch.print("getModel");

        Config config = new Lynx()
            .set("model", model)
            .set("minTimeOnPage", 25000)
            .set("correlationLimit", 0.3)
            .set("predictionMethod", PreprocessorStat.PredictionMethod.ClosestNeighbor);

        StopWatch.start("preprocess");
        Preprocessor preprocessor = new PreprocessorStat();
        model = preprocessor.preprocess(config);
        StopWatch.print("preprocess");

        StopWatch.start("buildRecommender");
        recommender = ((Lynx) config).getBuilder().buildRecommender(model);
        StopWatch.print("buildRecommender");

        counter = 0;
        lastUpdate = new Date();

        StopWatch.print("totalRebuild");
    }
}