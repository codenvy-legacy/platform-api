/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2013] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.local;


import com.codenvy.api.user.shared.dto.Attribute;
import com.codenvy.api.user.shared.dto.Profile;
import com.codenvy.dto.server.DtoFactory;

import org.everrest.core.impl.provider.json.ArrayValue;
import org.everrest.core.impl.provider.json.JsonException;
import org.everrest.core.impl.provider.json.JsonParser;
import org.everrest.core.impl.provider.json.JsonValue;
import org.everrest.core.impl.provider.json.JsonWriter;
import org.everrest.core.impl.provider.json.ObjectValue;
import org.everrest.core.impl.provider.json.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Allows to save the profile data in the file and obtain this data from the file.
 *
 * @author Roman Nikitenko
 */

@Singleton
public class ProfileStorage {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileStorage.class);
    private final String storageFile;

    /**
     * Create ProfileStorage.
     *
     * @param dirPath
     *         path to save the file with profile data
     */
    @Inject
    public ProfileStorage(@Nullable @Named("profile_store_location") String dirPath) {

        if (dirPath == null || dirPath.isEmpty()) {
            storageFile = System.getProperty("java.io.tmpdir") + "/ProfileStorage.json";
        } else {
            storageFile = dirPath + "/ProfileStorage.json";
        }
    }

    /**
     * Gets a Profile.
     * Read profile data from a file, if it exists. Otherwise a default profile.
     *
     * @param id
     *         profile identifier
     * @return profile, which contains id, userId, attributes and preference
     */
    public Profile get(String id) {
        Profile profile = null;
        try {
            File file = new File(storageFile);
            if (file.exists()) {
                JsonParser jsonParser = new JsonParser();
                jsonParser.parse(new InputStreamReader(new FileInputStream(file)));
                JsonValue jsonFromFile = jsonParser.getJsonObject();
                profile = profileFromJson(jsonFromFile);
            } else {
                profile = DtoFactory.getInstance().createDto(Profile.class).withId(id).withUserId("codenvy").withAttributes(
                        Arrays.asList(
                                DtoFactory.getInstance().createDto(Attribute.class).withName("First Name").withValue("Felix")
                                          .withDescription("User's first name")));
                update(profile);
            }
        } catch (JsonException | IOException e) {
            LOG.error("It is not possible to parse file " + storageFile + " or this file not found", e);
        }
        return profile;
    }

    /**
     * Updates already present profile.
     * Profile data stored to the file.
     *
     * @param profile
     *         - POJO representation of profile entity
     */
    public void update(Profile profile) {
        JsonValue profileJson = profileToJson(profile);
        File file = new File(storageFile);
        try {
            FileWriter file1 = new FileWriter(file);
            JsonWriter jsonWriter = new JsonWriter(file1);
            profileJson.writeTo(jsonWriter);
            jsonWriter.close();
            file1.close();
        } catch (IOException | JsonException e) {
            LOG.error("It is not possible to write file " + storageFile, e);
        }
    }

    /**
     * Сonverts the profile data into a Json format
     *
     * @param profile
     *         - POJO representation of profile entity
     * @return profile data in Json format
     */
    private JsonValue profileToJson(Profile profile) {
        JsonValue profileJson = new ObjectValue();
        profileJson.addElement("id", new StringValue(profile.getId()));
        profileJson.addElement("userId", new StringValue(profile.getUserId()));
        profileJson.addElement("attributes", listAttributesToJson(profile));
        profileJson.addElement("preference", preferenceToJson(profile));
        return profileJson;
    }

    /**
     * Gets a Profile from a Json format profile data
     *
     * @param profileJson
     *         profile data in Json format
     * @return POJO representation of profile entity
     */
    private Profile profileFromJson(JsonValue profileJson) {
        String id = profileJson.getElement("id").getStringValue();
        String userId = profileJson.getElement("userId").getStringValue();

        return DtoFactory.getInstance().createDto(Profile.class).withId(id).
                withUserId(userId).
                                 withAttributes(listAttributesFromJson(profileJson)).
                                 withPreferences(preferenceFromJson(profileJson));
    }

    /**
     * Сonverts the preference data of profile into a Json format
     *
     * @param profile
     *         - POJO representation of profile entity
     * @return preference data of profile in Json format
     */
    private JsonValue preferenceToJson(Profile profile) {
        JsonValue preferenceJson = new ObjectValue();
        Map<String, String> preference = profile.getPreferences();
        Iterator<String> keys;
        for (keys = preference.keySet().iterator(); keys.hasNext(); ) {
            String key = keys.next();
            preferenceJson.addElement(key, new StringValue(preference.get(key)));
        }
        return preferenceJson;
    }

    /**
     * Gets a preference data of profile from a Json format
     *
     * @param profileJson
     *         profile data in Json format
     * @return preference data of profile
     */
    private Map<String, String> preferenceFromJson(JsonValue profileJson) {
        Map<String, String> preference = new HashMap<>();
        JsonValue preferenceJson = profileJson.getElement("preference");
        for (Iterator<String> keys = preferenceJson.getKeys(); keys.hasNext(); ) {
            String key = keys.next();
            preference.put(key, preferenceJson.getElement(key).getStringValue());
        }
        return preference;
    }

    /**
     * Сonverts the attributes data of profile into a Json format
     *
     * @param profile
     *         - POJO representation of profile entity
     * @return attributes data of profile in Json format
     */
    private ArrayValue listAttributesToJson(Profile profile) {
        ArrayValue listAttributesJson = new ArrayValue();
        List<Attribute> listAttributes = profile.getAttributes();
        for (Attribute attribute : listAttributes) {
            ObjectValue attributeJson = new ObjectValue();
            attributeJson.addElement("name", new StringValue(attribute.getName()));
            attributeJson.addElement("value", new StringValue(attribute.getValue()));
            attributeJson.addElement("description", new StringValue(attribute.getDescription()));
            listAttributesJson.addElement(attributeJson);
        }
        return listAttributesJson;
    }

    /**
     * Gets a attributes data of profile from a Json format
     *
     * @param profileJson
     *         profile data in Json format
     * @return attributes data of profile
     */
    private List<Attribute> listAttributesFromJson(JsonValue profileJson) {
        List<Attribute> listAttributes = new ArrayList<>();
        JsonValue listAttributesJson = profileJson.getElement("attributes");
        for (Iterator<JsonValue> elements = listAttributesJson.getElements(); elements.hasNext(); ) {
            JsonValue next = elements.next();
            String name = next.getElement("name").getStringValue();
            String value = next.getElement("value").getStringValue();
            String description = next.getElement("description").getStringValue();
            Attribute attribute = DtoFactory.getInstance().createDto(Attribute.class).withName(name).withValue(value)
                                            .withDescription(description);
            listAttributes.add(attribute);
        }
        return listAttributes;
    }
}
