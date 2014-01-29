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

import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.user.server.exception.UserException;
import com.codenvy.api.user.shared.dto.Attribute;
import com.codenvy.api.user.shared.dto.Profile;
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
    public void create(Profile profile) throws UserException {
        validateProfileUserExists(profile.getUserId());
        collection.save(profileToDBObject(profile));
    }

    @Override
    public void update(Profile profile) throws UserException {
        DBObject query = new BasicDBObject("_id", profile.getId());
        DBObject res = collection.findOne(query);
        if (res == null) {
            throw new UserException("Specified user profile does not exists.");
        }
        validateProfileUserExists(profile.getUserId());
        collection.update(query, profileToDBObject(profile));
    }

    @Override
    public void remove(String id) throws UserException {
        DBObject query = new BasicDBObject("_id", id);
        collection.remove(query);
    }

    @Override
    public Profile getById(String id) throws UserException {
        DBObject query = new BasicDBObject("_id", id);
        DBObject res = collection.findOne(query);
        if (res == null) {
           throw new  UserException("Specified user profile does not exists.");
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
     * @param userId
     * @throws UserException
     */
    private void validateProfileUserExists(String userId) throws UserException{
        userDao.getById(userId);
    }

    /**
     * Convert Profile to Database ready-to-use object,
     * @param profile
     * @return DBObject
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

        profileDatabuilder.add("_id", profile.getId())
                          .add("userid", profile.getUserId())
                          .add("attributes", attrs);
        return profileDatabuilder.get();
    }
}
