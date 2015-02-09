/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.factory;

import com.codenvy.api.account.server.dao.AccountDao;
import com.codenvy.api.account.server.dao.Member;
import com.codenvy.api.account.server.dao.Subscription;
import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.factory.dto.Action;
import com.codenvy.api.factory.dto.Factory;
import com.codenvy.api.factory.dto.OnAppLoaded;
import com.codenvy.api.factory.dto.OnProjectOpened;
import com.codenvy.api.factory.dto.Policies;
import com.codenvy.api.factory.dto.WelcomePage;
import com.codenvy.api.factory.dto.Workspace;
import com.codenvy.api.user.server.dao.Profile;
import com.codenvy.api.user.server.dao.User;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.codenvy.api.factory.FactoryConstants.ILLEGAL_REQUIRE_AUTHENTICATION_FOR_NAMED_WORKSPACE_MESSAGE;
import static com.codenvy.api.factory.FactoryConstants.INVALID_FIND_REPLACE_ACTION;
import static com.codenvy.api.factory.FactoryConstants.INVALID_OPENFILE_ACTION;
import static com.codenvy.api.factory.FactoryConstants.INVALID_WELCOME_PAGE_ACTION;
import static com.codenvy.api.factory.FactoryConstants.PARAMETRIZED_ILLEGAL_ACCOUNTID_PARAMETER_MESSAGE;
import static com.codenvy.api.factory.FactoryConstants.PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE;
import static com.codenvy.commons.lang.Strings.emptyToNull;
import static com.codenvy.commons.lang.Strings.isNullOrEmpty;
import static java.lang.String.format;

/**
 * Validates values of factory parameters.
 *
 * @author Alexander Garagatyi
 * @author Valeriy Svydenko
 */
public abstract class FactoryBaseValidator {
    private static final Pattern PROJECT_NAME_VALIDATOR = Pattern.compile("^[\\\\\\w\\\\\\d]+[\\\\\\w\\\\\\d_.-]*$");

    private final boolean onPremises;

    private final AccountDao     accountDao;
    private final UserDao        userDao;
    private final UserProfileDao profileDao;

    public FactoryBaseValidator(AccountDao accountDao,
                                UserDao userDao,
                                UserProfileDao profileDao,
                                boolean onPremises) {
        this.accountDao = accountDao;
        this.userDao = userDao;
        this.profileDao = profileDao;
        this.onPremises = onPremises;
    }

    /**
     * Validates source parameter of factory.
     * TODO for now validates only git source
     *
     * @param factory
     *         - factory to validate
     * @throws ApiException
     */
    protected void validateSource(Factory factory) throws ApiException {
        String type = factory.getSource().getProject().getType();
        String location = factory.getSource().getProject().getLocation();
        String parameterTypeName = "source.project.type";
        String parameterLocationName = "source.project.location";

        // check that vcs value is correct
        if (!("git".equals(type) || "esbwso2".equals(type))) {
            throw new ConflictException("Parameter '" + parameterTypeName + "' has illegal value.");
        }
        if (isNullOrEmpty(location)) {
            throw new ConflictException(
                    format(FactoryConstants.PARAMETRIZED_ILLEGAL_PARAMETER_VALUE_MESSAGE, parameterLocationName, location));
        } else {
            try {
                URLDecoder.decode(location, "UTF-8");
            } catch (IllegalArgumentException | UnsupportedEncodingException e) {
                throw new ConflictException(
                        format(FactoryConstants.PARAMETRIZED_ILLEGAL_PARAMETER_VALUE_MESSAGE, parameterLocationName, location));
            }
        }
    }

    protected void validateProjectName(Factory factory) throws ApiException {
        // validate project name
        String projectName = null;
        switch (factory.getV()) {
            case "2.0":
            case "2.1":
                if (null != factory.getProject()) {
                    projectName = factory.getProject().getName();
                }
                break;
            default:
                // do nothing
        }
        if (null != projectName && !PROJECT_NAME_VALIDATOR.matcher(projectName).matches()) {
            throw new ConflictException(
                    "Project name must contain only Latin letters, digits or these following special characters -._.");
        }
    }

