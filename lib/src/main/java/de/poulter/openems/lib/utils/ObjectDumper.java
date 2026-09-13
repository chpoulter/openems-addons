package de.poulter.openems.lib.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

public class ObjectDumper {

    private static final Logger log = LoggerFactory.getLogger(ObjectDumper.class);

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
            .enable(tools.jackson.databind.SerializationFeature.INDENT_OUTPUT)
            .build();

    private ObjectDumper() {}

    public static <T> String objectToJsonString(T object) {
        if (object == null) {
            return null;
        }

        try {
            return JSON_MAPPER.writeValueAsString(object);

        } catch (JacksonException ex) {
            log.error("Could not convert object to json string.", ex);
            return null;
        }
    }
}
