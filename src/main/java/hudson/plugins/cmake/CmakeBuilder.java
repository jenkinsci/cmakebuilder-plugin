package hudson.plugins.cmake;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;

import java.io.IOException;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Executes <tt>cmake</tt> as the build process.
 *
 *
 * @author Volker Kaiser
 * @author Martin Weber
 */
public class CmakeBuilder extends Builder {

    private static final String CMAKE_EXECUTABLE = "CMAKE_EXECUTABLE";

    private static final String CMAKE = "cmake";

    private String sourceDir;
    private String buildDir;
    private String installDir;
    private String buildType;
    private String generator;
    private String makeCommand;
    private String installCommand;
    private String preloadScript;
    private String cmakeArgs;
    private String projectCmakePath;
    private boolean cleanBuild;
    private boolean cleanInstallDir;

    @DataBoundConstructor
    public CmakeBuilder(String sourceDir, String buildDir, String installDir,
            String buildType, boolean cleanBuild, boolean cleanInstallDir,
            String generator, String makeCommand, String installCommand,
            String preloadScript, String cmakeArgs, String projectCmakePath) {
        this.sourceDir = Util.fixEmptyAndTrim(sourceDir);
        this.buildDir = Util.fixEmptyAndTrim(buildDir);
        this.installDir = Util.fixEmptyAndTrim(installDir);
        this.buildType = Util.fixEmptyAndTrim(buildType);
        this.cleanBuild = cleanBuild;
        this.cleanInstallDir = cleanInstallDir;
        this.generator = Util.fixEmptyAndTrim(generator);
        this.makeCommand = Util.fixEmptyAndTrim(makeCommand);
        this.installCommand = Util.fixEmptyAndTrim(installCommand);
        this.cmakeArgs = Util.fixEmptyAndTrim(cmakeArgs);
        this.projectCmakePath = Util.fixEmptyAndTrim(projectCmakePath);
        this.preloadScript = Util.fixEmptyAndTrim(preloadScript);
    }

    public String getSourceDir() {
        return this.sourceDir;
    }

    public String getBuildDir() {
        return this.buildDir;
    }

    public String getInstallDir() {
        return this.installDir;
    }

    public String getBuildType() {
        return this.buildType;
    }

    public boolean getCleanBuild() {
        return this.cleanBuild;
    }

    public boolean getCleanInstallDir() {
        return this.cleanInstallDir;
    }

    public String getGenerator() {
        return this.generator;
    }

    public String getMakeCommand() {
        return this.makeCommand;
    }

    public String getInstallCommand() {
        return this.installCommand;
    }

    public String getPreloadScript() {
        return this.preloadScript;
    }

    public String getCmakeArgs() {
        return this.cmakeArgs;
    }

    public String getProjectCmakePath() {
        return this.projectCmakePath;
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

    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) throws InterruptedException, IOException {

        final EnvVars envs = build.getEnvironment(listener);
        envs.overrideAll(build.getBuildVariables());

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
            final String cmakeBin = getCmake(envs);
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
     * Determines the name of the cmake executable to invoke. Macro expansion
     * takes place.
     *
     * @param envs
     *            the build environment for macro expansion.
     */
    private String getCmake(EnvVars envs) {
        // determine command name...
        String cmakeBin = CMAKE; // built in default

        // NOTE: fixEmptyAndTrim() is called for backward compatiblity with
        // existing jobs only (pre 1.11)
        // override with global setting, if any..
        String cmakePath = Util.fixEmptyAndTrim(getDescriptor().cmakePath());
        if (cmakePath != null) {
            cmakeBin = cmakePath;
        }
        // override with job specific setting, if any..
        cmakePath = Util.fixEmptyAndTrim(this.getProjectCmakePath());
        if (cmakePath != null) {
            cmakeBin = Util.replaceMacro(cmakePath, envs);
        }
        // what is this for?
        if (envs.containsKey(CMAKE_EXECUTABLE)) {
            cmakeBin = envs.get(CMAKE_EXECUTABLE);
        }

        return cmakeBin;
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

    /*
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
         * To persist global configuration information, simply store it in a
         * field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private String cmakePath;

        public DescriptorImpl() {
            super(CmakeBuilder.class);
            load();
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

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "CMake Build";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject o)
                throws FormException {
            // to persist global configuration information,
            // set that to properties and call save().
            cmakePath = o.getString("cmakePath");
            save();
            return super.configure(req, o);
        }

        public String cmakePath() {
            return cmakePath;
        }

        @Override
        public Builder newInstance(StaplerRequest req, JSONObject formData)
                throws FormException {
            return req.bindJSON(CmakeBuilder.class, formData);
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
