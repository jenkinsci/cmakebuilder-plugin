/*
 * The MIT License
 *
 * Copyright 2018 Martin Weber
 */
package hudson.plugins.cmake;

import hudson.FilePath;
import hudson.util.ArgumentListBuilder;

/**
 * Utility functions for launching a tool on a (possibly remote) node.
 *
 * @author Martin Weber
 */
class LaunchUtils {

    private LaunchUtils() {
    }

    /**
     * Constructs the command line to invoke the tool.
     *
     * @param toolBin
     *            the name of the build tool binary, either as an absolute or
     *            relative file system path.
     * @param toolArgs
     *            additional arguments, separated by spaces to pass to cmake or
     *            {@code null}
     * @return the argument list, never {@code null}
     */
    static ArgumentListBuilder buildCommandline(final String toolBin,
            String toolArgs) {
        ArgumentListBuilder args = new ArgumentListBuilder();

        args.add(toolBin);
        if (toolArgs != null) {
            args.addTokenized(toolArgs);
        }
        return args;
    }

    /**
     * Constructs a directory below the workspace on a node.
     *
     * @param path
     *            the directory´s relative path {@code null} to return the
     *            workspace directory
     *
     * @return the full path of the directory on the remote machine.
     */
    static FilePath makeRemotePath(FilePath workSpace, String path) {
        if (path == null) {
            return workSpace;
        }
        FilePath file = workSpace.child(path);
        return file;
    }


}
