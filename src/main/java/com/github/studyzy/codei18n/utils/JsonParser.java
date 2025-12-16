package com.github.studyzy.codei18n.utils;

import com.github.studyzy.codei18n.models.CliResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.intellij.openapi.diagnostic.Logger;

public class JsonParser {
    private static final Logger LOG = Logger.getInstance(JsonParser.class);
    private static final Gson gson = new Gson();

    public static CliResponse parseCliResponse(String json) {
        try {
            return gson.fromJson(json, CliResponse.class);
        } catch (JsonSyntaxException e) {
            LOG.error("Failed to parse CLI response", e);
            return null;
        }
    }
}
