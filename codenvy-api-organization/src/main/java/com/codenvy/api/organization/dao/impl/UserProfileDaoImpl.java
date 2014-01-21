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
package com.codenvy.api.organization.dao.impl;

import com.codenvy.api.organization.dao.UserProfileDao;
import com.codenvy.api.organization.exception.OrganizationServiceException;
import com.codenvy.api.organization.shared.dto.Profile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;

/**
 * User Profile DAO implementation based on MongoDB storage.
 *
 */

public class UserProfileDaoImpl implements UserProfileDao {

    DBCollection factories;

    public MongoDBFactoryStore(String host, int port, String dbName, String collectionName, String username,
                               String password) {
        MongoClient mongoClient;
        DB db;
        if (dbName == null || dbName.isEmpty() || collectionName == null || collectionName.isEmpty()) {
            throw new RuntimeException("Parameters 'database' and 'collection' can't be null or empty.");
        }

        try {
            mongoClient = new MongoClient(host, port);
            db = mongoClient.getDB(dbName);

            if (username != null && password != null) {
                if (!db.authenticate(username, password.toCharArray()))
                    throw new RuntimeException("Wrong MongoDB credentians, authentication failed.");

            }
            factories = db.getCollection(collectionName);

        } catch (UnknownHostException e) {
            throw new RuntimeException("Can't connect to MongoDB.");
        }
    }

    public MongoDBFactoryStore(MongoDbConfiguration dbConf) {
        this(dbConf.getHost(), dbConf.getPort(), dbConf.getDatabase(), dbConf.getCollectionname(), dbConf.getUsername(),
             dbConf.getPassword());
    }


    @Override
    public void create(Profile profile) throws OrganizationServiceException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void update(Profile profile) throws OrganizationServiceException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void remove(String id) throws OrganizationServiceException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Profile getById(String id) throws OrganizationServiceException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
