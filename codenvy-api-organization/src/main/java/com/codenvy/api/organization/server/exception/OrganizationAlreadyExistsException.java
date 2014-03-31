/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2014] Codenvy, S.A. 
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
package com.codenvy.api.organization.server.exception;

/**
 * @author Eugene Voevodin
 */
public class OrganizationAlreadyExistsException extends OrganizationException {

    private OrganizationAlreadyExistsException(String message) {
        super(message);
    }

    public static OrganizationAlreadyExistsException existsWithOwner(String owner) {
        return new OrganizationAlreadyExistsException(String.format("Organization which owner is %s already exists", owner));
    }

    public static OrganizationAlreadyExistsException existsWithName(String name) {
        return new OrganizationAlreadyExistsException(String.format("Organization with name %s already exists", name));
    }
}
