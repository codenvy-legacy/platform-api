package com.codenvy.api.factory;

/** Welcome message for organizations. */
public class WelcomeConfiguration {

    private String title;
    private String iconurl;
    private String content;

    public WelcomeConfiguration() {
    }

    public WelcomeConfiguration(String title, String iconurl, String content) {
        this.title = title;
        this.iconurl = iconurl;
        this.content = content;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WelcomeConfiguration that = (WelcomeConfiguration)o;

        if (content != null ? !content.equals(that.content) : that.content != null) return false;
        if (iconurl != null ? !iconurl.equals(that.iconurl) : that.iconurl != null) return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (iconurl != null ? iconurl.hashCode() : 0);
        result = 31 * result + (content != null ? content.hashCode() : 0);
        return result;
    }
}
