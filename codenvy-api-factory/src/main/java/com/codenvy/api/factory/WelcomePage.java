package com.codenvy.api.factory;

/**
 * Welcome page which user can specified to show when factory accepted.
 * To show custom information applied only for this factory url.
 * Contains two configuration for authenticated users and non authenticated.
 */
public class WelcomePage {
    private WelcomeConfiguration authenticated;
    private WelcomeConfiguration nonauthenticated;

    public WelcomePage() {
    }

    public WelcomePage(WelcomeConfiguration authenticated, WelcomeConfiguration nonauthenticated) {
        this.authenticated = authenticated;
        this.nonauthenticated = nonauthenticated;
    }

    public WelcomeConfiguration getAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(WelcomeConfiguration authenticated) {
        this.authenticated = authenticated;
    }

    public WelcomeConfiguration getNonauthenticated() {
        return nonauthenticated;
    }

    public void setNonauthenticated(WelcomeConfiguration nonauthenticated) {
        this.nonauthenticated = nonauthenticated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WelcomePage that = (WelcomePage)o;

        if (authenticated != null ? !authenticated.equals(that.authenticated) : that.authenticated != null) return false;
        if (nonauthenticated != null ? !nonauthenticated.equals(that.nonauthenticated) : that.nonauthenticated != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = authenticated != null ? authenticated.hashCode() : 0;
        result = 31 * result + (nonauthenticated != null ? nonauthenticated.hashCode() : 0);
        return result;
    }
}
