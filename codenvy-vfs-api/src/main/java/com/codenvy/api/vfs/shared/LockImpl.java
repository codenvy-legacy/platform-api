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
package com.codenvy.api.vfs.shared;

public class LockImpl implements Lock {
    private String owner;
    private String lockToken;
    private long   timeout;

    /**
     * @param owner
     *         user who is owner of the lock
     * @param lockToken
     *         lock token
     * @param timeout
     *         lock timeout
     */
    public LockImpl(String owner, String lockToken, long timeout) {
        this.owner = owner;
        this.lockToken = lockToken;
        this.timeout = timeout;
    }

    public LockImpl() {
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public String getLockToken() {
        return lockToken;
    }

    @Override
    public void setLockToken(String lockToken) {
        this.lockToken = lockToken;
    }

    @Override
    public long getTimeout() {
        return timeout;
    }

    @Override
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (obj.getClass() != this.getClass()) || (lockToken == null)) {
            return false;
        }
        LockImpl otherLockToken = (LockImpl)obj;
        return lockToken.equals(otherLockToken.lockToken);
    }

    @Override
    public int hashCode() {
        int hash = 8;
        hash = hash * 31 + lockToken.hashCode();
        return hash;
    }
}
