package hudson.plugins.cmake;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Environment;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;

/**
 * Executes <tt>cmake</tt> as a build step.
 *
 * @author Volker Kaiser (initial implementation)
 * @author Martin Weber
 */
public class CmakeBuilder extends AbstractCmakeBuilder {

    /**
     * the key for the build variable that holds the build tool that the
     * build-scripts have been generated for (e.g. /usr/bin/make or
     * /usr/bin/ninja)
     */
    public static final String ENV_VAR_NAME_CMAKE_BUILD_TOOL = "CMAKE_BUILD_TOOL"; //$NON-NLS-1$

    /**
     * the name of cmake´s buildscript generator or {@code null} if the default
     * generator should be used
     */
    private String generator;
    private String sourceDir;
    private String buildType;
    private String preloadScript;
    private boolean cleanBuild;

    // for backward compatibility with < 2.4.0
    // see
    // https://wiki.jenkins-ci.org/display/JENKINS/Hint+on+retaining+backward+compatibility
    private transient String buildDir;
    private transient String cmakeArgs;

    private List<BuildToolStep> toolSteps;

    /**
     * Minimal constructor.
     *
     * @param installationName
     *            the name of the cmake tool installation from the global config
     *            page.
     */
    @DataBoundConstructor
    public CmakeBuilder(String installationName) {
        super(installationName);
    }

    /**
     * Old constructor.
     *
     * @param installationName
     *            the name of the cmake tool installation from the global config
     *            page.
     * @param generator
     *            the name of cmake´s buildscript generator. May be empty but
     *            not {@code null}
     * @deprecated to minimize number of mandatory field values.
     */
    @Deprecated
    public CmakeBuilder(String installationName, String generator) {
        super(installationName);
        setGenerator(generator);
    }

    // for backward compatibility with < 2.4.
    protected Object readResolve() {
        // convert to new format
        if (buildDir != null) {
            super.setWorkingDir(buildDir);
        }
        if (cmakeArgs != null) {
            super.setArguments(cmakeArgs);
        }
        return this;
    }

    /**
     * Sets the name of the build-script generator.
     *
     * @param generator
     *            the name of cmake´s build-script generator or {@code null} or
     *            empty if the default generator should be used
     */
    @DataBoundSetter
    public void setGenerator(String generator) {
        this.generator = Util.fixEmptyAndTrim(generator);
    }

    public String getGenerator() {
        return generator;
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
        // because of: error: @DataBoundConstructor may not be used on an
        // abstract class
        super.setWorkingDir(buildDir);
    }

    public String getBuildDir() {
        // because of: error: @DataBoundConstructor may not be used on an
        // abstract class
        return super.getWorkingDir();
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
        // because of: error: @DataBoundConstructor may not be used on an
        // abstract class
        super.setArguments(cmakeArgs);
    }

