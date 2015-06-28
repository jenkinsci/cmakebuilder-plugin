/*******************************************************************************
 * Copyright (c) 2015 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package hudson.plugins.cmake;

import hudson.FilePath;
import hudson.remoting.VirtualChannel;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jenkins.security.Roles;

import org.jenkinsci.remoting.RoleChecker;

import de.marw.cmake.cmakecache.CMakeCacheFileParser;
import de.marw.cmake.cmakecache.CMakeCacheFileParser.EntryFilter;
import de.marw.cmake.cmakecache.SimpleCMakeCacheEntry;

/**
 * Gets the value of the {@code "CMAKE_BUILD_TOOL"} entry from a cmake cache
 * file.
 *
 * @author Martin Weber
 */
public class BuildToolEntryParser implements FilePath.FileCallable<String> {

    private static final long serialVersionUID = 1L;

    /**
     * Parses the cach file and returns value of the {@code "CMAKE_BUILD_TOOL"}
     * entry.
     *
     * @return the entry value or {@code null} if the file could not be parsed
     */
    @Override
    public String invoke(File cmakeCacheFile, VirtualChannel channel)
            throws IOException, InterruptedException {
            BufferedInputStream is = new BufferedInputStream(
                    new FileInputStream(cmakeCacheFile));
            List<SimpleCMakeCacheEntry> result = new ArrayList<SimpleCMakeCacheEntry>(
                    1);
            final CMakeCacheFileParser parser = new CMakeCacheFileParser();

            parser.parse(is, new EntryFilter() {

                @Override
                public boolean accept(String key) {
                    return "CMAKE_BUILD_TOOL".equals(key);
                }
            }, result, null);
            if (result.size() > 0) {
                return result.get(0).getValue();
            }
        return null;
    }

    /*-
     * @see org.jenkinsci.remoting.RoleSensitive#checkRoles(org.jenkinsci.remoting.RoleChecker)
     */
    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        checker.check(this, Roles.SLAVE);
    }

}
