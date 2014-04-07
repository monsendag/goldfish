package edu.ntnu.idi.goldfish.configurations;


import org.apache.mahout.cf.taste.impl.eval.GenericRecommenderIRStatsEvaluator;

import java.util.*;

public class Config extends HashMap<String, Object> {

    private static Config config = new Config();


    public Config() {
        super();

        // default parameters

        // irStats params
        this
        .set("name", getClass().getSimpleName())
        .set("showProgress", false)
        .set("getIrStats", false)
        .set("topN", 10)
        .set("relevanceThreshold", GenericRecommenderIRStatsEvaluator.CHOOSE_THRESHOLD)

        .set("trainingPercentage", 0.9) // train/test distribution
        .set("evaluationPercentage", 1.0) // how much of the data set to evaluate
        .set("getRMSE", true)
        .set("getAAD", false)
        .set("getBuildTime", false)
        .set("getRecTime", false)
        .set("getRecTimeIterations", 10)
        ;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String property) {
        if(!containsKey(property)) {
            System.err.println("PROPERTY NOT FOUND!!! "+property);
        }

        return (T) super.get(property);
    }

    public synchronized Config set(String prop, Object val) {
        super.put(prop, val);
        return this;
    }

    public Config remove(String key) {
        super.remove(key);
        return this;
    }

    public static synchronized Class<Config> setGlobal(String prop, Object val) {
        config.put(prop, val);
        return Config.class;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getGlobal(String property) {
        return (T) config.get(property);
    }

    public static boolean containsGlobal(String property) {
        return config.containsKey(property);
    }

}
