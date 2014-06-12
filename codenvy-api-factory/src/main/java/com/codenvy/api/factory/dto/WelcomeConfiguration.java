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
package com.codenvy.api.factory.dto;

import com.codenvy.dto.shared.DTO;

/**
 * Welcome message configuration. Contains title, link for icon url, and link for content page e.g. HTML or something
 * else.
 * This configuration will be processed when user apply factory link. And shows in right side of IDE
 */
@DTO
public interface WelcomeConfiguration {

    // Greeting title

    String getTitle();

    void setTitle(String title);

    WelcomeConfiguration withTitle(String title);

    // URL to greeting icon

    String getIconurl();

    void setIconurl(String iconurl);

    WelcomeConfiguration withIconurl(String iconurl);

    // URL to greeting page

    String getContenturl();

    void setContenturl(String contenturl);

    WelcomeConfiguration withContenturl(String contenturl);

    // Notification

    String getNotification();

    void setNotification(String notification);

    WelcomeConfiguration withNotification(String notification);

}
