package hudson.plugins.cmake;

import hudson.CopyOnWrite;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.Environment;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/**
 * Executes <tt>cmake</tt> as a build step.
 *
 * @author Volker Kaiser (initial implementation)
 * @author Martin Weber
 */
public class CmakeBuilder extends Builder {

    /**
     * the key for the build variable that holds the build tool that the
     * build-scripts have been generated for (e.g. /usr/bin/make or
     * /usr/bin/ninja)
     */
    public static final String ENV_VAR_NAME_CMAKE_BUILD_TOOL = "CMAKE_BUILD_TOOL";

    /** the name of the cmake tool installation to use for this build step */
    private String installationName;
    /** allowed to be empty but not {@code null} */
    private String generator;
    private String sourceDir;
    private String buildDir;
    private String buildType;
    private String preloadScript;
    private String cmakeArgs;
    private boolean cleanBuild;

    private List<BuildToolStep> toolSteps = new ArrayList<BuildToolStep>(0);

    /**
     * Minimal constructor.
     *
     * @param installationName
     *            the name of the cmake tool installation from the global config
     *            page.
     * @param generator
     *            the name of cmake´s buildscript generator. May be empty but
     *            not {@code null}
     */
    @DataBoundConstructor
    public CmakeBuilder(String installationName, String generator) {
        this.installationName = Util.fixEmptyAndTrim(installationName);
        this.generator = Util.fixNull(generator);
    }

    /** Gets the name of the cmake installation to use for this build step */
    public String getInstallationName() {
        return this.installationName;
    }

    public String getGenerator() {
        return this.generator;
    }

    @DataBoundSetter
    public void setSourceDir(String sourceDir) {
        this.sourceDir = Util.fixEmptyAndTrim(sourceDir);
    }

    public String getSourceDir() {
        return this.sourceDir;
    }

    @DataBoundSetter
    public void setBuildDir(String buildDir) {
        this.buildDir = Util.fixEmptyAndTrim(buildDir);
    }

    public String getBuildDir() {
        return this.buildDir;
    }

    @DataBoundSetter
    public void setBuildType(String buildType) {
        this.buildType = Util.fixEmptyAndTrim(buildType);
    }

    public String getBuildType() {
        return this.buildType;
    }

    @DataBoundSetter
    public void setCleanBuild(boolean cleanBuild) {
        this.cleanBuild = cleanBuild;
    }

    public boolean getCleanBuild() {
        return this.cleanBuild;
    }

    @DataBoundSetter
    public void setPreloadScript(String preloadScript) {
        this.preloadScript = Util.fixEmptyAndTrim(preloadScript);
    }

    public String getPreloadScript() {
        return this.preloadScript;
    }

    @DataBoundSetter
    public void setCmakeArgs(String cmakeArgs) {
        this.cmakeArgs = Util.fixEmptyAndTrim(cmakeArgs);
    }

    public String getCmakeArgs() {
        return this.cmakeArgs;
    }

    /**
     * Sets the toolSteps property.
     */
    @DataBoundSetter
    public void setSteps(List<BuildToolStep> toolSteps) {
        this.toolSteps = toolSteps;
    }

    /**
     * Gets the toolSteps property.
     *
     * @return the current toolSteps property.
     */
    public List<BuildToolStep> getSteps() {
        return toolSteps;
    }

    /**
     * Constructs a directory under the workspace on the slave.
     *
     * @param path
     *            the directory´s relative path {@code null} for no op
     *
     * @return the full path of the directoy on the remote machine.
     */
    private static FilePath makeRemotePath(FilePath workSpace, String path) {
        if (path == null) {
            return workSpace;
        }
        FilePath file = workSpace.child(path);
        return file;
    }

