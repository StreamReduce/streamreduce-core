package com.streamreduce.util;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Convenience class used to make JSONObject creation a little more fluent.
 *
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 7/10/12 10:57 AM</p>
 */
public class JSONObjectBuilder {

    private JSONObject json;

    public JSONObjectBuilder() {
        json = new JSONObject();
    }

    public JSONObjectBuilder add(String key, Object value) {
        json.put(key, value);
        return this;
    }

    /**
     * Creates a new JSONArray with <code>values</code>, adding it to the JSONObject as <code>key</code>.
     *
     * @param key key
     * @param values values added to the array; may be null
     * @return JSONObjectBuilder
     */
    public JSONObjectBuilder array(String key, Object... values) {
        JSONArray array = new JSONArray();
        if (values != null) {
            for (Object o : values) {
                array.add(o);
            }
        }
        json.put(key, array);
        return this;
    }

    /**
     * Appends values to an existing JSONArray with <code>values</code>.
     *
     * @param key key
     * @param values values added to the array; may be null
     * @return JSONObjectBuilder
     */
    public JSONObjectBuilder append(String key, Object... values) {
        if (!json.containsKey(key)) {
            array(key, values);
        }
        else {
            JSONArray array = json.getJSONArray(key);
            for (Object value : values) {
                array.add(value);
            }
        }
        return this;
    }

    public JSONObject build() {
        return json;
    }

}
