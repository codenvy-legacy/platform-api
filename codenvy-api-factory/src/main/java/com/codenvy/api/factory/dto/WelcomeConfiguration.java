package com.codenvy.api.factory.dto;

import com.codenvy.dto.shared.DTO;

/**
 * Welcome message configuration. Contains title, link for icon url, and link for content page e.g. HTML or something
 * else.
 * This configuration will be processed when user apply factory link. And shows in right side of IDE
 */
@DTO
public interface WelcomeConfiguration {
    /**
     * @return
     */
    String getTitle();

    void setTitle(String title);

    WelcomeConfiguration withTitle(String title);

    /**
     * @return
     */
    String getIconurl();

    void setIconurl(String iconurl);

    WelcomeConfiguration withIconurl(String iconurl);

    /**
     * @return
     */
    String getContenturl();

    void setContenturl(String contenturl);

    WelcomeConfiguration withContenturl(String contenturl);
}