    /**
     * Finds the cmake tool installation to use for this build among all
     * installations configured in the Jenkins administration
     *
     * @return selected CMake installation or {@code null} if none could be
     *         found
     */
    private CmakeTool getSelectedInstallation() {
        CmakeTool.DescriptorImpl descriptor = (CmakeTool.DescriptorImpl) Jenkins
                .getInstance().getDescriptor(CmakeTool.class);
        for (CmakeTool i : descriptor.getInstallations()) {
            if (installationName != null
                    && i.getName().equals(installationName))
                return i;
        }

        return null;
    }

    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) throws InterruptedException, IOException {
        EnvVars exportedEnvVars = new EnvVars();

        CmakeTool installToUse = getSelectedInstallation();
        // Raise an error if the cmake installation isn't found
        if (installToUse == null) {
            listener.fatalError("There is no CMake installation selected."
                    + " Please review the build step configuration.");
            return false;
        }
        final EnvVars envs = build.getEnvironment(listener);
        envs.overrideAll(build.getBuildVariables());

        // Get the CMake version for this node, installing it if necessary
        installToUse = (CmakeTool) installToUse.translate(build, listener);
        // add CMAKEROOT/bin to PATH for sub-processes, if autoinstalled
        installToUse.buildEnvVars(exportedEnvVars);

        final String cmakeBin = installToUse.getCmakeExe();
        final FilePath workSpace = build.getWorkspace();
        try {
            /*
             * Determine remote build directory path. Clean it, if requested.
             * Create it.
             */
            FilePath theBuildDir = makeRemotePath(workSpace,
                    Util.replaceMacro(buildDir, envs));
            if (buildDir != null) {
                if (this.cleanBuild && !buildDir.equals(sourceDir)) {
                    // avoid deleting source dir
                    listener.getLogger().println(
                            "Cleaning build dir... " + theBuildDir.getRemote());
                    theBuildDir.deleteRecursive();
                }
                theBuildDir.mkdirs();
            }

            /* Invoke cmake in build dir */
            FilePath theSourceDir = makeRemotePath(workSpace,
                    Util.replaceMacro(sourceDir, envs));
            ArgumentListBuilder cmakeCall = buildCMakeCall(cmakeBin,
                    Util.replaceMacro(this.generator, envs),
                    Util.replaceMacro(this.preloadScript, envs), theSourceDir,
                    Util.replaceMacro(this.buildType, envs),
                    Util.replaceMacro(cmakeArgs, envs));
            // invoke cmake
            if (0 != launcher.launch().pwd(theBuildDir).envs(envs)
                    .stdout(listener).cmds(cmakeCall).join()) {
                return false; // invokation failed
            }

            /* parse CMakeCache.txt to get the actual build tool */
            FilePath cacheFile = theBuildDir.child("CMakeCache.txt");
            String buildTool = cacheFile.act(new BuildToolEntryParser());
            if (buildTool == null) {
                listener.getLogger()
                        .printf("WARNING: Failed to get value for variable `%1s` from %2$s.",
                                CmakeBuilder.ENV_VAR_NAME_CMAKE_BUILD_TOOL,
                                cacheFile.getRemote());
            }
            // add variable
            exportedEnvVars.putIfNotNull(
                    CmakeBuilder.ENV_VAR_NAME_CMAKE_BUILD_TOOL, buildTool);
            // export our environment
            build.getEnvironments().add(Environment.create(exportedEnvVars));

            /* invoke each build tool step in build dir */
            for (BuildToolStep step : toolSteps) {
                ArgumentListBuilder toolCall;
                if (!step.getWithCmake()) {
                    // invoke directly
                    // if buildTool == null, let the unexpanded macro show up in
                    // the log
                    final String buildToolMacro = Util.replaceMacro("${"
                            + CmakeBuilder.ENV_VAR_NAME_CMAKE_BUILD_TOOL + "}",
                            exportedEnvVars);
                    toolCall = buildBuildToolCall(buildToolMacro,
                            step.getCommandArguments(envs));
                } else {
                    // invoke through 'cmake --build <dir>'
                    toolCall = buildBuildToolCallWithCmake(cmakeBin,
                            theBuildDir, step.getCommandArguments(envs));
                }
                if (0 != launcher.launch().pwd(theBuildDir)
                        .envs(step.getEnvironmentVars(envs, listener))
                        .stdout(listener).cmds(toolCall).join()) {
                    return false; // invokation failed
                }
            }
        } catch (IOException e) {
            Util.displayIOException(e, listener);
            listener.error(e.getLocalizedMessage());
            return false;
        }
        return true;
    }

    /**
     * Constructs the command line to invoke cmake.
     *
     * @param cmakeBin
     *            the name of the cmake binary, either as an absolute or
     *            relative file system path.
     * @param generator
     *            the name of the build-script generator
     * @param preloadScript
     *            name of the pre-load a script to populate the cache or
     *            {@code null}
     * @param theSourceDir
     *            source directory, must not be {@code null}
     * @param buildType
     *            build type argument for cmake or {@code null} to pass none
     * @param cmakeArgs
     *            addional arguments, separated by spaces to pass to cmake or
     *            {@code null}
     * @return the argument list, never {@code null}
     */
    private static ArgumentListBuilder buildCMakeCall(final String cmakeBin,
            final String generator, final String preloadScript,
            final FilePath theSourceDir, final String buildType,
            final String cmakeArgs) {
        ArgumentListBuilder args = new ArgumentListBuilder();

        args.add(cmakeBin);
        args.add("-G").add(generator);
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
     *            addional arguments, separated by spaces to pass to cmake or
     *            {@code null}
     * @return the argument list, never {@code null}
     */
    private static ArgumentListBuilder buildBuildToolCall(final String toolBin,
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
     *            addional build tool arguments, separated by spaces to pass to
     *            cmake or {@code null}
     * @return the argument list, never {@code null}
     */
    private static ArgumentListBuilder buildBuildToolCallWithCmake(
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

    /**
     * Overridden for better type safety.
     */
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    // //////////////////////////////////////////////////////////////////
    // inner classes
    // //////////////////////////////////////////////////////////////////
    /**
     * Descriptor for {@link CmakeBuilder}. Used as a singleton. The class is
     * marked as public so that it can be accessed from views.
     */
    @Extension
    public static final class DescriptorImpl extends
            BuildStepDescriptor<Builder> {
        /**
         * the cmake tool installations
         */
        @CopyOnWrite
        private volatile CmakeTool[] installations = new CmakeTool[0];

        public DescriptorImpl() {
            super(CmakeBuilder.class);
            load();
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "CMake Build";
        }

        /**
         * Determines the values of the Cmake installation drop-down list box.
         */
        public ListBoxModel doFillInstallationNameItems() {
            ListBoxModel items = new ListBoxModel();
            CmakeTool.DescriptorImpl descriptor = (CmakeTool.DescriptorImpl) Jenkins
                    .getInstance().getDescriptor(CmakeTool.class);
            for (CmakeTool inst : descriptor.getInstallations()) {
                items.add(inst.getName());// , "" + inst.getPid());
            }
            return items;
        }

        /**
         * Performs on-the-fly validation of the form field 'generator'.
         *
         * @param value
         */
        public FormValidation doCheckGenerator(
                @QueryParameter final String value) throws IOException,
                ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Please set a generator name");
            }
            return FormValidation.ok();
        }

        /**
         * Performs on-the-fly validation of the form field 'sourceDir'.
         *
         * @param value
         */
        public FormValidation doCheckSourceDir(
                @AncestorInPath AbstractProject<?, ?> project,
                @QueryParameter final String value) throws IOException,
                ServletException {
            FilePath ws = project.getSomeWorkspace();
            if (ws == null)
                return FormValidation.ok();
            return ws.validateRelativePath(value, false, false);
        }

        @Override
        public boolean isApplicable(
                @SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
            // this builder can be used with all kinds of project types
            return true;
        }

    } // DescriptorImpl
}
