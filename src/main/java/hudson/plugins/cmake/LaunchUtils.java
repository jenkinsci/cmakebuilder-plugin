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

    /**
     * Constructs the command line to invoke cmake.
     *
     * @param cmakeBin
     *            the name of the cmake binary, either as an absolute or
     *            relative file system path.
     * @param generator
     *            the name of the build-script generator or {@code null}
     * @param preloadScript
     *            name of the pre-load a script to populate the cache or
     *            {@code null}
     * @param theSourceDir
     *            source directory, must not be {@code null}
     * @param buildType
     *            build type argument for cmake or {@code null} to pass none
     * @param cmakeArgs
     *            additional arguments, separated by spaces to pass to cmake or
     *            {@code null}
     * @return the argument list, never {@code null}
     */
    static ArgumentListBuilder buildCMakeCall(final String cmakeBin,
            final String generator, final String preloadScript,
            final FilePath theSourceDir, final String buildType,
            final String cmakeArgs) {
        ArgumentListBuilder args = new ArgumentListBuilder();
    
        args.add(cmakeBin);
        if (generator != null) {
            args.add("-G").add(generator);
        }
        if (preloadScript != null) {
            args.add("-C").add(preloadScript);
        }
        if (buildType != null) {
            args.add("-D").add("CMAKE_BUILD_TYPE=" + buildType);
        }
        if (cmakeArgs != null) {
            args.addTokenized(cmakeArgs);
        }
        args.add(theSourceDir.getRemote());
        return args;
    }

    /**
     * Constructs the command line to invoke the actual build tool.
     *
     * @param toolBin
     *            the name of the build tool binary, either as an absolute or
     *            relative file system path.
     * @param toolArgs
     *            additional arguments, separated by spaces to pass to cmake or
     *            {@code null}
     * @return the argument list, never {@code null}
     */
    static ArgumentListBuilder buildBuildToolCall(final String toolBin,
            String... toolArgs) {
        ArgumentListBuilder args = new ArgumentListBuilder();
    
        args.add(toolBin);
        if (toolArgs != null) {
            args.add(toolArgs);
        }
        return args;
    }

    /**
     * Constructs the command line to have the actual build tool invoked with
     * cmake.
     *
     * @param cmakeBin
     *            the name of the cmake tool binary, either as an absolute or
     *            relative file system path.
     * @param theBuildDir
     *            the build directory path
     * @param toolArgs
     *            additional build tool arguments, separated by spaces to pass to
     *            cmake or {@code null}
     * @return the argument list, never {@code null}
     */
    static ArgumentListBuilder buildBuildToolCallWithCmake(
            final String cmakeBin, FilePath theBuildDir, String... toolArgs) {
        ArgumentListBuilder args = new ArgumentListBuilder();
    
        args.add(cmakeBin);
        args.add("--build");
        args.add(theBuildDir.getRemote());
        if (toolArgs != null) {
            args.add(toolArgs);
        }
        return args;
    }


}
