/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.core.rest.shared.dto;

import com.codenvy.dto.shared.DTO;

/**
 * DTO interface for showing multiple statuses in single response. Idea is close to 207 Multi-Status (WebDAV; RFC 4918)
 * <a href="http://en.wikipedia.org/wiki/List_of_HTTP_status_codes#2xx_Success">2xx Success</a>.
 * Typically this is useful when we need to pass information about set of objects (collection of some other DTOs) even if can't provide
 * correct information about each one. Here is example:
 * 1. DTO interface
 * <pre>
 *     &#064DTO
 *     public interface Widget {
 *         String getProperty();
 *         void setProperty(String property);
 *         Widget withProperty(String property);
 *
 *         ObjectStatus getStatus();
 *         void getStatus(ObjectStatus status);
 *         Widget withStatus(ObjectStatus status);
 *     }
 * </pre>
 * 2. Service
 * <pre>
 *     &#064Path("some")
 *     public class SomeService {
 *         &#064GET
 *         &#064Produces("application/json")
 *         public List&lt;Widget&gt; getProperties() {
 *             List&lt;Widget&gt; result = new LinkedList&lt;&gt;();
 *             DtoFactory dtoFactory = DtoFactory.getInstance();
 *             Widget widget = dtoFactory.createDto(Widget.class);
 *             // Set widget's property. If have some error with one Widget instead of throwing exception add information about error for
 *             // current Widget.
 *             try {
 *                 widget.setProperty(calculateProperty());
 *             } catch (Exception e) {
 *                 widget.setStatus(dtoFactory.createDto(ObjectStatus.class).withCode(...).withMessage(...));
 *             }
 *             result.add(widget);
 *             return result;
 *         }
 *     }
 * </pre>
 *
 * @author andrew00x
 */
@DTO
public interface ObjectStatus {
    int getCode();

    void setCode(int status);

    ObjectStatus withCode(int status);

    String getMessage();

    void setMessage(String message);

    ObjectStatus withMessage(String message);
}
