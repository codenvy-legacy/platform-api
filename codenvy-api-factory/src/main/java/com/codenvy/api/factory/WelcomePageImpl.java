package com.codenvy.api.factory;

import com.codenvy.api.factory.dto.WelcomeConfiguration;
import com.codenvy.api.factory.dto.WelcomePage;

/**
 * Implementation of {@link com.codenvy.api.factory.dto.WelcomePage}
 */
public class WelcomePageImpl implements WelcomePage {
    private WelcomeConfiguration authenticated;
    private WelcomeConfiguration nonauthenticated;

    public WelcomePageImpl() {
        this.authenticated = new WelcomeConfigurationImpl();
        this.nonauthenticated = new WelcomeConfigurationImpl();
    }

    public WelcomePageImpl(WelcomeConfiguration authenticated, WelcomeConfiguration nonauthenticated) {
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

        WelcomePageImpl that = (WelcomePageImpl)o;

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
