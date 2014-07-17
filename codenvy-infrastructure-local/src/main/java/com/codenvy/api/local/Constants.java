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

import com.codenvy.api.auth.shared.dto.Token;
import com.codenvy.api.workspace.server.dao.Member;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.api.workspace.server.dao.Workspace;
import com.codenvy.dto.server.DtoFactory;

import java.util.Arrays;

/**
 * @author gazarenkov
 */
public final class Constants {

    public static final User USER = DtoFactory.getInstance().createDto(User.class)
                                              .withId("codenvy")
                                              .withEmail("codenvy@codenvy.com")
                                              .withPassword("pass1");

    public static final Token TOKEN = DtoFactory.getInstance().createDto(Token.class).withValue("123123");

    public static final Workspace WORKSPACE = new Workspace().withId("1q2w3e")
                                                             .withName("default")
                                                             .withTemporary(false);

    public static final Member MEMBER = new Member().withUserId("codenvy")
                                                    .withWorkspaceId(WORKSPACE.getId())
                                                    .withRoles(Arrays.asList("workspace/admin", "workspace/developer"));

    private Constants() {
    }
}
