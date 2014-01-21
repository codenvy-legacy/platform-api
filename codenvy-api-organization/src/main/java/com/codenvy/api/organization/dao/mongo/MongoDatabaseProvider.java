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
package com.codenvy.api.organization.dao.mongo;

import com.mongodb.DB;
import com.mongodb.MongoClient;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.net.UnknownHostException;

/**
 * Provides single instance of database to all consumers.
 *
 */

@Singleton
public class MongoDatabaseProvider implements Provider<DB>{

    protected static final String DB_URL      = "organization.storage.db.url";
    protected static final String DB_NAME     = "organization.storage.db.name";
    protected static final String DB_USERNAME = "organization.storage.db.username";
    protected static final String DB_PASSWORD = "organization.storage.db.password";

    @Inject
    @Named(DB_URL)
    String dbUrl;

    @Inject
    @Named(DB_NAME)
    String dbName;

    @Inject
    @Named(DB_USERNAME)
    String username;

    @Inject
    @Named(DB_PASSWORD)
    String password;

    private volatile DB db;

    @Override
    public DB get() {
        if (db == null) {
            synchronized (this) {
                try {
                    if (db == null) {
                        MongoClient mongoClient = new MongoClient(dbUrl);
                        db = mongoClient.getDB(dbName);
                        if (!db.authenticate(username, password.toCharArray()))
                            throw new RuntimeException("Incorrect MongoDB credentials: authentication failed.");
                    }
                } catch (UnknownHostException e) {
                    throw new RuntimeException("Can't connect to MongoDB.");
                }
            }
        }
        return db;
    }
}
