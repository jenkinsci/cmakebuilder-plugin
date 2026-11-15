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
     * Checks whether the given CMake generator is a multi-configuration
     * generator (such as Visual Studio, Xcode, or Ninja Multi-Config) where
     * {@code CMAKE_BUILD_TYPE} is ignored at configure time and
     * {@code --config <type>} is required at build time.
     *
     * @param generator
     *            the generator name or {@code null}
     * @return {@code true} if the generator is a multi-configuration generator
     */
    static boolean isMultiConfigGenerator(final String generator) {
        if (generator == null) {
            return false;
        }
        final String g = generator.trim();
        return g.regionMatches(true, 0, "Visual Studio", 0, "Visual Studio".length()) //$NON-NLS-1$ //$NON-NLS-2$
                || g.equalsIgnoreCase("Xcode") //$NON-NLS-1$
                || g.equalsIgnoreCase("Ninja Multi-Config") //$NON-NLS-1$
                || g.equalsIgnoreCase("Green Hills MULTI"); //$NON-NLS-1$
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
        return buildCMakeCall(cmakeBin, generator, preloadScript, null,
                theSourceDir, buildType, cmakeArgs);
    }

    /**
     * Constructs the command line to invoke cmake, supporting CMake presets
     * and multi-configuration generators.
     *
     * @param cmakeBin
     *            the name of the cmake binary, either as an absolute or
     *            relative file system path.
     * @param generator
     *            the name of the build-script generator or {@code null}
     * @param preloadScript
     *            name of the pre-load a script to populate the cache or
     *            {@code null}
     * @param preset
     *            CMake configure preset name ({@code --preset <preset>}) or
     *            {@code null}
     * @param theSourceDir
     *            source directory, or {@code null} when omitted for preset builds
     * @param buildType
     *            build type argument for cmake or {@code null} to pass none
     * @param cmakeArgs
     *            additional arguments, separated by spaces to pass to cmake or
     *            {@code null}
     * @return the argument list, never {@code null}
     */
    static ArgumentListBuilder buildCMakeCall(final String cmakeBin,
            final String generator, final String preloadScript,
            final String preset, final FilePath theSourceDir,
            final String buildType, final String cmakeArgs) {
        ArgumentListBuilder args = new ArgumentListBuilder();

        args.add(cmakeBin);
        if (preset != null) {
            args.add("--preset").add(preset); //$NON-NLS-1$
        }
        if (generator != null) {
            args.add("-G").add(generator); //$NON-NLS-1$
        }
        if (preloadScript != null) {
            args.add("-C").add(preloadScript); //$NON-NLS-1$
        }
        if (buildType != null && !isMultiConfigGenerator(generator)) {
            args.add("-D").add("CMAKE_BUILD_TYPE=" + buildType); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (cmakeArgs != null) {
            args.addTokenized(cmakeArgs);
        }
        if (theSourceDir != null) {
            args.add(theSourceDir.getRemote());
        }
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
     * {@code cmake --build}, supporting {@code --config <buildType>} and
     * {@code --preset <preset>}.
     *
     * @param cmakeBin
     *            the name of the cmake tool binary, either as an absolute or
     *            relative file system path.
     * @param theBuildDir
     *            the build directory path
     * @param buildType
     *            optional build configuration passed as {@code --config <buildType>}
     * @param preset
     *            optional CMake build preset passed as {@code --preset <preset>}
     * @param toolArgs
     *            additional build tool arguments, separated by spaces to pass to
     *            cmake or {@code null}
     * @return the argument list, never {@code null}
     */
    static ArgumentListBuilder buildBuildToolCallWithCmake(
            final String cmakeBin, FilePath theBuildDir, final String buildType,
            final String preset, String... toolArgs) {
        ArgumentListBuilder args = new ArgumentListBuilder();

        args.add(cmakeBin);
        args.add("--build"); //$NON-NLS-1$

        boolean argsContainPreset = false;
        boolean argsContainConfig = false;
        if (toolArgs != null) {
            for (String arg : toolArgs) {
                if ("--preset".equals(arg) //$NON-NLS-1$
                        || (arg != null && arg.startsWith("--preset="))) { //$NON-NLS-1$
                    argsContainPreset = true;
                }
                if ("--config".equals(arg)) { //$NON-NLS-1$
                    argsContainConfig = true;
                }
            }
        }

        if (preset != null) {
            args.add("--preset").add(preset); //$NON-NLS-1$
        } else if (!argsContainPreset && theBuildDir != null) {
            args.add(theBuildDir.getRemote());
        }

        if (buildType != null && !argsContainConfig) {
            args.add("--config").add(buildType); //$NON-NLS-1$
        }

        if (toolArgs != null) {
            args.add(toolArgs);
        }
        return args;
    }

}
