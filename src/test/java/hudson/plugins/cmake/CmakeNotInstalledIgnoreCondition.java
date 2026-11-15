/*
 * The MIT License
 *
 * Copyright 2019 Martin Weber
 */
package hudson.plugins.cmake;

import java.io.IOException;

import org.junit.Assume;

/**
 * Prevents test from being run if cmake is not in search path for executables
 * ({@code $PATH} on linux).
 *
 * @author weber
 */
public class CmakeNotInstalledIgnoreCondition {
    private static Boolean isInstalled = null;

    /**
     * @return {@code true} if {@code cmake} is installed and available on PATH
     */
    public static synchronized boolean isCmakeInstalled() {
        if (isInstalled == null) {
            try {
                Process p = new ProcessBuilder("cmake", "--version").start();
                p.waitFor();
                isInstalled = Boolean.TRUE;
            } catch (IOException | InterruptedException e) {
                isInstalled = Boolean.FALSE;
            }
        }
        return isInstalled.booleanValue();
    }

    /**
     * Skips the current JUnit test via {@link Assume#assumeTrue} if
     * {@code cmake} is not installed.
     */
    public static void assumeCmakeInstalled() {
        Assume.assumeTrue("SKIPPED: cmake tool not installed",
                isCmakeInstalled());
    }
}
