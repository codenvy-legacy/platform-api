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

import com.codenvy.api.organization.dao.MemberDao;
import com.codenvy.api.organization.dao.UserDao;
import com.codenvy.api.organization.dao.WorkspaceDao;
import com.codenvy.api.organization.exception.ItemNotFoundException;
import com.codenvy.api.organization.exception.OrganizationServiceException;
import com.codenvy.api.organization.shared.dto.Attribute;
import com.codenvy.api.organization.shared.dto.Member;
import com.codenvy.api.organization.shared.dto.Workspace;
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
    public MemberDaoImpl(UserDao userDao, WorkspaceDao workspaceDao,  DB db, @Named(DB_COLLECTION) String collectionName) {
        collection = db.getCollection(collectionName);
        this.userDao = userDao;
        this.workspaceDao = workspaceDao;
    }

    @Override
    public void create(Member member) throws OrganizationServiceException {
        validateSubjectsExists(member.getUserId(), member.getWorkspaceId());
        collection.save(memberToDBObject(member));
    }

    @Override
    public void update(Member member) throws OrganizationServiceException {
        validateSubjectsExists(member.getUserId(), member.getWorkspaceId());
        DBObject query = new BasicDBObject();
        query.put("workspaceid", member.getWorkspaceId());
        query.put("userid", member.getUserId());
        collection.update(query, memberToDBObject(member));
    }

    @Override
    public List<Member> getWorkspaceMembers(String wsId) throws OrganizationServiceException {
        List<Member> result = new ArrayList<>();
        DBObject query = new BasicDBObject();
        query.put("workspaceid", wsId);
        for (DBObject one : collection.find(query)) {
            List<String> roles = new ArrayList<>();
            BasicDBList dbRoles = (BasicDBList)one.get("roles");
            for (Object obj : dbRoles) {
                BasicDBObject dbrole = (BasicDBObject)obj;
                roles.add((String)dbrole.get("name"));
            }
            result.add(DtoFactory.getInstance().createDto(Member.class).withUserId((String)one.get("userid"))
                                 .withWorkspaceId(wsId).withRoles(roles));
        }
        return result;
    }

    @Override
    public List<Member> getUserRelationships(String userId) throws OrganizationServiceException {
        List<Member> result = new ArrayList<>();
        DBObject query = new BasicDBObject();
        query.put("userid", userId);
        for (DBObject one : collection.find(query)) {
            List<String> roles = new ArrayList<>();
            BasicDBList dbRoles = (BasicDBList)one.get("roles");
            for (Object obj : dbRoles) {
                BasicDBObject dbrole = (BasicDBObject)obj;
                roles.add((String)dbrole.get("name"));
            }
            result.add(DtoFactory.getInstance().createDto(Member.class).withUserId(userId)
                                 .withWorkspaceId((String)one.get("workspaceid")).withRoles(roles));
        }
        return result;
    }

    @Override
    public void removeWorkspaceMember(String wsId, String userId) {
        DBObject query = new BasicDBObject();
        query.put("workspaceid", wsId);
        query.put("userid", userId);
        collection.remove(query);
    }


    /**
     * Convert Member to Database ready-to-use object,
     * @param member
     * @return DBObject
     */
    private DBObject memberToDBObject (Member member) {
        BasicDBObjectBuilder memberDatabuilder = new BasicDBObjectBuilder();

        //Prepare attributes list
        List<BasicDBObject> roles = new ArrayList<>();
        for (String role  : member.getRoles()) {
            BasicDBObject one = new BasicDBObject();
            one.put("name", role);
            one.put("description", "");
            roles.add(one);
        }

        memberDatabuilder.add("userid", member.getUserId());
        memberDatabuilder.add("workspaceid", member.getWorkspaceId());
        memberDatabuilder.add("roles", roles);
        return memberDatabuilder.get();
    }



    void validateSubjectsExists(String userId, String workspaceId) throws OrganizationServiceException{
       userDao.getById(userId);
       workspaceDao.getById(workspaceId);
    }
}
