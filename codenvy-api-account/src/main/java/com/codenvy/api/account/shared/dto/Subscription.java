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
package com.codenvy.api.account.shared.dto;

import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.dto.shared.DTO;

import java.util.List;
import java.util.Map;

/**
 * Describes subscription - a link between {@link com.codenvy.api.account.server.SubscriptionService} and {@link
 * Account}
 *
 * @author Eugene Voevodin
 */
@DTO
public interface Subscription {

    public enum State {
        WAIT_FOR_PAYMENT, ACTIVE
    }

    String getId();

    void setId(String id);

    Subscription withId(String id);

    String getAccountId();

    void setAccountId(String orgId);

    Subscription withAccountId(String orgId);

    String getServiceId();

    void setServiceId(String id);

    Subscription withServiceId(String id);

    long getStartDate();

    void setStartDate(long date);

    Subscription withStartDate(long date);

    long getEndDate();

    void setEndDate(long date);

    Subscription withEndDate(long date);

    Map<String, String> getProperties();

    void setProperties(Map<String, String> properties);

    Subscription withProperties(Map<String, String> properties);

    State getState();

    void setState(State state);

    Subscription withState(State state);

    void setLinks(List<Link> links);

    List<Link> getLinks();

    Subscription withLinks(List<Link> links);
}