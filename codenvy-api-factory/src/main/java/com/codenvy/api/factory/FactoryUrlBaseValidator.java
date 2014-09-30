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
import com.codenvy.api.factory.dto.Factory;
import com.codenvy.api.factory.dto.Policies;
import com.codenvy.api.factory.dto.Restriction;
import com.codenvy.api.user.server.dao.Profile;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.user.server.dao.User;
import com.codenvy.commons.lang.Strings;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import static com.codenvy.api.factory.FactoryConstants.PARAMETRIZED_ILLEGAL_ORGID_PARAMETER_MESSAGE;
import static com.codenvy.api.factory.FactoryConstants.PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE;
import static java.lang.String.format;

/**
 * Validates values of factory parameters.
 *
 * @author Alexander Garagatyi
 */
public abstract class FactoryUrlBaseValidator {
    private static final Pattern PROJECT_NAME_VALIDATOR = Pattern.compile("^[\\\\\\w\\\\\\d]+[\\\\\\w\\\\\\d_.-]*$");

    private AccountDao accountDao;

    private UserDao userDao;

    private UserProfileDao profileDao;

    public FactoryUrlBaseValidator(AccountDao accountDao, UserDao userDao, UserProfileDao profileDao) {
        this.accountDao = accountDao;
        this.userDao = userDao;
        this.profileDao = profileDao;
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
        if (factory.getV().startsWith("1.")) {
            type = factory.getVcs();
            location = factory.getVcsurl();
            parameterTypeName = "vcs";
            parameterLocationName = "vcsurl";
        } else {
            type = factory.getSource().getType();
            location = factory.getSource().getLocation();
            parameterTypeName = "source.type";
            parameterLocationName = "source.location";
        }
        // check that vcs value is correct (only git is supported for now)
        if (!"git".equals(type)) {
            throw new ConflictException("Parameter '" + parameterTypeName + "' has illegal value. Only 'git' is supported for now.");
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
            case "1.0":
                pname = factory.getPname();
                break;
            case "1.1":
            case "1.2":
                if (factory.getProjectattributes() != null) {
                    pname = factory.getProjectattributes().getPname();
                }
                break;
            case "2.0":
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
        // validate orgid
        String orgid;
        String userid;
        // TODO do we need check if user is temporary?
        if (factory.getV().startsWith("1.")) {
            orgid = Strings.emptyToNull(factory.getOrgid());
            userid = factory.getUserid();
        } else {
            orgid = factory.getCreator() != null ? Strings.emptyToNull(factory.getCreator().getAccountId()) : null;
            userid = factory.getCreator() != null ? factory.getCreator().getUserId() : null;
        }

        if (null != orgid) {
            if (userid != null) {
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
                        throw new ConflictException("You are not authorized to use this orgid.");
                    }
                } catch (NotFoundException | ServerException e) {
                    throw new ConflictException("You are not authorized to use this orgid.");
                }
            }
        }

    }

    protected void validateTrackedFactoryAndParams(Factory factory) throws ApiException {
        // validate tracked parameters
        String orgid;
        if (factory.getV().startsWith("1.")) {
            orgid = Strings.emptyToNull(factory.getOrgid());
        } else {
            orgid = factory.getCreator() != null ? Strings.emptyToNull(factory.getCreator().getAccountId()) : null;
        }

        if (orgid != null) {
            try {
                List<Subscription> subscriptions = accountDao.getSubscriptions(orgid, "Factory");
                boolean isTracked = false;
                for (Subscription one : subscriptions) {
                    if ("Tracked".equals(one.getProperties().get("Package"))) {
                        isTracked = true;
                        break;
                    }
                }
                if (!isTracked) {
                    throw new ConflictException(format(PARAMETRIZED_ILLEGAL_ORGID_PARAMETER_MESSAGE, orgid));
                }
            } catch (NotFoundException | ServerException | NumberFormatException e) {
                throw new ConflictException(format(PARAMETRIZED_ILLEGAL_ORGID_PARAMETER_MESSAGE, orgid));
            }
        }

        if (factory.getV().startsWith("1.")) {
            final Restriction restriction = factory.getRestriction();
            if (restriction != null) {
                if (0 != restriction.getValidsince()) {
                    if (null == orgid) {
                        throw new ConflictException(format(PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, null, "validsince"));
                    }

                    if (new Date().before(new Date(restriction.getValidsince()))) {
                        throw new ConflictException(FactoryConstants.ILLEGAL_VALIDSINCE_MESSAGE);
                    }
                }

                if (0 != restriction.getValiduntil()) {
                    if (null == orgid) {
                        throw new ConflictException(format(PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, null, "validuntil"));
                    }

                    if (new Date().after(new Date(restriction.getValiduntil()))) {
                        throw new ConflictException(FactoryConstants.ILLEGAL_VALIDUNTIL_MESSAGE);
                    }
                }
            }

            if (null != factory.getWelcome()) {
                if (null == orgid) {
                    throw new ConflictException(format(PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, null, "welcome"));
                }
            }
        } else {
            final Policies policies = factory.getPolicies();
            if (policies != null) {
                if (0 != policies.getValidSince()) {
                    if (null == orgid) {
                        throw new ConflictException(format(PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, null, "validSince"));
                    }

                    if (new Date().before(new Date(policies.getValidSince()))) {
                        throw new ConflictException(FactoryConstants.ILLEGAL_VALIDSINCE_MESSAGE);
                    }
                }

                if (0 != policies.getValidUntil()) {
                    if (null == orgid) {
                        throw new ConflictException(format(PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, null, "validUntil"));
                    }

                    if (new Date().after(new Date(policies.getValidUntil()))) {
                        throw new ConflictException(FactoryConstants.ILLEGAL_VALIDUNTIL_MESSAGE);
                    }
                }
            }

            if (factory.getActions() != null && factory.getActions().getWelcome() != null) {
                if (null == orgid) {
                    throw new ConflictException(format(PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, null, "welcome"));
                }
            }
        }
    }
}
