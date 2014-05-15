package edu.ntnu.idi.goldfish;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.ntnu.idi.goldfish.configurations.Config;
import edu.ntnu.idi.goldfish.configurations.Lynx;
import edu.ntnu.idi.goldfish.mahout.DBModel;
import edu.ntnu.idi.goldfish.preprocessors.Preprocessor;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorStat;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.mahout.cf.taste.common.NoSuchUserException;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.restlet.*;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.data.Status;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

public class RecommenderAPI extends Application {

    public int counter = 0;
    public Date lastUpdate;
    public Recommender recommender;
    public BiMap<String, Long> userMap;
    public BiMap<String, Long> itemMap;

    private String hostname;
    private int port;
    private String username;
    private String password;
    private String database;
    private String collection;
    static Logger log;

    public static void main(String[] args) throws Exception {
        log = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        log.setLevel(Level.INFO);

        Component component = new Component();
        component.getDefaultHost().attach("", new RecommenderAPI());
        Server server = component.getServers().add(Protocol.HTTP, 8182);
        server.getContext().getParameters().add("useForwardedForHeader", "true");
        component.start();
    }

    public static String getJSONError(Exception e) {
        try {
            JSONObject out = new JSONObject();
            out.put("stacktrace", ExceptionUtils.getStackTrace(e));
            out.put("error", e.getClass().getSimpleName());
            out.put("message", e.getMessage());
            return out.toString();

        } catch (JSONException e1) {
            e1.printStackTrace();
            return getJSONError(e1);
        }
    }

    public Restlet createInboundRoot() {
        Router router = new Router(getContext());
        router.setDefaultMatchingMode(Template.MODE_STARTS_WITH);

        // default route
        router.attachDefault(new Restlet() {
            public void handle(Request request, Response response) {
                System.err.println("Invalid request: "+request.toString());
                response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
                response.setEntity(getJSONError(new Exception("allowed paths are /recommend/{userid} and /notify")), MediaType.APPLICATION_JSON);
            }
        });

        router.attach("/favico", new Restlet() {
            @Override
            public void handle(Request request, Response response) {
                response.setEntity("null", MediaType.TEXT_PLAIN);
            }
        });

        // request for recommendations
        router.attach("/recommend/{userid}", new Restlet() {
            public void handle(Request request, Response response) {

                JSONObject output = new JSONObject();

                try {
                    // parse userid and make sure it exists
                    String rawUserid = (String) request.getAttributes().get("userid");
                    if (!userMap.containsKey(rawUserid)) {
                        throw new NoSuchUserException(String.format("User %s does not exist", rawUserid));
                    }
                    long userID = userMap.get(rawUserid);

                    // parse optional ?limit={limit}
                    Parameter limitParam = request.getResourceRef().getQueryAsForm().getFirst("limit");
                    int limit = limitParam != null && !limitParam.getValue().isEmpty() ? Integer.parseInt(limitParam.getValue()) : 10;

                    // get list of recommendations
                    List<RecommendedItem> recommend = recommender.recommend(userID, limit);

                    JSONArray items = new JSONArray();
                    // iterate over recomendation items
                    for (RecommendedItem item : recommend) {
                        // convert Long itemID to respective String ID
                        String itemID = String.valueOf(itemMap.inverse().get(item.getItemID()));
                        // serialize to JSON

                        JSONObject itemObject = new JSONObject();
                        itemObject.put("itemid", itemID);
                        itemObject.put("rating", String.format("%.2f", item.getValue()));
                        items.put(itemObject);
                    }

                    output.put("data", items);

                    response.setEntity(output.toString(), MediaType.APPLICATION_JSON);
                }
                catch (Exception e) {

                    // would have constructed a proper JSON object here, but that might throw an exception
                    // so we do it manually

                    String out = getJSONError(e);
                    System.out.println(out);

                    response.setStatus(Status.SERVER_ERROR_INTERNAL);
                    response.setEntity(out, MediaType.APPLICATION_JSON);
                }
            }
        });

        // update notification
        router.attach("/notify", new Restlet() {
            public void handle(Request request, Response response) {
                try {

                    // get number of minutes since last rebuild
                    long minuteDelta = (new Date().getTime() - lastUpdate.getTime()) / (1000 * 60);

                    boolean doRebuild = counter > 10 && minuteDelta > 10;

                    JSONObject output = new JSONObject();
                    output.put("rebuild", Boolean.toString(doRebuild));
                    output.put("lastrebuild", lastUpdate.getTime()/1000); // unix timestamp (convert ms to sec)
                    output.put("updates", counter);

                    response.setEntity(output.toString(), MediaType.APPLICATION_JSON);



                    if (doRebuild) {
                        rebuild();
                    }

                    // increment notification updates
                    counter ++;
                }
                catch(Exception e) {

                    // would have constructed a proper JSON object here, but that might throw an exception
                    // so we do it manually

                    String out = getJSONError(e);
                    System.out.println(out);

                    response.setStatus(Status.SERVER_ERROR_INTERNAL);
                    response.setEntity(out, MediaType.APPLICATION_JSON);
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

        /**
         * The userIDs and itemIDs are represented as strings in mongo (i.e 5320767388b38d00075f7b91)
         * We create two biMaps where we store the mappings between these strings and our internal Long values
         */

        BiMap<String, Long> newUserMap = HashBiMap.create();
        BiMap<String, Long> newitemMap = HashBiMap.create();


        // start timing of rebuild
        StopWatch.start("totalRebuild");

        // load dataset from database into DataModel
        StopWatch.start("getModel");
        DataModel model = new DBModel(hostname, port, username, password, database, collection, newUserMap, newitemMap);

        log.info(String.format("%s (%d users, %d items)", StopWatch.str("getModel"), model.getNumUsers(), model.getNumItems()));

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
        log.info(String.format("%s (%d users, %d items)", StopWatch.str("preprocess"), model.getNumUsers(), model.getNumItems()));

        // build Recommendation model
        StopWatch.start("buildRecommender");
        Recommender newRecommender = ((Lynx) config).getBuilder().buildRecommender(model);
        log.info(StopWatch.str("buildRecommender"));

        // swap in new models
        recommender = newRecommender;
        userMap = newUserMap;
        itemMap = newitemMap;

        // reset rebuild cycle
        counter = 0;
        lastUpdate = new Date();

        log.info(StopWatch.str("totalRebuild"));
    }
}