    protected void validateWorkspace(Factory factory) throws ApiException {
        final Workspace workspace = factory.getWorkspace();
        if (workspace != null && workspace.getType() != null) {
            if (workspace.getType().equals("named")) {
                Policies policies = factory.getPolicies();
                if (policies == null || policies.getRequireAuthentication() == null || !policies.getRequireAuthentication()) {
                    throw new ConflictException(ILLEGAL_REQUIRE_AUTHENTICATION_FOR_NAMED_WORKSPACE_MESSAGE);
                }
            } else if (!workspace.getType().equals("temp")) {
                throw new ConflictException("workspace.type have only two possible values - named or temp");
            }
        }
    }

    protected void validateAccountId(Factory factory) throws ApiException {
        // TODO do we need check if user is temporary?
        String accountId = factory.getCreator() != null ? emptyToNull(factory.getCreator().getAccountId()) : null;
        String userId = factory.getCreator() != null ? factory.getCreator().getUserId() : null;

        if (accountId == null || userId == null) {
            return;
        }

        try {
            User user = userDao.getById(userId);
            Profile profile = profileDao.getById(userId);
            if (profile.getAttributes() != null && "true".equals(profile.getAttributes().get("temporary"))) {
                throw new ConflictException("Current user is not allowed for using this method.");
            }

            boolean isOwner = false;
            List<Member> members = accountDao.getMembers(accountId);
            if (members.isEmpty()) {
                throw new ConflictException(format(PARAMETRIZED_ILLEGAL_ACCOUNTID_PARAMETER_MESSAGE, accountId));
            }
            for (Member accountMember : members) {
                if (accountMember.getUserId().equals(user.getId()) && accountMember.getRoles().contains("account/owner")) {
                    isOwner = true;
                    break;
                }
            }
            if (!isOwner) {
                throw new ConflictException("You are not authorized to use this accountId.");
            }
        } catch (NotFoundException | ServerException e) {
            throw new ConflictException("You are not authorized to use this accountId.");
        }
    }

    protected void validateTrackedFactoryAndParams(Factory factory) throws ApiException {
        if (onPremises) {
            return;
        }

        // validate tracked parameters
        String accountId = factory.getCreator() != null ? emptyToNull(factory.getCreator().getAccountId()) : null;

        if (accountId != null) {
            try {
                List<Subscription> subscriptions = accountDao.getActiveSubscriptions(accountId, "Factory");
                boolean isTracked = false;
                for (Subscription one : subscriptions) {
                    if ("Tracked".equalsIgnoreCase(one.getProperties().get("Package"))) {
                        isTracked = true;
                        break;
                    }
                }
                if (!isTracked) {
                    throw new ConflictException(format(PARAMETRIZED_ILLEGAL_ACCOUNTID_PARAMETER_MESSAGE, accountId));
                }
            } catch (NotFoundException | ServerException | NumberFormatException e) {
                throw new ConflictException(format(PARAMETRIZED_ILLEGAL_ACCOUNTID_PARAMETER_MESSAGE, accountId));
            }
        }

        final Policies policies = factory.getPolicies();
        if (policies != null && accountId == null) {
            Long validSince = policies.getValidSince();
            Long validUntil = policies.getValidUntil();

            if (validSince != null && validSince > 0) {
                throw new ConflictException(format(PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, null, "policies.validSince"));
            }

            if (validUntil != null && validUntil > 0) {
                throw new ConflictException(format(PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, null, "policies.validUntil"));
            }

            if (policies.getRefererHostname() != null && !policies.getRefererHostname().isEmpty()) {
                throw new ConflictException(format(PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, null, "policies.refererHostname"));
            }
        }

        if ("2.0".equals(factory.getV())) {
            WelcomePage welcomePage = null;
            if (factory.getActions() != null) {
                welcomePage = factory.getActions().getWelcome();
            }

            if (null != welcomePage && null == accountId) {
                throw new ConflictException(format(PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, null, "actions.welcome"));
            }
        } else {//Version 2.1
            if (factory.getIde() != null) {
                if (factory.getIde().getOnAppLoaded() != null && factory.getIde().getOnAppLoaded().getActions() != null) {
                    List<Action> onLoadedActions = factory.getIde().getOnAppLoaded().getActions();
                    for (Action onLoadedAction : onLoadedActions) {
                        if ("openWelcomePage".equals(onLoadedAction.getId()) && null == accountId) {
                            throw new ConflictException(format(PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, null,
                                                               "ide.onAppLoaded.actions.[%index%].id=openWelcomePage"));
                        }
                    }
                }
            }
        }
    }

