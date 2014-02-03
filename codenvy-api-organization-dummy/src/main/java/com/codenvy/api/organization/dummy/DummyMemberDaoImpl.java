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
package com.codenvy.api.organization.dummy;

import com.codenvy.api.user.server.dao.MemberDao;
import com.codenvy.api.user.server.exception.MembershipException;
import com.codenvy.api.user.shared.dto.Member;
import com.codenvy.dto.server.DtoFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DummyMemberDaoImpl implements MemberDao {
    @Override
    public void create(Member member) throws MembershipException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void update(Member member) throws MembershipException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public List<Member> getWorkspaceMembers(String wsId) throws MembershipException {
        List<Member> members = new ArrayList<>();
        members.add(
                DtoFactory.getInstance().createDto(Member.class).withUserId("user1").withWorkspaceId(wsId).withRoles(
                        Arrays.asList("workspace/admin")));
        members.add(
                DtoFactory.getInstance().createDto(Member.class).withUserId("user2").withWorkspaceId(wsId).withRoles(
                        Arrays.asList("workspace/user")));

        return members;
    }

    @Override
    public List<Member> getUserRelationships(String userId) throws MembershipException {
        List<Member> members = new ArrayList<>();
        members.add(
                DtoFactory.getInstance().createDto(Member.class).withUserId(userId).withWorkspaceId("ws1").withRoles(
                        Arrays.asList("workspace/admin")));
        members.add(
                DtoFactory.getInstance().createDto(Member.class).withUserId(userId).withWorkspaceId("ws2").withRoles(
                        Arrays.asList("workspace/user")));

        return members;

    }

    @Override
    public void remove(Member member) {
        throw new RuntimeException("Not implemented");
    }
}
