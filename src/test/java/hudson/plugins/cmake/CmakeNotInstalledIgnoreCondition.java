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

    /**
     * @return <code>true</code> if the test should be skipped (cmake is NOT
     *         installed), otherwise <code>false</code>
     */
    @Override
    public boolean evaluate(Description testCaseDescription) {
        try {
            new ProcessBuilder("cmake", "--version").start();
            return false; // cmake is installed
        } catch (IOException e) {
            return true; // skip test
        }
    }
}
