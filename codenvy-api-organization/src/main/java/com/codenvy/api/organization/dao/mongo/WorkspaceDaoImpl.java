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

import com.codenvy.api.organization.dao.exception.ItemNamingException;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.organization.dao.util.NamingValidator;
import com.codenvy.api.workspace.server.dao.WorkspaceDao;
import com.codenvy.api.workspace.server.exception.WorkspaceException;
import com.codenvy.api.workspace.shared.dto.Attribute;
import com.codenvy.api.workspace.shared.dto.Workspace;
import com.codenvy.dto.server.DtoFactory;
import com.mongodb.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

/**
 * Workspace DAO implementation based on MongoDB storage.
 */
public class WorkspaceDaoImpl implements WorkspaceDao {

    protected static final String DB_COLLECTION = "organization.storage.db.workspace.collection";

    DBCollection collection;

    UserDao userDao;

    @Inject
    public WorkspaceDaoImpl(UserDao userDao, DB db, @Named(DB_COLLECTION) String collectionName) {
        collection = db.getCollection(collectionName);
        this.userDao = userDao;
    }

    @Override
    public void create(Workspace workspace) throws WorkspaceException {
        try {
        NamingValidator.validate(workspace.getName());
        } catch (ItemNamingException e) {
            throw new WorkspaceException(e.getMessage());
        }
        validateWorkspaceNameAvailable(workspace);
        collection.save(toDBObject(workspace));
    }

    @Override
    public void update(Workspace workspace) throws WorkspaceException {
        DBObject query = new BasicDBObject("_id", workspace.getId());
        DBObject res = collection.findOne(query);
        if (res == null) {
            throw new WorkspaceException("Specified workspace does not exists.");
        }
        try {
            NamingValidator.validate(workspace.getName());
        } catch (ItemNamingException e) {
            throw new WorkspaceException(e.getMessage());
        }
        validateWorkspaceNameAvailable(workspace);
        collection.update(query, toDBObject(workspace));
    }

    @Override
    public void remove(String id) throws WorkspaceException {
        DBObject query = new BasicDBObject("_id", id);
        collection.remove(query);
    }

    @Override
    public Workspace getById(String id) throws WorkspaceException {
        DBObject query = new BasicDBObject("_id", id);
        DBObject res = collection.findOne(query);
        if (res == null) {
            return null;
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
                DtoFactory.getInstance().createDto(Workspace.class).withId(id)
                          .withName((String)res.get("name")).withTemporary((boolean)res.get("temporary"))
                          .withAttributes(attributes);

    }

    @Override
    public Workspace getByName(String name) throws WorkspaceException {
        DBObject query = new BasicDBObject("name", name);
        DBObject res = collection.findOne(query);
        if (res == null) {
            return null;
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
                DtoFactory.getInstance().createDto(Workspace.class).withId((String)res.get("_id"))
                          .withName(name).withTemporary((boolean)res.get("temporary"))
                          .withAttributes(attributes);
    }

    /**
     * Convert Workspace to Database ready-to-use object,
     * @param workspace
     * @return DBObject
     */
    private DBObject toDBObject(Workspace workspace) {
        BasicDBObjectBuilder workspaceDatabuilder = new BasicDBObjectBuilder();

        //Prepare attributes list
        List<BasicDBObject> attrs = new ArrayList<>();
        for (Attribute attribute : workspace.getAttributes()) {
            BasicDBObject one = new BasicDBObject();
            one.put("name", attribute.getName());
            one.put("value", attribute.getValue());
            one.put("description", attribute.getDescription());
            attrs.add(one);
        }

        workspaceDatabuilder.add("_id", workspace.getId())
                            .add("name", workspace.getName())
                            .add("temporary", workspace.isTemporary())
                            .add("attributes", attrs);
        return workspaceDatabuilder.get();
    }


    /**
     * Ensure that user given workspace name not already occupied.
     * @param workspace
     * @throws WorkspaceException
     */
    private void validateWorkspaceNameAvailable(Workspace workspace) throws WorkspaceException{
        DBObject query = new BasicDBObject("name", workspace.getName());
        DBObject res = collection.findOne(query);
        if (res != null) {
            throw new WorkspaceException("Workspace with such name already exists.");
        }

    }
}
