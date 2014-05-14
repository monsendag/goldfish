package edu.ntnu.idi.goldfish;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class StopWatch {
	
	protected static HashMap<String, Long> starts = new HashMap<String,Long>();
	protected static HashMap<String, Long> timings = new HashMap<String,Long>();
	
	private static String id(String name) {
        return ""+Thread.currentThread().hashCode()+name;
    }

	public static void start(String name) {
		timings.remove(id(name));
		starts.put(id(name), System.currentTimeMillis());
	}
	
	public static void stop(String name) {
		if(!starts.containsKey(id(name))) {
			System.err.format("have no start timing for %s\n", name);
			return;
		}
		if(!timings.containsKey(id(name))) {
			timings.put(id(name), System.currentTimeMillis() - starts.get(id(name)));
		}
	}
	
	public static String getString(long time) {
		return String.format("%d min, %d sec (%d ms)",
			    TimeUnit.MILLISECONDS.toMinutes(time),
			    TimeUnit.MILLISECONDS.toSeconds(time) - 
			    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time)),
			    time
		);
	}
	
	public static String str(String name) {
		stop(name);
		long time = timings.get(id(name));
		return name+": "+getString(time);
	}
	
	public static void print(String name) {
		System.out.format("%s: %s\n", name, str(name));
	}
	
	public static long get(String name) {
		stop(name);
		return timings.get(id(name));
	}
}
