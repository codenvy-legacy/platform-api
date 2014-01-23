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
import com.codenvy.api.organization.dao.WorkspaceDao;
import com.codenvy.api.organization.exception.ItemAlreadyExistException;
import com.codenvy.api.organization.exception.ItemNotFoundException;
import com.codenvy.api.organization.exception.OrganizationServiceException;
import com.codenvy.api.organization.shared.dto.Attribute;
import com.codenvy.api.organization.shared.dto.Workspace;
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
    public void create(Workspace workspace) throws OrganizationServiceException {
        validateWorkspaceNameAvailable(workspace);
        collection.save(workspaceToDBObject(workspace));
    }

    @Override
    public void update(Workspace workspace) throws OrganizationServiceException {
        DBObject query = new BasicDBObject();
        query.put("_id", workspace.getId());
        DBObject res = collection.findOne(query);
        if (res == null) {
            throw new ItemNotFoundException("Specified workspace does not exists.");
        }
        validateWorkspaceNameAvailable(workspace);
        collection.update(query, workspaceToDBObject(workspace));
    }

    @Override
    public void remove(String id) throws OrganizationServiceException {
        DBObject query = new BasicDBObject();
        query.put("_id", id);
        collection.remove(query);
    }

    @Override
    public Workspace getById(String id) throws OrganizationServiceException {
        DBObject query = new BasicDBObject();
        query.put("_id", id);
        DBObject res = collection.findOne(query);
        if (res == null) {
            throw new  ItemNotFoundException("Specified workspace does not exists.");
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
                          .withName((String)res.get("name"))
                          .withAttributes(attributes);

    }

    @Override
    public Workspace getByName(String name) throws OrganizationServiceException {
        DBObject query = new BasicDBObject();
        query.put("name", name);
        DBObject res = collection.findOne(query);
        if (res == null) {
            throw new  ItemNotFoundException("Workspace with such name does not exists.");
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
                          .withName(name)
                          .withAttributes(attributes);
    }

    /**
     * Convert Workspace to Database ready-to-use object,
     * @param workspace
     * @return DBObject
     */
    private DBObject workspaceToDBObject (Workspace workspace) {
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

        workspaceDatabuilder.add("_id", workspace.getId());
        workspaceDatabuilder.add("name", workspace.getName());
        workspaceDatabuilder.add("attributes", attrs);
        return workspaceDatabuilder.get();
    }


    /**
     * Ensure that user given workspace name not already occupied.
     * @param workspace
     * @throws OrganizationServiceException
     */
    private void validateWorkspaceNameAvailable(Workspace workspace) throws OrganizationServiceException{
        DBObject query = new BasicDBObject();
        query.put("name", workspace.getName());
        DBObject res = collection.findOne(query);
        if (res != null) {
            throw new ItemAlreadyExistException("Workspace with such name already exists.");
        }

    }
}
