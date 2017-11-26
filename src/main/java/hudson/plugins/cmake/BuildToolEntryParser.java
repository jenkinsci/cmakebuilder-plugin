/*******************************************************************************
 * Copyright (c) 2015-2017 Martin Weber.
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
import java.io.InputStreamReader;
import java.nio.charset.Charset;
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
     * Parses the cache file and returns value of the
     * {@code "CMAKE_MAKE_PROGRAM"} entry.
     *
     * @return the entry value or {@code null} if the file could not be parsed
     */
    @Override
    public String invoke(File cmakeCacheFile, VirtualChannel channel)
            throws IOException, InterruptedException {
        InputStreamReader isr = new InputStreamReader(
                new BufferedInputStream(new FileInputStream(cmakeCacheFile)),
                Charset.defaultCharset());
        List<SimpleCMakeCacheEntry> result = new ArrayList<>(1);
        final CMakeCacheFileParser parser = new CMakeCacheFileParser();
        try {
            parser.parse(isr, new EntryFilter() {

                @Override
                public boolean accept(String key) {
                    return "CMAKE_MAKE_PROGRAM".equals(key);
                }
            }, result, null);
        } finally {
            isr.close();
        }
        if (result.size() > 0) {
            return result.get(0).getValue();
        }
        return null;
    }
}
