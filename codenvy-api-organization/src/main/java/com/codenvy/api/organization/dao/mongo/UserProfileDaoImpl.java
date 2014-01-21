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

import com.codenvy.api.organization.dao.UserProfileDao;
import com.codenvy.api.organization.exception.OrganizationServiceException;
import com.codenvy.api.organization.shared.dto.Profile;
import com.mongodb.DB;
import com.mongodb.DBCollection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * User Profile DAO implementation based on MongoDB storage.
 *
 */

@Singleton
public class UserProfileDaoImpl implements UserProfileDao    {


    protected static final  String DB_COLLECTION = "organization.storage.db.profile.collection";

    DBCollection collection;

    @Inject
    public UserProfileDaoImpl(DB db, @Named(DB_COLLECTION)String collectionName) {
        collection = db.getCollection(collectionName);
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
