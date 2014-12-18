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
import com.codenvy.api.factory.dto.OnAppClosed;
import com.codenvy.api.factory.dto.OnProjectOpened;
import com.codenvy.api.factory.dto.Policies;
import com.codenvy.api.factory.dto.WelcomePage;
import com.codenvy.api.user.server.dao.Profile;
import com.codenvy.api.user.server.dao.User;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.commons.lang.Strings;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.codenvy.api.factory.FactoryConstants.INVALID_FIND_REPLACE_ACTION;
import static com.codenvy.api.factory.FactoryConstants.INVALID_OPENFILE_ACTION;
import static com.codenvy.api.factory.FactoryConstants.PARAMETRIZED_ILLEGAL_ORGID_PARAMETER_MESSAGE;
import static com.codenvy.api.factory.FactoryConstants.PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE;
import static java.lang.String.format;

/**
 * Validates values of factory parameters.
 *
 * @author Alexander Garagatyi
 * @author Valeriy Svydenko
 */
public abstract class FactoryUrlBaseValidator {
    private static final Pattern PROJECT_NAME_VALIDATOR = Pattern.compile("^[\\\\\\w\\\\\\d]+[\\\\\\w\\\\\\d_.-]*$");

    private final boolean onPremises;

    private final AccountDao     accountDao;
    private final UserDao        userDao;
    private final UserProfileDao profileDao;

