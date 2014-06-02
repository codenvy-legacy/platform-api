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

import com.codenvy.dto.shared.DTO;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
@DTO
public interface File extends Item {
    /** @return version id */
    String getVersionId();

    File withVersionId(String versionId);

    void setVersionId(String versionId);

    /** @return content length */
    long getLength();

    File withLength(long length);

    void setLength(long length);

    /** @return date of last modification */
    long getLastModificationDate();

    File withLastModificationDate(long lastModificationDate);

    void setLastModificationDate(long lastModificationDate);

    /** @return <code>true</code> if object locked and <code>false</code> otherwise */
    boolean isLocked();

    File withLocked(boolean locked);

    void setLocked(boolean locked);
}
