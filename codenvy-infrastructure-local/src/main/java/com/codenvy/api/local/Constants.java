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
package com.codenvy.api.local;

import com.codenvy.api.auth.shared.dto.Token;
import com.codenvy.api.user.shared.dto.Member;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.api.workspace.shared.dto.Workspace;
import com.codenvy.dto.server.DtoFactory;

import java.util.Arrays;


/**
 * @author gazarenkov
 */
public interface Constants {

    public static final User USER = DtoFactory.getInstance().createDto(User.class)
            .withId("codenvy")
            .withEmail("codenvy@codenvy.com")
            .withPassword("pass1");

    public static final Token TOKEN = DtoFactory.getInstance().createDto(Token.class)
            .withValue("123123");

    public static final Member MEMBER = DtoFactory.getInstance().createDto(Member.class).
            withUserId("codenvy")
            .withWorkspaceId("default")
            .withRoles(Arrays.asList("workspace/admin", "workspace/developer", "system/admin"));

    public static final Workspace WORKSPACE = DtoFactory.getInstance().createDto(Workspace.class)
            .withId("default")
            .withName("default")
            .withTemporary(false);

}
