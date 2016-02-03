package hudson.plugins.cmake;

import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.ModelObject;
import hudson.util.ArgumentListBuilder;
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
        final FilePath workSpace = build.getWorkspace();
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
            final String workDir = getWorkingDir();
            final FilePath theBuildDir = makeRemotePath(workSpace,
                    Util.replaceMacro(workDir, envs));
            if (workDir != null) {
                theBuildDir.mkdirs();
            }

            /* Invoke tool in working dir */
            ArgumentListBuilder cmakeCall = buildToolCall(bindir + getToolId(),
                    Util.replaceMacro(getArguments(), envs));
            if (0 != launcher.launch().pwd(theBuildDir).envs(envs)
                    .stdout(listener).cmds(cmakeCall).join()) {
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
    private static ArgumentListBuilder buildToolCall(final String toolBin,
            String... toolArgs) {
        ArgumentListBuilder args = new ArgumentListBuilder();

        args.add(toolBin);
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
