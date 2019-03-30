/*
 * The MIT License
 *
 * Copyright 2019 Martin Weber
 */
package hudson.plugins.cmake;

import java.io.IOException;

import org.apache.geode.test.junit.IgnoreCondition;
import org.junit.runner.Description;

/**
 * Prevents test from being run if cmake is not in search path for executables
 * ({@code $path} on linux).
 *
 * @author weber
 */
public class CmakeNotInstalledIgnoreCondition implements IgnoreCondition {
    private Boolean isInstalled= null;
    /**
     * @return <code>true</code> if the test should be skipped (cmake is NOT
     *         installed), otherwise <code>false</code>
     */
    @Override
    public boolean evaluate(Description testCaseDescription) {
        if(isInstalled == null)
        try {
            new ProcessBuilder("cmake", "--version").start();
            isInstalled= Boolean.TRUE; // cmake is installed
        } catch (IOException e) {
            isInstalled= Boolean.FALSE; // skip test
        }
        return !isInstalled.booleanValue();
    }
}
