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

import com.codenvy.api.organization.dao.UserDao;
import com.codenvy.api.organization.dao.UserProfileDao;
import com.codenvy.api.organization.exception.OrganizationServiceException;
import com.codenvy.api.organization.shared.dto.Attribute;
import com.codenvy.api.organization.shared.dto.Profile;
import com.codenvy.dto.server.DtoFactory;
import com.mongodb.*;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * User Profile DAO implementation based on MongoDB storage.
 *
 */

@Singleton
public class UserProfileDaoImpl implements UserProfileDao    {


    protected static final String DB_COLLECTION = "organization.storage.db.profile.collection";

    DBCollection collection;

    UserDao userDao;

    @Inject
    public UserProfileDaoImpl(UserDao userDao, DB db, @Named(DB_COLLECTION) String collectionName) {
        collection = db.getCollection(collectionName);
        this.userDao = userDao;
    }

    @Override
    public void create(Profile profile) throws OrganizationServiceException {
        validateOnUpdate(profile);
        collection.save(profileToDBObject(profile));
    }

    @Override
    public void update(Profile profile) throws OrganizationServiceException {
        DBObject query = new BasicDBObject();
        query.put("_id", profile.getId());
        DBObject res = collection.findOne(query);
        if (res == null) {
            throw new OrganizationServiceException(404, "Specified user profile does not exists.");
        }
        validateOnUpdate(profile);
        collection.update(query, profileToDBObject(profile));
    }

    @Override
    public void remove(String id) throws OrganizationServiceException {
        DBObject query = new BasicDBObject();
        query.put("_id", id);
        collection.remove(query);
    }

    @Override
    public Profile getById(String id) throws OrganizationServiceException {
        DBObject query = new BasicDBObject();
        query.put("_id", id);
        DBObject res = collection.findOne(query);
        if (res == null) {
            throw new OrganizationServiceException(404, "Specified user profile does not exists.");
        }

        List<Attribute> attributes = new ArrayList<>();
        BasicDBList dbList = (BasicDBList)res.get("attributes");
        for (Object one : dbList) {
            BasicDBObject obj = (BasicDBObject)one;
            attributes.add(DtoFactory.getInstance().createDto(Attribute.class).withName((String)obj.get("name"))
                                     .withValue((String)obj.get("value"))
                                     .withDescription((String)obj.get("description")));
        }
        return
                DtoFactory.getInstance().createDto(Profile.class).withId(id)
                                                                 .withUserId((String)res.get("userid"))
                                                                 .withAttributes(attributes);

    }

    /**
     * Ensure that user linked to this profile is already exists.
     * @param profile
     * @throws OrganizationServiceException
     */
    private void validateOnUpdate(Profile profile) throws OrganizationServiceException{
        userDao.getById(profile.getUserId());
    }

    /**
     * Convert Profile to Database ready-to-use object,
     * @param profile
     * @return
     */
    private DBObject profileToDBObject (Profile profile) {
        BasicDBObjectBuilder profileDatabuilder = new BasicDBObjectBuilder();

        //Prepare attributes list
        List<BasicDBObject> attrs = new ArrayList<>();
        for (Attribute attribute : profile.getAttributes()) {
            BasicDBObject one = new BasicDBObject();
            one.put("name", attribute.getName());
            one.put("value", attribute.getValue());
            one.put("description", attribute.getDescription());
            attrs.add(one);
        }

        profileDatabuilder.add("_id", profile.getId());
        profileDatabuilder.add("userid", profile.getUserId());
        profileDatabuilder.add("attributes", attrs);
        return profileDatabuilder.get();
    }
}
