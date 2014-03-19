package edu.ntnu.idi.goldfish.preprocessors;

/**
 * Created by dag on 19/03/14.
 */
public class YowModel {

    BasicDBObject query = new BasicDBObject("i", 71);

    cursor = coll.find(query);

    try {
        while(cursor.hasNext()) {
            System.out.println(cursor.next());
        }
    } finally {
        cursor.close();
    }
}
