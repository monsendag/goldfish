package edu.ntnu.idi.goldfish;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.ntnu.idi.goldfish.configurations.Config;
import edu.ntnu.idi.goldfish.configurations.Lynx;
import edu.ntnu.idi.goldfish.mahout.DBModel;
import edu.ntnu.idi.goldfish.preprocessors.Preprocessor;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorStat;
import org.apache.commons.lang3.StringUtils;
import org.apache.mahout.cf.taste.common.NoSuchUserException;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.restlet.*;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.data.Status;
import org.restlet.routing.Router;
import org.restlet.routing.Template;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class RecommenderAPI extends Application {
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
        Component component = new Component();
        component.getDefaultHost().attach("", new RecommenderAPI());
        new Server(Protocol.HTTP, 8182, component).start();

    }

    public Restlet createInboundRoot() {
        Router router = new Router(getContext());
        router.setDefaultMatchingMode(Template.MODE_STARTS_WITH);

        // default route
        router.attachDefault(new Restlet() {
            public void handle(Request request, Response response) {
                System.err.println("Invalid request: "+request.toString());
                response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
                response.setEntity("Error: allowed paths /recommend/{userid} and /notify", MediaType.TEXT_PLAIN);
            }
        });

        // request for recommendations
        router.attach("/recommend/{userid}", new Restlet() {
            public void handle(Request request, Response response) {
                try {
                    // parse userid and make sure it exists
                    String rawUserid = (String) request.getAttributes().get("userid");
                    if(!userMaps.containsKey(rawUserid)) {
                        throw new NoSuchUserException("user id not found");
                    }
                    long userID = userMaps.get(rawUserid);

                    // parse optional ?limit={limit}
                    Parameter limitParam = request.getResourceRef().getQueryAsForm().getFirst("limit");
                    int limit = limitParam != null && !limitParam.getValue().isEmpty() ? Integer.parseInt(limitParam.getValue()) : 10;

                    // get list of recommendations
                    List<RecommendedItem> recommend = recommender.recommend(userID, limit);

                    // iterate over recomendation items
                    List<String> items = recommend.stream().map(item -> {
                        // convert Long itemID to respective String ID
                        String itemID = String.valueOf(itemMaps.inverse().get(item.getItemID()));
                        // serialize to JSON
                        return String.format("{\"itemid\":\"%s\", \"rating\":\"%.2f\"}", itemID, item.getValue());
                    }).collect(Collectors.toList());

                    response.setEntity(StringUtils.join(items), MediaType.APPLICATION_JSON);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    response.setStatus(Status.SERVER_ERROR_INTERNAL);
                    response.setEntity(String.format("{\"error\":\"%s\"}", e.getMessage()), MediaType.APPLICATION_JSON);
                }
            }
        });

        // update notification
        router.attach("/notify", new Restlet() {
            public void handle(Request request, Response response) {
                try {
                    // get number of minutes since last rebuild
                    long minuteDelta = (new Date().getTime() - lastUpdate.getTime()) / (1000 * 60);
                    String out = "";
                    // determine if we want to rebuild
                    if (counter++ > 10 && minuteDelta > 10) {
                        rebuild();
                        out += String.format("%d minutes passed ... rebuilding", minuteDelta);
                    }
                    System.out.format("%s - %s\n", request.toString(), out);
                    response.setEntity("1", MediaType.APPLICATION_JSON);
                }
                catch(Exception e) {
                    e.printStackTrace();
                    response.setStatus(Status.SERVER_ERROR_INTERNAL);
                    response.setEntity(String.format("{\"error\":\"%s\"}", e.getMessage()), MediaType.APPLICATION_JSON);
                }
            }
        });

        return router;
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

            /**
             * The userIDs and itemIDs are represented as strings in mongo (i.e 5320767388b38d00075f7b91)
             * We create two biMaps where we store the mappings between these strings and our internal Long values
              */
            userMaps = HashBiMap.create();
            itemMaps = HashBiMap.create();

            // build recommendation model before starting
            rebuild();
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