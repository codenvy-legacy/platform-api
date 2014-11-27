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
package com.codenvy.api.factory.converter;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.ConflictException;
import com.codenvy.api.factory.dto.Action;
import com.codenvy.api.factory.dto.Actions;
import com.codenvy.api.factory.dto.Factory;
import com.codenvy.api.factory.dto.Ide;
import com.codenvy.api.factory.dto.OnAppClosed;
import com.codenvy.api.factory.dto.OnProjectOpened;
import com.codenvy.api.factory.dto.Part;
import com.codenvy.api.factory.dto.WelcomePage;
import com.codenvy.api.vfs.shared.dto.ReplacementSet;
import com.codenvy.api.vfs.shared.dto.Variable;
import com.codenvy.dto.server.DtoFactory;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sergii Kabashniuk
 */
public class ActionsConverter implements LegacyConverter {
    private final DtoFactory dto = DtoFactory.getInstance();

    @Override
    public void convert(Factory factory) throws ApiException {

        Actions actions = factory.getActions();
        if (actions != null && factory.getIde() != null) {
            throw new ConflictException("Factory contains both 2.0 and 2.1 actions");
        }
        final WelcomePage welcomePage = actions.getWelcome();
        Ide ide = null;

        if (welcomePage != null) {
            ide = dto.createDto(Ide.class);
            ide.withOnProjectOpened(dto.createDto(OnProjectOpened.class)
                                       .withParts(singletonList(dto.createDto(Part.class)
                                                                   .withId("welcomepanel")
                                                                   .withProperties(ImmutableMap
                                                                                           .<String,
                                                                                                   String>builder()
                                                                                           .put("authenticatedTitle",
                                                                                                welcomePage.getAuthenticated().getTitle())
                                                                                           .put("authenticatedIconUrl",
                                                                                                welcomePage.getAuthenticated().getIconurl())
                                                                                           .put("authenticatedContentUrl",
                                                                                                welcomePage.getAuthenticated()
                                                                                                           .getContenturl())
                                                                                           .put("nonAuthenticatedTitle",
                                                                                                welcomePage.getNonauthenticated()
                                                                                                           .getTitle())
                                                                                           .put("nonAuthenticatedIconUrl",
                                                                                                welcomePage.getNonauthenticated()
                                                                                                           .getIconurl())
                                                                                           .put("nonAuthenticatedContentUrl",
                                                                                                welcomePage.getNonauthenticated()
                                                                                                           .getContenturl())


                                                                                           .build()))));
        }

        final Boolean warnOnClose = actions.getWarnOnClose();
        if (warnOnClose != null) {
            if (ide != null) {
                ide = dto.createDto(Ide.class);
            }
            ide.withOnAppClosed(
                    dto.createDto(OnAppClosed.class)
                       .withActions(
                               singletonList(dto.createDto(Action.class).withId("warnonclose"))));
        }


        final List<ReplacementSet> replacement = actions.getFindReplace();
        if (replacement != null) {
            if (ide != null) {
                ide = dto.createDto(Ide.class);
            }
            List<Action> replacementActions = new ArrayList<>();
            for (ReplacementSet replacementSet : replacement) {
                for (String file : replacementSet.getFiles()) {
                    for (Variable variable : replacementSet.getEntries()) {
                        replacementActions.add(dto.createDto(Action.class).withId("findReplace").withProperties(
                                ImmutableMap.of(
                                        "in",
                                        file,
                                        "find", variable.getFind(),
                                        "replace", variable.getReplace()
                                               )));
                    }
                }

            }
            ide.withOnProjectOpened(dto.createDto(OnProjectOpened.class).withActions(replacementActions));
        }

        final String openFile = actions.getOpenFile();
        if (openFile != null) {
            if (ide != null) {
                ide = dto.createDto(Ide.class);
            }
            OnProjectOpened onProjectOpened =
                    ide.getOnProjectOpened() != null ? ide.getOnProjectOpened() : dto.createDto(OnProjectOpened.class);

            List<Action> onProjectOpenedActions =
                    onProjectOpened.getActions() != null ? new ArrayList<>(onProjectOpened.getActions()) : new ArrayList<Action>();

            onProjectOpenedActions.add(dto.createDto(Action.class)
                                          .withId("openfile")
                                          .withProperties(singletonMap("file", openFile)));

        }


    }
}
