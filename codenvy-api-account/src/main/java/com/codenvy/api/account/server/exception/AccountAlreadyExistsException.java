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
package com.codenvy.api.account.server.exception;

/**
 * @author Eugene Voevodin
 */
public class AccountAlreadyExistsException extends AccountException {

    private AccountAlreadyExistsException(String message) {
        super(message);
    }

    public static AccountAlreadyExistsException existsWithOwner(String owner) {
        return new AccountAlreadyExistsException(String.format("Account which owner is %s already exists!", owner));
    }

    public static AccountAlreadyExistsException existsWithName(String name) {
        return new AccountAlreadyExistsException(String.format("Account with name %s already exists!", name));
    }
}
