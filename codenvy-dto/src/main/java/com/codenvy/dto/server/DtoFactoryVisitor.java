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
package com.codenvy.dto.server;

/**
 * Visitor pattern. Generally needed to register DtoProviders by generated code in DtoFactory. Class which contains generated code for
 * server side implements this interface. When DtoFactory class is loaded it looks up for all implementation of this interface and calls
 * method {@link #accept(DtoFactory)}.
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
public interface DtoFactoryVisitor {
    void accept(DtoFactory dtoFactory);
}
