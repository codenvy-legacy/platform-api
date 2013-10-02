/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
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
package com.codenvy.api.vfs.shared.dto;

import com.codenvy.api.vfs.shared.ItemType;
import com.codenvy.dto.shared.DTO;

import java.util.List;
import java.util.Map;

/**
 * Representation of abstract item used to interaction with client via JSON.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
@DTO
public interface Item {
    /** @return id of virtual file system that contains object */
    String getVfsId();

    void setVfsId(String vfsId);

    /** @return id of object */
    String getId();

    /**
     * @param id
     *         the id of object
     */
    void setId(String id);

    /** @return name of object */
    String getName();

    /**
     * @param name
     *         the name of object
     */
    void setName(String name);

    /** @return type of item */
    ItemType getItemType();

    void setItemType(ItemType itemType);

    /** @return path */
    String getPath();

    /**
     * @param path
     *         the path
     */
    void setPath(String path);

    /** @return id of parent folder and <code>null</code> if current item is root folder */
    String getParentId();

    /**
     * @param parentId
     *         id of parent folder and <code>null</code> if current item is root folder
     */
    void setParentId(String parentId);

    /** @return creation date */
    long getCreationDate();

    /**
     * @param creationDate
     *         the creation date
     */
    void setCreationDate(long creationDate);

    /** @return media type */
    String getMimeType();

    /**
     * @param mimeType
     *         media type
     */
    void setMimeType(String mimeType);

    /**
     * Other properties.
     *
     * @return properties. If there is no properties then empty list returned, never <code>null</code>
     */
    List<Property> getProperties();

    /** @see #getProperties() */
    void setProperties(List<Property> properties);

    /**
     * Links for retrieved or(and) manage item.
     *
     * @return links map. Never <code>null</code> but empty map instead
     */
    Map<String, Link> getLinks();

    /** @see #getLinks() */
    void setLinks(Map<String, Link> links);

    /**
     * Get permissions of current user. Current user is user who retrieved this item.
     *
     * @return set of permissions of current user.
     * @see com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo.BasicPermissions
     */
    List<String> getPermissions();

    /** @see #getPermissions() */
    void setPermissions(List<String> permissions);
}
