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
public class AccountNotFoundException extends AccountException {

    private AccountNotFoundException(String message) {
        super(message);
    }

    public static AccountNotFoundException doesNotExistWithId(String id) {
        return new AccountNotFoundException(String.format("Account with identifier %s doesn't exist!", id));
    }

    public static AccountNotFoundException doesNotExistWithName(String name) {
        return new AccountNotFoundException(String.format("Account with name %s doesn't exist!", name));
    }

    public static AccountNotFoundException doesNotExistWithOwner(String owner) {
        return new AccountNotFoundException(String.format("Account which owner is %s doesn't exist!", owner));
    }
}