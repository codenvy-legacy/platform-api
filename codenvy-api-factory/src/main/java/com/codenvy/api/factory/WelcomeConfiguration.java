package com.codenvy.api.factory;

/**
 * Welcome message configuration. Contains title, link for icon url, and link for content page e.g. HTML or something else.
 * This configuration will be processed when user apply factory link. And shows in right side of IDE
 */
public class WelcomeConfiguration {

    private String title;
    private String iconurl;
    private String contenturl;

    public WelcomeConfiguration() {
    }

    public WelcomeConfiguration(String title, String iconurl, String contenturl) {
        this.title = title;
        this.iconurl = iconurl;
        this.contenturl = contenturl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIconurl() {
        return iconurl;
    }

    public void setIconurl(String iconurl) {
        this.iconurl = iconurl;
    }

    public String getContenturl() {
        return contenturl;
    }

    public void setContenturl(String contenturl) {
        this.contenturl = contenturl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WelcomeConfiguration that = (WelcomeConfiguration)o;

        if (contenturl != null ? !contenturl.equals(that.contenturl) : that.contenturl != null) return false;
        if (iconurl != null ? !iconurl.equals(that.iconurl) : that.iconurl != null) return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (iconurl != null ? iconurl.hashCode() : 0);
        result = 31 * result + (contenturl != null ? contenturl.hashCode() : 0);
        return result;
    }
}
