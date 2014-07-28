/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.local;


import com.codenvy.api.user.server.dao.Profile;
import com.google.gson.Gson;

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
import java.util.HashMap;
import java.util.Map;

/**
 * Allows to save the profile data in the file and obtain this data from the file.
 *
 * @author Roman Nikitenko
 */

@Singleton
public class ProfileStorage {

    private static final Logger LOG  = LoggerFactory.getLogger(ProfileStorage.class);
    private static final Gson   gson = new Gson();
    private final String storageFile;

    /**
     * Create ProfileStorage.
     *
     * @param dirPath
     *         path to save the file with profile data
     */
    @Inject
    public ProfileStorage(@Nullable @Named("profile.store_location") String dirPath) {
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
     * @throws IOException
     *         if an i/o error occurs
     */
    public synchronized Profile get(String id) throws IOException {
        Profile profile = null;
        File file = new File(storageFile);
        InputStreamReader inputStreamReader = null;
        try {
            if (file.exists()) {
                inputStreamReader = new InputStreamReader(new FileInputStream(file));
                profile = gson.fromJson(inputStreamReader, Profile.class);
            } else {
                final Map<String, String> attributes = new HashMap<>(1);
                attributes.put("First Name", "Felix");
                profile = new Profile().withId(id)
                                       .withUserId("codenvy")
                                       .withAttributes(attributes);
                update(profile);
            }
        } catch (IOException e) {
            LOG.error("It is not possible to parse file " + storageFile + " or this file not found", e);
        } finally {
            if (inputStreamReader != null) {
                inputStreamReader.close();
            }
        }
        return profile;
    }

    /**
     * Updates already present profile.
     * Profile data stored to the file.
     *
     * @param profile
     *         POJO representation of profile entity
     * @throws IOException
     *         if an i/o error occurs
     */
    public synchronized void update(Profile profile) throws IOException {
        String profileJson = gson.toJson(profile);
        File file = new File(storageFile);
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file);
            fileWriter.write(profileJson);
        } catch (IOException e) {
            LOG.error("It is not possible to write file " + storageFile, e);
        } finally {
            if (fileWriter != null) {
                fileWriter.close();
            }
        }
    }
}
