package com.codenvy.api.machine.server;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

/**
 * String representation of machine recipe
 *
 * @author Alexander Garagatyi
 */
public class StringMachineRecipeImpl implements MachineRecipe {
    private final String recipe;

    public StringMachineRecipeImpl(String recipe) {
        this.recipe = recipe;
    }

    @Override
    public String asString() {
        return recipe;
    }

    @Override
    public Reader asReader() {
        return null;
    }

    @Override
    public URL asURL() {
        return null;
    }

    @Override
    public File asFile() {
        return null;
    }

    @Override
    public InputStream asStream() {
        return null;
    }
}