    public FactoryUrlBaseValidator(AccountDao accountDao,
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
        String type;
        String location;
        String parameterTypeName;
        String parameterLocationName;

        type = factory.getSource().getProject().getType();
        location = factory.getSource().getProject().getLocation();
        parameterTypeName = "source.project.type";
        parameterLocationName = "source.project.location";

        // check that vcs value is correct
        if (!("git".equals(type) || "esbwso2".equals(type))) {
            throw new ConflictException("Parameter '" + parameterTypeName + "' has illegal value.");
        }
        if (location == null || location.isEmpty()) {
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
        String pname = null;
        switch (factory.getV()) {
            case "2.0":
            case "2.1":
                if (null != factory.getProject()) {
                    pname = factory.getProject().getName();
                }
                break;
            default:
                // do nothing
        }
        if (null != pname && !PROJECT_NAME_VALIDATOR.matcher(pname).matches()) {
            throw new ConflictException(
                    "Project name must contain only Latin letters, digits or these following special characters -._.");
        }
    }

    protected void validateOrgid(Factory factory) throws ApiException {
        // validate accountid
        String orgid;
        String userid;
        // TODO do we need check if user is temporary?

        orgid = factory.getCreator() != null ? Strings.emptyToNull(factory.getCreator().getAccountId()) : null;
        userid = factory.getCreator() != null ? factory.getCreator().getUserId() : null;

        if (null != orgid) {
            if (null != userid) {
                try {
                    User user = userDao.getById(userid);
                    Profile profile = profileDao.getById(userid);
                    if (profile.getAttributes() != null && "true".equals(profile.getAttributes().get("temporary")))
                        throw new ConflictException("Current user is not allowed for using this method.");
                    boolean isOwner = false;
                    List<Member> members = accountDao.getMembers(orgid);
                    if (members.isEmpty()) {
                        throw new ConflictException(format(PARAMETRIZED_ILLEGAL_ORGID_PARAMETER_MESSAGE, orgid));
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
        }

    }

    protected void validateTrackedFactoryAndParams(Factory factory) throws ApiException {
        // validate tracked parameters
        String orgid = factory.getCreator() != null ? Strings.emptyToNull(factory.getCreator().getAccountId()) : null;

        if (orgid != null) {
            try {
                List<Subscription> subscriptions = accountDao.getSubscriptions(orgid, "Factory");
                boolean isTracked = false;
                for (Subscription one : subscriptions) {
                    if ("Tracked".equalsIgnoreCase(one.getProperties().get("Package"))) {
                        isTracked = true;
                        break;
                    }
                }
                if (!isTracked && !onPremises) {
                    throw new ConflictException(format(PARAMETRIZED_ILLEGAL_ORGID_PARAMETER_MESSAGE, orgid));
                }
            } catch (NotFoundException | ServerException | NumberFormatException e) {
                throw new ConflictException(format(PARAMETRIZED_ILLEGAL_ORGID_PARAMETER_MESSAGE, orgid));
            }
        }

        Long validSince = null;
        Long validUntil = null;


        final Policies policies = factory.getPolicies();
        if (policies != null) {
            validSince = policies.getValidSince();
            validUntil = policies.getValidUntil();
        }

        if (validSince != null && validSince > 0) {
            if (null == orgid && !onPremises) {
                throw new ConflictException(format(PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, null, "policies.validSince"));
            }
        }

        if (validUntil != null && validUntil > 0) {
            if (null == orgid && !onPremises) {
                throw new ConflictException(format(PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, null, "policies.validUntil"));
            }
        }
        if (policies != null && policies.getRefererHostname() != null && !policies.getRefererHostname().isEmpty()) {
            if (null == orgid && !onPremises) {
                throw new ConflictException(format(PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, null, "policies.refererHostname"));
            }
        }

        if (factory.getV().equals("2.0")) {
            WelcomePage welcomePage = null;
            if (factory.getActions() != null) {
                welcomePage = factory.getActions().getWelcome();
            }
            if (null != welcomePage) {
                if (null == orgid && !onPremises) {
                    throw new ConflictException(format(PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, null, "actions.welcome"));
                }
            }
        } else {
            if (factory.getIde() != null) {
                if (factory.getIde().getOnProjectOpened() != null && factory.getIde().getOnProjectOpened().getActions() != null) {
                    List<Action> onOpenedActions = factory.getIde().getOnProjectOpened().getActions();
                    for (Action onOpenedAction : onOpenedActions) {
                        if ("welcomePanel".equals(onOpenedAction.getId()) && null == orgid && !onPremises) {
                            throw new ConflictException(format(PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, null,
                                                               "ide.onProjectOpened.parts.[%index%].id=welcomePanel"));
                        }
                    }
                }

                if (factory.getIde().getOnAppLoaded() != null && factory.getIde().getOnAppLoaded().getActions() != null) {
                    List<Action> onLoadedActions = factory.getIde().getOnAppLoaded().getActions();
                    for (Action onLoadedAction : onLoadedActions) {
                        if ("welcomePanel".equals(onLoadedAction.getId()) && null == orgid && !onPremises) {
                            throw new ConflictException(format(PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, null,
                                                               "ide.onAppLoaded.parts.[%index%].id=welcomePanel"));
                        }
                    }
                }
            }
        }
    }

    protected void validateCurrentTimeBetweenSinceUntil(Factory factory) throws ConflictException {
        Long validSince = null;
        Long validUntil = null;

        final Policies policies = factory.getPolicies();
        if (policies != null) {
            validSince = policies.getValidSince();
            validUntil = policies.getValidUntil();
        }

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
        Long validSince = null;
        Long validUntil = null;

        final Policies policies = factory.getPolicies();
        if (policies != null) {
            validSince = policies.getValidSince();
            validUntil = policies.getValidUntil();
        }

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
        if (factory.getV() ==  null || !factory.getV().equals("2.1") || factory.getIde() == null) {
            return;
        }

        List<Action> applicationActions  =  new ArrayList<>();
        if (factory.getIde().getOnAppClosed() != null) {
            applicationActions.addAll(factory.getIde().getOnAppClosed().getActions());
        }
        if (factory.getIde().getOnAppLoaded() != null) {
            applicationActions.addAll(factory.getIde().getOnAppLoaded().getActions());
        }

        for (Action applicationAction : applicationActions) {
            String id =  applicationAction.getId();
            if (id.equals("openFile") || id.equals("findReplace")) {
                throw new ConflictException(String.format(FactoryConstants.INVALID_ACTION_SECTION, id));
            }
        }

        OnProjectOpened onOpened  = factory.getIde().getOnProjectOpened();
        if (onOpened != null) {
            List<Action> onProjectOpenedActions = factory.getIde().getOnProjectOpened().getActions();
            for (Action applicationAction : onProjectOpenedActions) {
                String id = applicationAction.getId();
                Map<String, String> properties = applicationAction.getProperties();
                if (id.equals("openFile") && Strings.isNullOrEmpty(properties.get("file"))) {
                    throw new ConflictException(INVALID_OPENFILE_ACTION);
                }

                if (id.equals("findReplace") && (Strings.isNullOrEmpty(properties.get("in")) ||
                                                 Strings.isNullOrEmpty(properties.get("find")) ||
                                                 Strings.isNullOrEmpty(properties.get("replace"))
                )) {
                    throw new ConflictException(INVALID_FIND_REPLACE_ACTION);
                }
            }
        }
    }
}