    public String getCmakeArgs() {
        // because of: error: @DataBoundConstructor may not be used on an
        // abstract class
        return super.getArguments();
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

    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) throws InterruptedException, IOException {
        CmakeTool installToUse = getSelectedInstallation();
        // Raise an error if the cmake installation isn't found
        if (installToUse == null) {
            listener.fatalError(Messages.getString("No_installation_selected")); //$NON-NLS-1$
            return false;
        }
        final EnvVars envs = build.getEnvironment(listener);
        envs.overrideAll(build.getBuildVariables());

        // Get the CMake version for this node, installing it if necessary
        installToUse = (CmakeTool) installToUse.translate(build, listener);

        final String cmakeBin = installToUse
                .getAbsoluteCommand(build.getBuiltOn(), "cmake"); //$NON-NLS-1$
        final FilePath workSpace = build.getWorkspace();
        try {
            /*
             * Determine remote build directory path. Clean it, if requested.
             * Create it.
             */
            final String buildDir = getWorkingDir();
            FilePath theBuildDir = LaunchUtils.makeRemotePath(workSpace,
                    Util.replaceMacro(buildDir, envs));
            if (buildDir != null) {
                if (this.cleanBuild && !buildDir.equals(sourceDir)) {
                    // avoid deleting source dir
                    listener.getLogger().format(
                            Messages.getString("Cleaning_build_dir"), theBuildDir.getRemote()); //$NON-NLS-1$
                    theBuildDir.deleteRecursive();
                }
                theBuildDir.mkdirs();
            }

            /* Invoke cmake in build dir */
            FilePath theSourceDir = LaunchUtils.makeRemotePath(workSpace,
                    Util.replaceMacro(sourceDir, envs));
            ArgumentListBuilder cmakeCall = LaunchUtils.buildCMakeCall(cmakeBin,
                    Util.replaceMacro(getGenerator(), envs),
                    Util.replaceMacro(this.preloadScript, envs), theSourceDir,
                    Util.replaceMacro(this.buildType, envs),
                    Util.replaceMacro(getCmakeArgs(), envs));
            // invoke cmake
            if (0 != launcher.launch().pwd(theBuildDir).envs(envs)
                    .stdout(listener).cmds(cmakeCall).join()) {
                return false; // invocation failed
            }

            /* parse CMakeCache.txt to get the actual build tool */
            FilePath cacheFile = theBuildDir.child("CMakeCache.txt"); //$NON-NLS-1$
            String buildTool = cacheFile.act(new BuildToolEntryParser());
            if (buildTool == null) {
                listener.getLogger().printf(
                        Messages.getString("Failed_to_get_var_value"), //$NON-NLS-1$
                        CmakeBuilder.ENV_VAR_NAME_CMAKE_BUILD_TOOL,
                        cacheFile.getRemote());
            } else {
                // add CMAKE_BUILD_TOOL variable for toolSteps
                envs.put(CmakeBuilder.ENV_VAR_NAME_CMAKE_BUILD_TOOL, buildTool);
                // add CMAKE_BUILD_TOOL to env for other build-steps
                EnvVars exportedEnvVars = new EnvVars();
                exportedEnvVars.put(CmakeBuilder.ENV_VAR_NAME_CMAKE_BUILD_TOOL,
                        buildTool);
                // export environment
                build.getEnvironments()
                        .add(Environment.create(exportedEnvVars));
            }

            /* invoke each build tool step in build dir */
            if( toolSteps != null) {
                for (BuildToolStep step : toolSteps) {
                    ArgumentListBuilder toolCall;
                    if (!step.getWithCmake()) {
                        // invoke directly
                        // if buildTool == null, let the unexpanded macro show up in
                        // the log
                        final String buildToolMacro = Util.replaceMacro("${" //$NON-NLS-1$
                                + CmakeBuilder.ENV_VAR_NAME_CMAKE_BUILD_TOOL + "}", //$NON-NLS-1$
                                envs);
                        toolCall = LaunchUtils.buildBuildToolCall(buildToolMacro,
                                step.getCommandArguments(envs));
                    } else {
                        // invoke through 'cmake --build <dir>'
                        toolCall = LaunchUtils.buildBuildToolCallWithCmake(cmakeBin,
                                theBuildDir, step.getCommandArguments(envs));
                    }
                    final EnvVars stepEnv = new EnvVars(envs)
                            .overrideAll(step.getEnvironmentVars(envs, listener));
                    if (0 != launcher.launch().pwd(theBuildDir).envs(stepEnv)
                            .stdout(listener).cmds(toolCall).join()) {
                        return false; // invocation failed
                    }
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
    public static final class DescriptorImpl
            extends AbstractCmakeBuilder.DescriptorImpl {

        public DescriptorImpl() {
            super(CmakeBuilder.class);
            load();
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return Messages.getString("CmakeBuilder.Descriptor.DisplayName"); //$NON-NLS-1$
        }

        /**
         * Performs on-the-fly validation of the form field 'sourceDir'.
         *
         * @param value
         */
        public FormValidation doCheckSourceDir(
                @AncestorInPath AbstractProject<?, ?> project,
                @QueryParameter final String value)
                throws IOException, ServletException {
            FilePath ws = project.getSomeWorkspace();
            if (ws == null)
                return FormValidation.ok();
            return ws.validateRelativePath(value, false, false);
        }

    } // DescriptorImpl
}
