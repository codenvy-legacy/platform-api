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
package com.codenvy.api.account.shared.dto;

import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * @author andrew00x
 */
@DTO
public interface Account {
    String getOwner();

    void setOwner(String owner);

    Account withOwner(String owner);

    List<Attribute> getAttributes();

    void setAttributes(List<Attribute> attributes);

    Account withAttributes(List<Attribute> attributes);

    String getName();

    void setName(String name);

    Account withName(String name);

    String getId();

    void setId(String id);

    Account withId(String id);

    List<Link> getLinks();

    void setLinks(List<Link> links);

    Account withLinks(List<Link> links);


}
