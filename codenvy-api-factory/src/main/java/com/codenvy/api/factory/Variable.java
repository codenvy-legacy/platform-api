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
    }
}
