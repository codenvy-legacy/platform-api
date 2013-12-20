package com.codenvy.api.factory;

import java.util.List;


/** Replacement variable, that contains list of files to find variables and replace by specified values. */
public class Variable {
    private List<String>      files;
    private List<Replacement> entries;

    public Variable() {
    }

    public Variable(List<String> files, List<Replacement> entries) {
        this.files = files;
        this.entries = entries;
    }

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }

    public List<Replacement> getEntries() {
        return entries;
    }

    public void setEntries(List<Replacement> entries) {
        this.entries = entries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Variable variable = (Variable)o;

        if (entries != null ? !entries.equals(variable.entries) : variable.entries != null) return false;
        if (files != null ? !files.equals(variable.files) : variable.files != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = files != null ? files.hashCode() : 0;
        result = 31 * result + (entries != null ? entries.hashCode() : 0);
        return result;
    }

    public static class Replacement {
        private String find;
        private String replace;

        public Replacement() {
        }

        public Replacement(String find, String replace) {
            this.find = find;
            this.replace = replace;
        }

        public String getFind() {
            return find;
        }

        public void setFind(String find) {
            this.find = find;
        }

        public String getReplace() {
            return replace;
        }

        public void setReplace(String replace) {
            this.replace = replace;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Replacement that = (Replacement)o;

            if (find != null ? !find.equals(that.find) : that.find != null) return false;
            if (replace != null ? !replace.equals(that.replace) : that.replace != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = find != null ? find.hashCode() : 0;
            result = 31 * result + (replace != null ? replace.hashCode() : 0);
            return result;
        }
    }
}