    protected void validateCurrentTimeBetweenSinceUntil(Factory factory) throws ConflictException {
        final Policies policies = factory.getPolicies();

        if (policies == null) {
            return;
        }

        Long validSince = policies.getValidSince();
        Long validUntil = policies.getValidUntil();

        if (validSince != null && validSince != 0) {
            if (new Date().before(new Date(validSince))) {
                throw new ConflictException(FactoryConstants.ILLEGAL_FACTORY_BY_VALIDSINCE_MESSAGE);
            }
        }

        if (validUntil != null && validUntil != 0) {
            if (new Date().after(new Date(validUntil))) {
                throw new ConflictException(FactoryConstants.ILLEGAL_FACTORY_BY_VALIDUNTIL_MESSAGE);
            }
        }
    }

    protected void validateCurrentTimeBeforeSinceUntil(Factory factory) throws ConflictException {
        final Policies policies = factory.getPolicies();
        if (policies == null) {
            return;
        }

        Long validSince = policies.getValidSince();
        Long validUntil = policies.getValidUntil();

        if (validSince != null && validSince != 0 && validUntil != null && validUntil != 0 && validSince >= validUntil) {
            throw new ConflictException(FactoryConstants.INVALID_VALIDSINCEUNTIL_MESSAGE);
        }

        if (validSince != null && validSince != 0 && new Date().after(new Date(validSince))) {
            throw new ConflictException(FactoryConstants.INVALID_VALIDSINCE_MESSAGE);
        }

        if (validUntil != null && validUntil != 0 && new Date().after(new Date(validUntil))) {
            throw new ConflictException(FactoryConstants.INVALID_VALIDUNTIL_MESSAGE);
        }
    }


    protected void validateProjectActions(Factory factory) throws ConflictException {
        if (!"2.1".equals(factory.getV()) || factory.getIde() == null) {
            return;
        }

        List<Action> applicationActions = new ArrayList<>();
        if (factory.getIde().getOnAppClosed() != null) {
            applicationActions.addAll(factory.getIde().getOnAppClosed().getActions());
        }
        if (factory.getIde().getOnAppLoaded() != null) {
            applicationActions.addAll(factory.getIde().getOnAppLoaded().getActions());
        }

        for (Action applicationAction : applicationActions) {
            String id = applicationAction.getId();
            if ("openFile".equals(id) || "findReplace".equals(id)) {
                throw new ConflictException(String.format(FactoryConstants.INVALID_ACTION_SECTION, id));
            }
        }

        final OnAppLoaded onAppLoaded = factory.getIde().getOnAppLoaded();
        if (onAppLoaded != null) {
            final List<Action> actions = onAppLoaded.getActions();
            if (actions != null) {
                for (Action action : actions) {
                    final Map<String, String> properties = action.getProperties();
                    if ("openWelcomePage".equals(action.getId()) && (isNullOrEmpty(properties.get("nonAuthenticatedContentUrl")) ||
                                                                     isNullOrEmpty(properties.get("authenticatedContentUrl")))) {

                        throw new ConflictException(INVALID_WELCOME_PAGE_ACTION);
                    }
                }
            }
        }

        OnProjectOpened onOpened = factory.getIde().getOnProjectOpened();
        if (onOpened != null) {
            List<Action> onProjectOpenedActions = onOpened.getActions();
            for (Action applicationAction : onProjectOpenedActions) {
                String id = applicationAction.getId();
                Map<String, String> properties = applicationAction.getProperties();

                switch (id) {
                    case "openFile":
                        if (isNullOrEmpty(properties.get("file"))) {
                            throw new ConflictException(INVALID_OPENFILE_ACTION);
                        }
                        break;

                    case "findReplace":
                        if (isNullOrEmpty(properties.get("in")) ||
                            isNullOrEmpty(properties.get("find")) ||
                            isNullOrEmpty(properties.get("replace"))) {

                            throw new ConflictException(INVALID_FIND_REPLACE_ACTION);
                        }
                        break;
                }
            }
        }
    }
}
