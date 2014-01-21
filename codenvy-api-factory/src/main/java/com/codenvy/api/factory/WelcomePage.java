package com.codenvy.api.factory;

/** Welcome page for organizations. */
public class WelcomePage {
    private WelcomeConfiguration authenticate;
    private WelcomeConfiguration nonauthenticate;

    public WelcomePage() {
    }

    public WelcomePage(WelcomeConfiguration authenticate, WelcomeConfiguration nonauthenticate) {
        this.authenticate = authenticate;
        this.nonauthenticate = nonauthenticate;
    }

    public WelcomeConfiguration getAuthenticate() {
        return authenticate;
    }

    public void setAuthenticate(WelcomeConfiguration authenticate) {
        this.authenticate = authenticate;
    }

    public WelcomeConfiguration getNonauthenticate() {
        return nonauthenticate;
    }

    public void setNonauthenticate(WelcomeConfiguration nonauthenticate) {
        this.nonauthenticate = nonauthenticate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WelcomePage that = (WelcomePage)o;

        if (authenticate != null ? !authenticate.equals(that.authenticate) : that.authenticate != null) return false;
        if (nonauthenticate != null ? !nonauthenticate.equals(that.nonauthenticate) : that.nonauthenticate != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = authenticate != null ? authenticate.hashCode() : 0;
        result = 31 * result + (nonauthenticate != null ? nonauthenticate.hashCode() : 0);
        return result;
    }
}
