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

import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.ConflictException;
import com.codenvy.api.factory.dto.Action;
import com.codenvy.api.factory.dto.Actions;
import com.codenvy.api.factory.dto.Factory;
import com.codenvy.api.factory.dto.Ide;
import com.codenvy.api.factory.dto.OnAppClosed;
import com.codenvy.api.factory.dto.OnAppLoaded;
import com.codenvy.api.factory.dto.OnProjectOpened;
import com.codenvy.api.factory.dto.Part;
import com.codenvy.api.factory.dto.WelcomePage;
import com.codenvy.api.vfs.shared.dto.ReplacementSet;
import com.codenvy.api.vfs.shared.dto.Variable;
import com.codenvy.dto.server.DtoFactory;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

/**
 * Convert 2.0 actions to 2.1 format.
 *
 * @author Sergii Kabashniuk
 * @author Sergii Leschenko
 */
public class ActionsConverter implements LegacyConverter {
    private final DtoFactory dto = DtoFactory.getInstance();

    @Override
    public void convert(Factory factory) throws ApiException {
        if (factory.getActions() == null) {
            //nothing to convert
            return;
        }

        if (factory.getIde() != null) {
            throw new ConflictException("Factory contains both 2.0 and 2.1 actions");
        }

        factory.setIde(dto.createDto(Ide.class));

        Actions actions = factory.getActions();

        final WelcomePage welcomePage = actions.getWelcome();
        if (welcomePage != null) {
            addToOnProjectOpened(factory,
                                 null,
                                 singletonList(dto.createDto(Part.class)
                                                  .withId("welcomePanel")
                                                  .withProperties(ImmutableMap.<String, String>builder()
                                                                              .put("authenticatedTitle",
                                                                                   welcomePage.getAuthenticated()
                                                                                              .getTitle())
                                                                              .put("authenticatedIconUrl",
                                                                                   welcomePage.getAuthenticated()
                                                                                              .getIconurl())
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
                                                                              .build())));
        }

        final String openFile = actions.getOpenFile();
        if (openFile != null) {
            addToOnProjectOpened(factory,
                                 singletonList(dto.createDto(Action.class)
                                                  .withId("openfile")
                                                  .withProperties(singletonMap("file", openFile))),
                                 null);
        }

        final List<ReplacementSet> replacement = actions.getFindReplace();
        if (replacement != null) {
            List<Action> replacementActions = new ArrayList<>();
            for (ReplacementSet replacementSet : replacement) {
                for (String file : replacementSet.getFiles()) {
                    for (Variable variable : replacementSet.getEntries()) {
                        replacementActions.add(dto.createDto(Action.class)
                                                  .withId("findReplace")
                                                  .withProperties(ImmutableMap.of(
                                                          "in", file,
                                                          "find", variable.getFind(),
                                                          "replace", variable.getReplace())));
                    }
                }
            }
            addToOnProjectOpened(factory, replacementActions, null);
        }

        final Boolean warnOnClose = actions.getWarnOnClose();
        if (warnOnClose != null && warnOnClose) {
            addToOnAppClosed(factory, singletonList(dto.createDto(Action.class).withId("warnonclose")), null);
        }

        factory.setActions(null);
    }

    private void addToOnAppClosed(Factory factory, List<Action> actions, List<Part> parts) {
        OnAppClosed onAppClosed = factory.getIde().getOnAppClosed();
        if (onAppClosed == null) {
            onAppClosed = dto.createDto(OnAppClosed.class);
            factory.getIde().setOnAppClosed(onAppClosed);
        }
        if (actions != null) {
            List<Action> currentActions = onAppClosed.getActions();
            if (currentActions == null) {
                currentActions = new ArrayList<>();
                onAppClosed.setActions(currentActions);
            }
            currentActions.addAll(actions);
        }
        if (parts != null) {
            List<Part> currentParts = onAppClosed.getParts();
            if (currentParts == null) {
                currentParts = new ArrayList<>();
                onAppClosed.setParts(currentParts);
            }
            currentParts.addAll(parts);
        }
    }

    private void addToOnProjectOpened(Factory factory, List<Action> actions, List<Part> parts) {
        OnProjectOpened onProjectOpened = factory.getIde().getOnProjectOpened();
        if (onProjectOpened == null) {
            onProjectOpened = dto.createDto(OnProjectOpened.class);
            factory.getIde().setOnProjectOpened(onProjectOpened);
        }

        if (actions != null) {
            List<Action> currentActions = onProjectOpened.getActions();
            if (currentActions == null) {
                currentActions = new ArrayList<>();
                onProjectOpened.setActions(currentActions);
            }
            currentActions.addAll(actions);
        }
        if (parts != null) {
            List<Part> currentParts = onProjectOpened.getParts();
            if (currentParts == null) {
                currentParts = new ArrayList<>();
                onProjectOpened.setParts(currentParts);
            }
            currentParts.addAll(parts);
        }
    }
}
