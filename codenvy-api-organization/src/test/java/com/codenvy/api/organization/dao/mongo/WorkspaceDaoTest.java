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


import com.codenvy.api.organization.dao.ldap.UserDaoImpl;
import com.codenvy.api.user.exception.UserException;
import com.codenvy.api.workspace.exception.WorkspaceException;
import com.codenvy.api.workspace.shared.dto.Attribute;
import com.codenvy.api.workspace.server.dao.WorkspaceDao;
import com.codenvy.api.workspace.shared.dto.Workspace;
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
public class WorkspaceDaoTest extends BaseDaoTest {

    @Mock
    private UserDaoImpl userDao;
    private static final String COLL_NAME = "workspaces";
    WorkspaceDao workspaceDao;

    private static final String WORKSPACE_ID = "workspace123abc456def";
    private static final String WORKSPACE_NAME = "ws1";

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp(COLL_NAME);
        workspaceDao = new WorkspaceDaoImpl(userDao, db, COLL_NAME);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        super.tearDown();
    }


    @Test
    public void mustSaveWorkspace() throws Exception {

        List<Attribute> attributes = new ArrayList<>();
        attributes.add(DtoFactory.getInstance().createDto(Attribute.class).withName("attr1").withValue("value1")
                                 .withDescription("description1"));
        attributes.add(DtoFactory.getInstance().createDto(Attribute.class).withName("attr2").withValue("value2")
                                 .withDescription("description2"));
        attributes.add(DtoFactory.getInstance().createDto(Attribute.class).withName("attr3").withValue("value3")
                                 .withDescription("description3"));
        Workspace workspace = DtoFactory.getInstance().createDto(Workspace.class).withId(WORKSPACE_ID).withName(WORKSPACE_NAME)
                                    .withAttributes(attributes).withTemporary(true);

        // main invoke
        workspaceDao.create(workspace);

        DBObject query = new BasicDBObject("_id", WORKSPACE_ID);
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
        Workspace result =
                DtoFactory.getInstance().createDto(Workspace.class).withId(WORKSPACE_ID)
                          .withName((String)res.get("name"))
                          .withAttributes(resultAttributes)
                          .withTemporary((boolean)res.get("temporary"));

        assertEquals(workspace, result);
    }

    @Test
    public void mustNotSaveWorkspaceIfSameNameExist() throws Exception {
        DBObject obj = new BasicDBObject("_id", WORKSPACE_ID).append("name", WORKSPACE_NAME);
        collection.insert(obj);

        Workspace workspace = DtoFactory.getInstance().createDto(Workspace.class).withId(WORKSPACE_ID).withName(
                WORKSPACE_NAME).withTemporary(true);
        try {
            workspaceDao.create(workspace);
            fail("Workspace with same name exists, but another is created.");
        } catch (WorkspaceException e) {
            // OK
        }
    }


    @Test
    public void mustRemoveWorkspace() throws Exception {
        DBObject obj = new BasicDBObject("_id", WORKSPACE_ID).append("name", WORKSPACE_NAME);
        collection.insert(obj);

        // main invoke
        workspaceDao.remove(WORKSPACE_ID);

        DBObject query = new BasicDBObject("_id", WORKSPACE_ID);
        assertNull(collection.findOne(query));
    }

}
