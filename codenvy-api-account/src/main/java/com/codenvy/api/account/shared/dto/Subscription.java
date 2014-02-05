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

import com.codenvy.dto.shared.DTO;

import java.util.Map;

/**
 * Describes subscription
 *
 * @author Eugene Voevodin
 */
@DTO
public interface Subscription {

    String getServiceId();

    void setServiceId(String id);

    Subscription withServiceId(String id);

    String getStartDate();

    void setStartDate(String date);

    Subscription withStartDate(String date);

    String getEndDate();

    void setEndDate(String date);

    Subscription withEndDate(String date);

    Map<String, String> getProperties();

    void setProperties(Map<String, String> properties);

    Subscription withProperties(Map<String, String> properties);
}