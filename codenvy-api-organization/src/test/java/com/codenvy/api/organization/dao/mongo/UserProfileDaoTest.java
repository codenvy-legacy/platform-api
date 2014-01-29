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

import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.organization.dao.ldap.UserDaoImpl;
import com.codenvy.api.user.server.exception.UserException;
import com.codenvy.api.user.shared.dto.Attribute;
import com.codenvy.api.user.shared.dto.Profile;
import com.codenvy.dto.server.DtoFactory;
import com.mongodb.*;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

/**
 *
 */
@Listeners(value = {MockitoTestNGListener.class})
public class UserProfileDaoTest extends BaseDaoTest {
    @Mock
    private UserDaoImpl userDao;
    private static final String COLL_NAME = "profile";
    UserProfileDao profileDao;

    private static final String       PROFILE_ID = "profile123abc456def";
    private static final String       USER_ID    = "user123abc456def";



    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp(COLL_NAME);
        profileDao = new UserProfileDaoImpl(userDao, db, COLL_NAME);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void mustSaveProfile() throws Exception {

        List<Attribute> attributes = new ArrayList<>();
        attributes.add(DtoFactory.getInstance().createDto(Attribute.class).withName("attr1").withValue("value1")
                                 .withDescription("description1"));
        attributes.add(DtoFactory.getInstance().createDto(Attribute.class).withName("attr2").withValue("value2")
                                 .withDescription("description2"));
        attributes.add(DtoFactory.getInstance().createDto(Attribute.class).withName("attr3").withValue("value3")
                                 .withDescription("description3"));
        Profile profile = DtoFactory.getInstance().createDto(Profile.class).withId(PROFILE_ID).withUserId(USER_ID)
                                    .withAttributes(attributes);

        // main invoke
        profileDao.create(profile);


        DBObject query = new BasicDBObject("_id", PROFILE_ID);
        DBObject res = collection.findOne(query);
        if (res == null) {
            fail("Specified user profile does not exists.");
        }

        List<Attribute> resultAttributes = new ArrayList<>();
        BasicDBList dbList = (BasicDBList)res.get("attributes");
        for (Object one : dbList) {
            BasicDBObject obj = (BasicDBObject)one;
            resultAttributes.add(DtoFactory.getInstance().createDto(Attribute.class).withName((String)obj.get("name"))
                                     .withValue((String)obj.get("value"))
                                     .withDescription((String)obj.get("description")));
        }
        Profile result =
                DtoFactory.getInstance().createDto(Profile.class).withId(PROFILE_ID)
                          .withUserId((String)res.get("userid"))
                          .withAttributes(resultAttributes);

        assertEquals(profile, result);
    }

    @Test
    public void mustNotSaveProfileIfUserNotExist() throws Exception {

        List<Attribute> attributes = new ArrayList<>();
        attributes.add(DtoFactory.getInstance().createDto(Attribute.class).withName("attr1").withValue("value1")
                                 .withDescription("description1"));
        attributes.add(DtoFactory.getInstance().createDto(Attribute.class).withName("attr2").withValue("value2")
                                 .withDescription("description2"));
        attributes.add(DtoFactory.getInstance().createDto(Attribute.class).withName("attr3").withValue("value3")
                                 .withDescription("description3"));
        Profile profile = DtoFactory.getInstance().createDto(Profile.class).withId(PROFILE_ID).withUserId(USER_ID)
                                    .withAttributes(attributes);
        Mockito.when(userDao.getById(USER_ID)).thenThrow(UserException.class);

        try {
           profileDao.create(profile);
           fail("User described in profile is not found, but profile is created.");
        } catch (UserException e) {
            // OK
        }
    }

    @Test
    public void mustNotUpdateProfileIfNotExist() throws Exception {

        List<Attribute> attributes = new ArrayList<>();
        attributes.add(DtoFactory.getInstance().createDto(Attribute.class).withName("attr1").withValue("value1")
                                 .withDescription("description1"));
        attributes.add(DtoFactory.getInstance().createDto(Attribute.class).withName("attr2").withValue("value2")
                                 .withDescription("description2"));
        attributes.add(DtoFactory.getInstance().createDto(Attribute.class).withName("attr3").withValue("value3")
                                 .withDescription("description3"));
        Profile profile = DtoFactory.getInstance().createDto(Profile.class).withId(PROFILE_ID).withUserId(USER_ID)
                                    .withAttributes(attributes);

        try {
            profileDao.update(profile);
            fail("Update of non-existing profile prohibited.");
        } catch (UserException e) {
            // OK
        }
    }

    @Test
    public void mustNotUpdateProfileIfUserNotExist() throws Exception {

        List<Attribute> attributes = new ArrayList<>();
        attributes.add(DtoFactory.getInstance().createDto(Attribute.class).withName("attr1").withValue("value1")
                                 .withDescription("description1"));
        attributes.add(DtoFactory.getInstance().createDto(Attribute.class).withName("attr2").withValue("value2")
                                 .withDescription("description2"));
        attributes.add(DtoFactory.getInstance().createDto(Attribute.class).withName("attr3").withValue("value3")
                                 .withDescription("description3"));
        Profile profile = DtoFactory.getInstance().createDto(Profile.class).withId(PROFILE_ID).withUserId(USER_ID)
                                    .withAttributes(attributes);
        Mockito.when(userDao.getById(USER_ID)).thenThrow(UserException.class);

        try {
            profileDao.update(profile);
            fail("User described in profile is not found, but profile is updated.");
        } catch (UserException e) {
            // OK
        }
    }


    @Test
    public void mustRemoveProfile() throws Exception {
        DBObject obj = new BasicDBObject("_id", PROFILE_ID).append("userid", USER_ID);
        collection.insert(obj);

        profileDao.remove(PROFILE_ID);

        DBObject query = new BasicDBObject("_id", PROFILE_ID);
        assertNull(collection.findOne(query));
    }
}
