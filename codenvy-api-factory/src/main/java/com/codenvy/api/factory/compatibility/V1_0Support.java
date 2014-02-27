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
package com.codenvy.api.factory.compatibility;

import com.codenvy.api.factory.FactoryUrlException;
import com.codenvy.api.factory.dto.Factory;
import com.codenvy.api.factory.dto.ProjectAttributes;
import com.codenvy.dto.server.DtoFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Alexander Garagatyi
 */
public class V1_0Support implements FactoryVersionSupporter {
    public static enum ParameterState {
        SUPPORTED, UNSUPPORTED, IGNORED
    }

    private static final Map<String, ParameterState> support;

    static {
        support = new HashMap<>();
        support.put("v", ParameterState.SUPPORTED);
        support.put("vcs", ParameterState.SUPPORTED);
        support.put("vcsurl", ParameterState.SUPPORTED);
        support.put("commitid", ParameterState.SUPPORTED);
        support.put("idcommit", ParameterState.SUPPORTED);
        support.put("pname", ParameterState.SUPPORTED);
        support.put("ptype", ParameterState.SUPPORTED);
        support.put("action", ParameterState.SUPPORTED);
        support.put("wname", ParameterState.IGNORED);
    }

    @Override
    public void set(Factory factory, String key, String value) throws FactoryUrlException {
        ParameterState state = support.get(key);
        if (null == state || ParameterState.UNSUPPORTED.equals(state)) {
            throw new FactoryUrlException("Parameter " + key + " is unsupported.");
        }
        switch (state) {
            case SUPPORTED:
                doSet(factory, key, value);
                break;
            case IGNORED:
                return;
        }
    }

    @Override
    public void validate(Factory factory) throws FactoryUrlException {
        throw new FactoryUrlException("Factory 1.0 does not support encoded version.");
    }

    private void doSet(Factory factory, String key, String value) {
        ProjectAttributes attributes;
        switch (key) {
            case "v":
                factory.setV("1.0");
                break;
            case "vcs":
                factory.setVcs(value);
                break;
            case "vcsurl":
                factory.setVcsurl(value);
                break;
            case "idcommit":
            case "commitid":
                factory.setCommitid(value);
                break;
            case "pname":
                attributes = factory.getProjectattributes();
                if (attributes == null) {
                    attributes = DtoFactory.getInstance().createDto(ProjectAttributes.class);
                }
                attributes.setPname(value);
                factory.setProjectattributes(attributes);
                break;
            case "ptype":
                attributes = factory.getProjectattributes();
                if (attributes == null) {
                    attributes = DtoFactory.getInstance().createDto(ProjectAttributes.class);
                }
                attributes.setPtype(value);
                factory.setProjectattributes(attributes);
                break;
            case "action":
                factory.setAction(value);
                break;
        }
    }
}
