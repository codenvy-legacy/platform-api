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
import java.util.Arrays;

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
                profile =
                        DtoFactory.getInstance().createDtoFromJson(new InputStreamReader(new FileInputStream(file)), Profile.class);

            } else {
                profile = DtoFactory.getInstance().createDto(Profile.class).withId(id).withUserId("codenvy").withAttributes(
                        Arrays.asList(
                                DtoFactory.getInstance().createDto(Attribute.class).withName("First Name").withValue("Felix")
                                          .withDescription("User's first name")));
                update(profile);
            }
        } catch (IOException e) {
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
        String profileJson = DtoFactory.getInstance().toJson(profile);
        try {
            FileWriter fileWriter = new FileWriter(new File(storageFile));
            fileWriter.write(profileJson);
            fileWriter.close();
        } catch (IOException e) {
            LOG.error("It is not possible to write file " + storageFile, e);
        }
    }
}
