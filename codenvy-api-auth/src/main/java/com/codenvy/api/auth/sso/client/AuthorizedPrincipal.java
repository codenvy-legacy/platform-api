package com.codenvy.api.auth.sso.client;

import java.io.*;
import java.security.Principal;
import java.util.*;

/** Principal with roles. */
public class AuthorizedPrincipal implements Principal, Externalizable {
    private String                name;
    private Map<String, RolesSet> wsRoles;
    private Set<String>           roles;
    private String                token;

    public AuthorizedPrincipal(String name) {
        this.name = name;
        this.wsRoles = Collections.emptyMap();
        this.roles = Collections.emptySet();
    }

    public AuthorizedPrincipal() {
        this(null);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    /**
     * @param workspaceId
     *         - given workspace.
     * @return Set of roles in given workspace.
     */
    public Set<String> getWsRoles(String workspaceId) {
        if (wsRoles.get(workspaceId) != null && wsRoles.get(workspaceId).getRoles() != null) {
            return new HashSet<>(wsRoles.get(workspaceId).getRoles());
        }
        return Collections.emptySet();
    }

    public Set<String> getRoles() {
        return new HashSet<>(roles);
    }

    public Map<String, RolesSet> getWsRoles() {
        Map<String, RolesSet> wsRolesCopy = new HashMap<>(this.wsRoles.size());
        for (Map.Entry<String, RolesSet> entry : this.wsRoles.entrySet()) {
            wsRolesCopy.put(entry.getKey(), new RolesSet(entry.getValue()));
        }
        return wsRolesCopy;
    }

    public synchronized void setWsRoles(Map<String, RolesSet> wsRoles) {
        if (wsRoles == null) {
            throw new IllegalArgumentException("Null value is not allowed for roles parameter");
        }
        Map<String, RolesSet> newWsRoles = new HashMap<>(wsRoles.size());
        for (Map.Entry<String, RolesSet> entry : wsRoles.entrySet()) {
            if (entry.getValue() != null) {
                newWsRoles.put(entry.getKey(), new RolesSet(entry.getValue()));
            }
        }
        this.wsRoles = newWsRoles;
    }

    public synchronized void setRoles(Set<String> roles) {
        if (roles == null) {
            throw new IllegalArgumentException("Null value is not allowed for roles parameter");
        }
        this.roles = new HashSet<>(roles);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthorizedPrincipal)) return false;

        AuthorizedPrincipal principal = (AuthorizedPrincipal)o;

        if (name != null ? !name.equals(principal.name) : principal.name != null) return false;
        if (roles != null ? !roles.equals(principal.roles) : principal.roles != null) return false;
        if (token != null ? !token.equals(principal.token) : principal.token != null) return false;
        if (wsRoles != null ? !wsRoles.equals(principal.wsRoles) : principal.wsRoles != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (wsRoles != null ? wsRoles.hashCode() : 0);
        result = 31 * result + (roles != null ? roles.hashCode() : 0);
        result = 31 * result + (token != null ? token.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AuthorizedPrincipal{" +
               "name='" + name + '\'' +
               ", wsRoles=" + wsRoles +
               ", roles=" + roles +
               ", token='" + token + '\'' +
               '}';
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(name);
        out.writeInt(wsRoles.size());
        for (Map.Entry<String, RolesSet> entry : wsRoles.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeInt(entry.getValue().getRoles().size());
            for (String role : entry.getValue().getRoles()) {
                out.writeUTF(role);
            }
        }
        out.writeInt(roles.size());
        for (String role : roles) {
            out.writeUTF(role);
        }
        out.writeUTF(token);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        name = in.readUTF();
        int wsRolesMapSize = in.readInt();
        if (wsRolesMapSize > 0) {
            Map<String, RolesSet> newWsRoles = new HashMap<>(wsRolesMapSize);
            for (int i = 0; i < wsRolesMapSize; ++i) {
                String ws = in.readUTF();
                int wsRolesSize = in.readInt();
                Set<String> wsRoles = new LinkedHashSet<>();

                if (wsRolesSize > 0) {
                    for (int j = 0; j < wsRolesSize; ++j) {
                        wsRoles.add(in.readUTF());
                    }
                }

                newWsRoles.put(ws, new RolesSet(wsRoles));
            }
            this.wsRoles = newWsRoles;
        }
        int rolesSetSize = in.readInt();
        if (rolesSetSize > 0) {
            Set<String> newRoles = new HashSet<>(rolesSetSize);
            for (int i = 0; i < rolesSetSize; ++i) {
                newRoles.add(in.readUTF());
            }
            this.roles = newRoles;
        }
        this.token = in.readUTF();
    }

    /**
     * Helper class to avoid everrest restriction of usage collection as parameter of other collection
     */
    public static class RolesSet {
        private Set<String> roles;

        public RolesSet() {
            this.roles = new HashSet<>();
        }

        public RolesSet(RolesSet roles) {
            if (roles != null && roles.getRoles() != null) {
                this.roles = new HashSet<>(roles.getRoles());
            } else {
                this.roles = new HashSet<>();
            }
        }

        public RolesSet(Set<String> roles) {
            if (roles != null) {
                this.roles = new HashSet<>(roles);
            } else {
                this.roles = new HashSet<>();
            }

        }

        public Set<String> getRoles() {
            return roles;
        }

        public void setRoles(Set<String> roles) {
            this.roles = roles;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RolesSet)) return false;

            RolesSet rolesSet = (RolesSet)o;

            if (roles != null ? !roles.equals(rolesSet.roles) : rolesSet.roles != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return roles != null ? roles.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "RolesSet{" +
                   "roles=" + roles +
                   '}';
        }
    }
}
