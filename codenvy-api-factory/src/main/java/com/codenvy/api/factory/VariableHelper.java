package com.codenvy.api.factory;

import com.codenvy.commons.json.JsonParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static com.codenvy.commons.json.JsonHelper.fromJson;

/** Helper class to make instance of Variables list from JSON string. */
public class VariableHelper {

    private static final Logger LOG = LoggerFactory.getLogger(VariableHelper.class);

    public static List<Variable> getVariables(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }

        try {
            return Arrays.asList(fromJson(json, Variable[].class, null));
        } catch (JsonParseException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }

        return null;
    }
}
