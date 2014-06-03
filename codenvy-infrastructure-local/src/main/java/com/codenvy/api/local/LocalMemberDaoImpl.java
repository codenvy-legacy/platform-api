/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.local;

import com.codenvy.api.user.server.dao.MemberDao;
import com.codenvy.api.user.shared.dto.Member;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class LocalMemberDaoImpl implements MemberDao {
    @Override
    public void create(Member member) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void update(Member member) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public List<Member> getWorkspaceMembers(String wsId) {
        List<Member> members = new ArrayList<>();
        members.add(Constants.MEMBER);
        return members;
    }

    @Override
    public List<Member> getUserRelationships(String userId) {
        List<Member> members = new ArrayList<>();
        members.add(Constants.MEMBER);
        return members;

    }

    @Override
    public void remove(Member member) {
        throw new RuntimeException("Not implemented");
    }
}
