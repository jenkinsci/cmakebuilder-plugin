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

import hudson.remoting.VirtualChannel;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jenkins.MasterToSlaveFileCallable;
import de.marw.cmake.cmakecache.CMakeCacheFileParser;
import de.marw.cmake.cmakecache.CMakeCacheFileParser.EntryFilter;
import de.marw.cmake.cmakecache.SimpleCMakeCacheEntry;

/**
 * Gets the value of the {@code "CMAKE_MAKE_PROGRAM"} entry from a cmake cache
 * file.
 *
 * @author Martin Weber
 */
public class BuildToolEntryParser extends MasterToSlaveFileCallable<String> {

    private static final long serialVersionUID = 1L;

    /**
     * Parses the cache file and returns value of the {@code "CMAKE_MAKE_PROGRAM"}
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
                    return "CMAKE_MAKE_PROGRAM".equals(key);
                }
            }, result, null);
            if (result.size() > 0) {
                return result.get(0).getValue();
            }
        return null;
    }
}
