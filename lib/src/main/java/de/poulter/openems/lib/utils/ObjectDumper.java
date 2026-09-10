package de.poulter.openems.lib.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectDumper {

    private static final Logger log = LoggerFactory.getLogger(ObjectDumper.class);

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ObjectDumper() {}

    public static <T> String objectToJsonString(T object) {
        if (object == null) {
            return null;
        }

        try {
            return GSON.toJson(object);

        } catch (Exception ex) {
            log.error("Could not convert object to json string.", ex);
            return object.toString();
        }
    }
}
