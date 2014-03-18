package com.codenvy.api.local;


import com.codenvy.api.user.shared.dto.Attribute;
import com.codenvy.api.user.shared.dto.Profile;
import com.codenvy.dto.server.DtoFactory;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.*;

public class LocalStorage {
    private final String fileName = System.getProperty("java.io.tmpdir") + "/localStorage.json";

    public Profile get(String id) {
        Profile profile = null;
        try {
            InputStreamReader reader = new InputStreamReader(new FileInputStream(new File(fileName)));

            if (reader.ready()) {
                JSONParser parser = new JSONParser();
                JSONObject jsonFromFile = (JSONObject) parser.parse(reader);
                profile = profileFromJson(jsonFromFile);
            } else {
                profile = DtoFactory.getInstance().createDto(Profile.class).withId(id).withUserId("Сodenvy").withAttributes(
                        Arrays.asList(
                                DtoFactory.getInstance().createDto(Attribute.class).withName("First Name").withValue("Felix")
                                        .withDescription("User's first name")));
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return profile;
    }

    public void update(Profile profile) {
        JSONObject profileJson = profileToJson(profile);
        FileWriter file;
        try {
            file = new FileWriter(fileName);
            profileJson.writeJSONString(file);
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private JSONObject profileToJson(Profile profile){
        JSONObject profileJson = new JSONObject();
        profileJson.put("id", profile.getId());
        profileJson.put("userId", profile.getUserId());

        JSONArray listAttributesJson = new JSONArray();
        List<Attribute> listAttributes = profile.getAttributes();
        for (Attribute attribute : listAttributes) {
            JSONObject attributeJson = new JSONObject();
            attributeJson.put("name", attribute.getName());
            attributeJson.put("value", attribute.getValue());
            attributeJson.put("description", attribute.getDescription());
            listAttributesJson.add(attributeJson);
        }
        profileJson.put("attributes", listAttributesJson);

        JSONObject preference = new JSONObject(profile.getPreferences());
        profileJson.put("preference", preference);
        return profileJson;
    }
    private Profile profileFromJson(JSONObject profileJson){
        String id = (String) profileJson.get("id");
        String userId = (String) profileJson.get("userId");

        List<Attribute> listAttributes = new ArrayList<Attribute>();
        JSONArray listAttributesJson = (JSONArray) profileJson.get("attributes");
        for (JSONObject attributeJson : (Iterable<JSONObject>) listAttributesJson) {
            String name = String.valueOf(attributeJson.get("name"));
            String value = String.valueOf(attributeJson.get("value"));
            String description = String.valueOf(attributeJson.get("description"));
            Attribute attribute = DtoFactory.getInstance().createDto(Attribute.class).withName(name).withValue(value)
                    .withDescription(description);
            listAttributes.add(attribute);
        }
        Map preference = (Map) profileJson.get("preference");

        Profile profile = DtoFactory.getInstance().createDto(Profile.class).withId(id).withUserId(userId);
        profile.setAttributes(listAttributes);
        profile.setPreferences(preference);
        return profile;
    }

}
