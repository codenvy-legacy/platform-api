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

import com.codenvy.api.user.server.dao.MemberDao;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.exception.MembershipException;
import com.codenvy.api.user.server.exception.UserException;
import com.codenvy.api.user.shared.dto.Member;
import com.codenvy.api.workspace.server.dao.WorkspaceDao;
import com.codenvy.api.workspace.server.exception.WorkspaceException;
import com.codenvy.dto.server.DtoFactory;
import com.mongodb.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of MemberDAO based on MongoDB storage.
 *
 */
public class MemberDaoImpl implements MemberDao {

    protected static final String DB_COLLECTION = "organization.storage.db.member.collection";

    DBCollection collection;

    UserDao userDao;

    WorkspaceDao workspaceDao;

    @Inject
    public MemberDaoImpl(UserDao userDao, WorkspaceDao workspaceDao, DB db,
                         @Named(DB_COLLECTION) String collectionName) {
        collection = db.getCollection(collectionName);
        this.userDao = userDao;
        this.workspaceDao = workspaceDao;
    }

    @Override
    public void create(Member member) throws MembershipException {
        validateSubjectsExists(member.getUserId(), member.getWorkspaceId());
        try {
            collection.save(toDBObject(member));
        } catch (MongoException me) {
            throw new MembershipException(me.getMessage(), me);
        }
    }

    @Override
    public void update(Member member) throws MembershipException {
        validateSubjectsExists(member.getUserId(), member.getWorkspaceId());
        DBObject query = new BasicDBObject();
        query.put("workspaceid", member.getWorkspaceId());
        query.put("userid", member.getUserId());
        try {
            collection.update(query, toDBObject(member));
        } catch (MongoException me) {
            throw new MembershipException(me.getMessage(), me);
        }
    }

    @Override
    public List<Member> getWorkspaceMembers(String wsId) throws MembershipException {
        List<Member> result = new ArrayList<>();
        DBObject query = new BasicDBObject("workspaceid", wsId);
        try (DBCursor cursor = collection.find(query)) {
            for (DBObject one : cursor) {
                List<String> roles = new ArrayList<>();
                BasicDBList dbRoles = (BasicDBList)one.get("roles");
                for (Object obj : dbRoles) {
                    BasicDBObject dbrole = (BasicDBObject)obj;
                    roles.add((String)dbrole.get("name"));
                }
                result.add(DtoFactory.getInstance().createDto(Member.class).withUserId((String)one.get("userid"))
                                     .withWorkspaceId(wsId).withRoles(roles));
            }
        } catch (MongoException me) {
            throw new MembershipException(me.getMessage(), me);
        }
        return result;
    }

    @Override
    public List<Member> getUserRelationships(String userId) throws MembershipException {
        List<Member> result = new ArrayList<>();
        DBObject query = new BasicDBObject("userid", userId);
        try (DBCursor cursor = collection.find(query)) {
            for (DBObject one : cursor) {
                List<String> roles = new ArrayList<>();
                BasicDBList dbRoles = (BasicDBList)one.get("roles");
                for (Object obj : dbRoles) {
                    BasicDBObject dbrole = (BasicDBObject)obj;
                    roles.add((String)dbrole.get("name"));
                }
                result.add(DtoFactory.getInstance().createDto(Member.class).withUserId(userId)
                                     .withWorkspaceId((String)one.get("workspaceid")).withRoles(roles));
            }
        } catch (MongoException me) {
            throw new MembershipException(me.getMessage(), me);
        }
        return result;
    }

    @Override
    public void remove(Member member) throws MembershipException {
        DBObject query = new BasicDBObject();
        query.put("workspaceid", member.getWorkspaceId());
        query.put("userid", member.getUserId());
        try {
            collection.remove(query);
        } catch (MongoException me) {
            throw new MembershipException(me.getMessage(), me);
        }
    }


    /**
     * Convert Member to Database ready-to-use object,
     * @param member
     * @return DBObject
     */
    private DBObject toDBObject(Member member) {
        BasicDBObjectBuilder memberDatabuilder = new BasicDBObjectBuilder();

        //Prepare attributes list
        List<BasicDBObject> roles = new ArrayList<>();
        for (String role  : member.getRoles()) {
            BasicDBObject one = new BasicDBObject();
            one.put("name", role);
            one.put("description", "");
            roles.add(one);
        }

        memberDatabuilder.add("userid", member.getUserId())
                         .add("workspaceid", member.getWorkspaceId())
                         .add("roles", roles);
        return memberDatabuilder.get();
    }


    void validateSubjectsExists(String userId, String workspaceId) throws MembershipException {
        try {
            if (userDao.getById(userId) == null)
                throw new MembershipException("User specified in membership is not found.");
            if (workspaceDao.getById(workspaceId) == null)
                throw new MembershipException("User specified in membership is not found.");
        } catch (UserException | WorkspaceException e) {
            throw new MembershipException(e.getMessage());
        }
    }
}
