package hudson.plugins.cmake;

import hudson.CopyOnWrite;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.IOException;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/**
 * Executes <tt>cmake</tt> as a build step.
 *
 * @author Volker Kaiser
 * @author Martin Weber
 */
public class CmakeBuilder extends Builder {

    private String sourceDir;
    private String buildDir;
    private String installDir;
    private String generator;
    private String makeCommand;
    private String buildType;
    private String installCommand;
    private String preloadScript;
    private String cmakeArgs;
    private boolean cleanBuild;
    private boolean cleanInstallDir;
    /** the name of the cmake installation to use for this build step */
    private String installationName;

    @DataBoundConstructor
    public CmakeBuilder(String installationName, String generator,
            String sourceDir, String buildDir, String makeCommand) {
        this.installationName = Util.fixEmptyAndTrim(installationName);
        this.generator = Util.fixEmptyAndTrim(generator);
        this.sourceDir = Util.fixEmptyAndTrim(sourceDir);
        this.buildDir = Util.fixEmptyAndTrim(buildDir);
        this.installDir = Util.fixEmptyAndTrim(installDir);
        this.makeCommand = Util.fixEmptyAndTrim(makeCommand);
    }

    /** Gets the name of the cmake installation to use for this build step */
    public String getInstallationName() {
        return this.installationName;
    }

    public String getGenerator() {
        return this.generator;
    }

    public String getSourceDir() {
        return this.sourceDir;
    }

    public String getBuildDir() {
        return this.buildDir;
    }

    public String getMakeCommand() {
        return this.makeCommand;
    }

    @DataBoundSetter
    public void setInstallDir(String installDir) {
        this.installDir = Util.fixEmptyAndTrim(installDir);
    }

    public String getInstallDir() {
        return this.installDir;
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
    public void setCleanInstallDir(boolean cleanInstallDir) {
        this.cleanInstallDir = cleanInstallDir;
    }

    public boolean getCleanInstallDir() {
        return this.cleanInstallDir;
    }

    @DataBoundSetter
    public void setInstallCommand(String installCommand) {
        this.installCommand = Util.fixEmptyAndTrim(installCommand);
    }

    public String getInstallCommand() {
        return this.installCommand;
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

        CmakeTool installToUse = getSelectedInstallation();

        // Raise an error if the doxygen installation isn't found
        if (installToUse == null) {
            listener.getLogger().println(
                    "There is no CMake installation selected."
                            + " Please review the build step configuration.");
            return false;
        }
        final EnvVars envs = build.getEnvironment(listener);
        envs.overrideAll(build.getBuildVariables());
        // Get the CMake version for this node, installing it if necessary
        installToUse = installToUse.forNode(Computer.currentComputer()
                .getNode(), listener);
        installToUse = installToUse.forEnvironment(envs);
        final String cmakeBin = installToUse.getHome();

        final FilePath workSpace = build.getWorkspace();

        try {
            /*
             * Determine remote directory paths. Clean each, if requested.
             * Create each.
             */
            FilePath theSourceDir;
            FilePath theBuildDir;
            FilePath theInstallDir = null;

            theSourceDir = makeRemotePath(workSpace,
                    Util.replaceMacro(sourceDir, envs));

            theBuildDir = makeRemotePath(workSpace,
                    Util.replaceMacro(buildDir, envs));
            if (buildDir != null) {
                if (this.cleanBuild) {
                    listener.getLogger().println(
                            "Cleaning build dir... " + theBuildDir.getRemote());
                    theBuildDir.deleteRecursive();
                }
                theBuildDir.mkdirs();
            }

            if (installDir != null) {
                theInstallDir = makeRemotePath(workSpace,
                        Util.replaceMacro(installDir, envs));
                if (this.cleanInstallDir) {
                    listener.getLogger().println(
                            "Cleaning install dir... "
                                    + theInstallDir.getRemote());
                    theInstallDir.deleteRecursive();
                }
                theInstallDir.mkdirs();
            }

            listener.getLogger().println(
                    "Build   dir  : " + theBuildDir.getRemote());
            if (theInstallDir != null)
                listener.getLogger().println(
                        "Install dir  : " + theInstallDir.getRemote());

            /* Invoke cmake in build dir */
            ArgumentListBuilder cmakeCall = buildCMakeCall(cmakeBin,
                    Util.replaceMacro(this.generator, envs),
                    Util.replaceMacro(this.preloadScript, envs), theSourceDir,
                    theInstallDir, Util.replaceMacro(this.buildType, envs),
                    Util.replaceMacro(cmakeArgs, envs));
            // invoke cmake
            if (0 != launcher.launch().pwd(theBuildDir).envs(envs)
                    .stdout(listener).cmds(cmakeCall).join()) {
                return false; // invokation failed
            }

            /* invoke make in build dir */
            if (0 != launcher
                    .launch()
                    .pwd(theBuildDir)
                    .envs(envs)
                    .stdout(listener)
                    .cmdAsSingleString(
                            Util.replaceMacro(getMakeCommand(), envs)).join()) {
                return false; // invokation failed
            }

            /* invoke 'make install' in build dir */
            if (theInstallDir != null) {
                if (0 != launcher
                        .launch()
                        .pwd(theBuildDir)
                        .envs(envs)
                        .stdout(listener)
                        .cmdAsSingleString(
                                Util.replaceMacro(getInstallCommand(), envs))
                        .join()) {
                    return false; // invokation failed
                }
            }
        } catch (IOException e) {
            Util.displayIOException(e, listener);
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
     * @param theInstallDir
     *            install directory or {@code null}
     * @param buildType
     *            build type argument for cmake or {@code null} to pass none
     * @param cmakeArgs
     *            addional arguments, separated by spaces to pass to cmake or
     *            {@code null}
     * @return the argument list, never {@code null}
     */
    private ArgumentListBuilder buildCMakeCall(final String cmakeBin,
            final String generator, final String preloadScript,
            final FilePath theSourceDir, final FilePath theInstallDir,
            final String buildType, final String cmakeArgs) {
        ArgumentListBuilder args = new ArgumentListBuilder();

        args.add(cmakeBin);
        args.add("-G").add(generator);
        if (preloadScript != null) {
            args.add("-C").add(preloadScript);
        }
        if (buildType != null) {
            args.add("-D").add("CMAKE_BUILD_TYPE=" + buildType);
        }
        if (theInstallDir != null) {
            args.add("-D").add(
                    "CMAKE_INSTALL_PREFIX=" + theInstallDir.getRemote());
        }
        if (cmakeArgs != null) {
            args.addTokenized(cmakeArgs);
        }
        args.add(theSourceDir.getRemote());
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

        public FormValidation doCheckSourceDir(
                @AncestorInPath AbstractProject<?, ?> project,
                @QueryParameter final String value) throws IOException,
                ServletException {
            FilePath ws = project.getSomeWorkspace();
            if (ws == null)
                return FormValidation.ok();
            return ws.validateRelativePath(value, true, false);
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         */
        public FormValidation doCheckBuildDir(@QueryParameter final String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a build directory");
            return FormValidation.ok();
        }

        /**
         * Performs on-the-fly validation of the form field 'makeCommand'.
         *
         * @param value
         */
        public FormValidation doCheckMakeCommand(
                @QueryParameter final String value) throws IOException,
                ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Please set make command");
            }
            return FormValidation.validateExecutable(value);
        }

        @Override
        public boolean isApplicable(
                @SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
            // Indicates that this builder can be used with all kinds of project
            // types
            return true;
        }

    } // DescriptorImpl
}
