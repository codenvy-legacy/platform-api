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
package com.codenvy.api.auth.sso.server;

import java.util.Set;

/** Manager to persist access tickets for SSO login process */
public interface TicketManager {

    /**
     * Add access ticket
     *
     * @param accessTicket
     *         ticket to add
     */
    void putAccessTicket(AccessTicket accessTicket);

    /**
     * Get access ticket from manager by its token
     *
     * @param accessToken
     *         unique token of access ticket
     * @return access ticket
     */
    AccessTicket getAccessTicket(String accessToken);

    /**
     * Remove access ticket from manager.
     *
     * @param accessToken
     *         unique token of ticket to remove
     * @return removed instance of <code>AccessTicket</code>
     */
    AccessTicket removeTicket(String accessToken);

    /**
     * Get all access tickets.
     *
     * @return set of access tickets
     */
    Set<AccessTicket> getAccessTickets();

    /**
     * Execute task with lock of write methods
     *
     * @param task
     *         - task to execute
     * @return - future result
     */
    <V> V executeTaskDuringWriteLock(TickedManagerTask<V> task);

    /**
     * Execute task with lock of read methods
     *
     * @param task
     *         - task to execute
     * @return - future result
     */
    <V> V executeTaskDuringReadLock(TickedManagerTask<V> task);

    /**
     * External task what can be executed during read or readWrite lock inside TickedManager
     *
     * @param <V>
     *         - future result of execution
     */
    public interface TickedManagerTask<V> {
        V call();
    }
}
