package hudson.plugins.cmake;

import java.io.IOException;
import java.util.Iterator;

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
import hudson.model.ModelObject;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

/**
 * Provides a build step that allows to invoke selected tools of the cmake-suite
 * ({@code cmake}, {@code cpack} and {@code ctest}) with arbitrary arguments.
 *
 * @author Martin Weber
 */
public class CToolBuilder extends AbstractCmakeBuilder {
    /** the ID of the tool in the CMake-suite to invoke {@link Tool}. */
    private String toolId;

    /**
     * Exit codes of the tool that indicate a failure but should be ignored,
     * thus causing the build to proceed.<br>
     */
    private String ignoredExitCodes;

    /**
     * Parsed and cached exit codes to ignore.
     */
    private transient IntSet ignoredExitCodesParsed;

    /**
     * Minimal constructor.
     *
     * @param installationName
     *            the name of the cmake tool installation from the global config
     *            page.
     */
    @DataBoundConstructor
    public CToolBuilder(String installationName) {
        super(installationName);
        setToolId("cmake");
    }

    @DataBoundSetter
    public void setToolId(String toolId) {
        this.toolId = Util.fixNull(toolId);
    }

    public String getToolId() {
        return toolId;
    }

    /**
     * Gets the exit codes of the tool that indicate a failure but should be
     * ignored, thus causing the build to proceed.
     *
     * @return the ignoredExitCodes property value or <code>null</code>
     */
    public String getIgnoredExitCodes() {
        return ignoredExitCodes;
    }

    @DataBoundSetter
    public void setIgnoredExitCodes(String ignoredExitCodes) {
        this.ignoredExitCodes = Util.fixEmptyAndTrim(ignoredExitCodes);

    }

    @DataBoundSetter
    public void setWorkingDir(String buildDir) {
        // because of: error: @DataBoundConstructor may not be used on an
        // abstract class
        super.setWorkingDir(buildDir);
    }

    public String getWorkingDir() {
        // because of: error: @DataBoundConstructor may not be used on an
        // abstract class
        return super.getWorkingDir();
    }

    @DataBoundSetter
    public void setArguments(String arguments) {
        // because of: error: @DataBoundConstructor may not be used on an
        // abstract class
        super.setArguments(arguments);
    }

    public String getArguments() {
        // because of: error: @DataBoundConstructor may not be used on an
        // abstract class
        return super.getArguments();
    }

    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) throws InterruptedException, IOException {

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

        final String cmakeBin = installToUse.getCmakeExe();
        // strip off the last path segment (usually 'cmake')
        String bindir;
        {
            int idx;
            if (launcher.isUnix()) {
                idx = cmakeBin.lastIndexOf('/');
            } else {
                if ((idx = cmakeBin.lastIndexOf('\\')) != -1
                        || (idx = cmakeBin.lastIndexOf('/')) != -1)
                    ;
            }
            if (idx >= 0) {
                bindir = cmakeBin.substring(0, idx + 1);
            } else {
                bindir = "";
            }
        }

        try {
            /* Determine remote working directory path. Create it. */
            final FilePath workSpace = build.getWorkspace();
            final String workDir = getWorkingDir();
            final FilePath theWorkDir = makeRemotePath(workSpace,
                    Util.replaceMacro(workDir, envs));
            if (workDir != null) {
                theWorkDir.mkdirs();
            }

            /* Invoke tool in working dir */
            ArgumentListBuilder cmakeCall = LaunchUtils.buildCommandline(bindir + getToolId(),
                    Util.replaceMacro(getArguments(), envs));
            final int exitCode;
            if (0 != (exitCode = launcher.launch().pwd(theWorkDir).envs(envs)
                    .stdout(listener).cmds(cmakeCall).join())) {
                // should this failure be ignored?
                if (ignoredExitCodes != null) {
                    if (ignoredExitCodesParsed == null) {
                        // parse and cache
                        final IntSet ints = new IntSet();
                        ints.setValues(ignoredExitCodes);
                        ignoredExitCodesParsed = ints;
                    }
                    for (Iterator<Integer> iter = ignoredExitCodesParsed
                            .iterator(); iter.hasNext();) {
                        if (exitCode == iter.next()) {
                            // ignore this failure exit code
                            listener.getLogger().printf(
                                    "%1s exited with failure code %2$s, ignored.%n",
                                    getToolId(), exitCode);
                            return true; // no failure
                        }
                    }
                    // invocation failed, not ignored
                    listener.getLogger().printf(
                            "%1s exited with failure code %2$s%n", getToolId(),
                            exitCode);
                }
                return false; // invocation failed
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
     * Descriptor for {@link CToolBuilder}. Used as a singleton. The class is
     * marked as public so that it can be accessed from views.
     */
    @Extension
    public static final class DescriptorImpl
            extends AbstractCmakeBuilder.DescriptorImpl {

        private static Tool[] tools = { new Tool("cmake", "CMake"),
                new Tool("cpack", "CPack"), new Tool("ctest", "CTest") };

        public DescriptorImpl() {
            super(CToolBuilder.class);
            load();
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "CMake/CPack/CTest execution";
        }

        public ListBoxModel doFillToolIdItems() {
            ListBoxModel items = new ListBoxModel();
            for (Tool tool : tools) {
                items.add(tool.getDisplayName(), tool.getId());
            }
            return items;
        }

        /**
         * Performs on-the-fly validation of the form field 'ignoredExitCodes'.
         *
         * @param value
         */
        public FormValidation doCheckIgnoredExitCodes(
                @AncestorInPath AbstractProject<?, ?> project,
                @QueryParameter final String value)
                throws IOException, ServletException {
            try {
                new IntSet().setValues(Util.fixEmptyAndTrim(value));
            } catch (IllegalArgumentException iae) {
                return FormValidation.error(iae.getLocalizedMessage());
            }
            return FormValidation.ok();
        }
    } // DescriptorImpl

    /**
     * Represents one of the tools of the CMake-suite.
     *
     * @author Martin Weber
     */
    private static class Tool implements ModelObject {
        private final String id;
        private final String displayName;

        /**
         * @param id
         * @param displayName
         */
        public Tool(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public String getId() {
            return id;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }
    }
}
