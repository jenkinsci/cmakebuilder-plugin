package hudson.plugins.cmake;

import static hudson.init.InitMilestone.EXTENSIONS_AUGMENTED;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.init.Initializer;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.InstallSourceProperty;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

/**
 * Information about Cmake installation. A CmakeTool is used to select between
 * different installations of cmake, as in "cmake2.6.4" or "cmake2.8.12".
 *
 * @author Martin Weber
 */
public class CmakeTool extends ToolInstallation implements
        NodeSpecific<CmakeTool>, EnvironmentSpecific<CmakeTool> {
    // private static final Logger LOGGER = Logger.getLogger(CmakeTool.class
    // .getName());

    /**
     * Tool name of the default tool (usually found on the executable search
     * path). Do not use: Exposed here only for testing purposes.
     */
    public static transient final String DEFAULT = "InSearchPath";

    private static final long serialVersionUID = 1;

    /**
     * the parent directory of the [@code cmake} tool from the installation or
     * {@code null} if this object has not been retrieved through
     * {@link #forNode(Node, TaskListener)}. This is used to determine the file
     * system path of any of the tools of the cmake suite (cmake/cpack/ctest) on the node.
     */
    private transient String bindir;

    /**
     * Constructor for CmakeTool.
     *
     * @param name
     *            Tool name (for example, "cmake2.6.4" or "cmake2.8.12")
     * @param home
     *            Tool location (usually "cmake")
     * @param properties
     *            {@link java.util.List} of properties for this tool
     */
    @DataBoundConstructor
    public CmakeTool(String name, String home,
            List<? extends ToolProperty<?>> properties) {
        super(Util.fixEmptyAndTrim(name), Util.fixEmptyAndTrim(home),
                properties);
    }

    /**
     * @see #CmakeTool(String, String, List)
     *
     * @param bindir
     *            the parent directory of the [@code cmake} tool from the
     *            installation. This is used to determine the file system path
     *            of any of the tools of the cmake suite (cmake/cpack/ctest).
     */
    private CmakeTool(String name, String home,
            List<ToolProperty<?>> properties, String bindir) {
        this(name, home, properties);
        this.bindir = bindir;
    }

    /**
     * @return {@link java.lang.String} that will be used to execute cmake (e.g.
     *         "cmake" or "/usr/bin/cmake")
     */
    public String getCmakeExe() {
        // return what was entered on the global config page
        return getHome();
    }

    /**
     * Gets the parent directory of the [@code cmake} tool from the installation
     * or {@code null}. This may be used to determine the file system path of
     * any of the tools of the cmake suite (cmake/cpack/ctest).
     *
     * @return the directory including a trailing, node specific directory
     *         separator character or {@code null} if this object has not been
     *         retrieved through {@link #forNode(Node, TaskListener)}
     */
    public String getBindir() {
        return bindir;
    }

    /**
     * Overwritten to add the path to cmake`s bin directory, if tool was
     * downloaded.
     */
    @Override
    public void buildEnvVars(EnvVars env) {
        if (getProperties().get(InstallSourceProperty.class) != null) {
            // cmake was downloaded and installed
            if (bindir != null && !bindir.isEmpty()) {
                env.put("PATH+CMAKE", bindir);
            }
        }
    }

    public CmakeTool forNode(Node node, TaskListener log) throws IOException,
            InterruptedException {
        String home = translateFor(node, log); // the home on the slave!!!
        String bindir = "";
        // think of this as a cross-platform version of 'dirname(home)'...
        int idx;
        if ((idx = home.lastIndexOf('/')) != -1
                || (idx = home.lastIndexOf('\\')) != -1) {
            bindir = home.substring(0, idx + 1);
        }

        return new CmakeTool(getName(), home, getProperties().toList(), bindir);
    }

    public CmakeTool forEnvironment(EnvVars environment) {
        return new CmakeTool(getName(), environment.expand(getHome()),
                getProperties().toList(), bindir);
    }

    /**
     * Creates a default tool installation if needed. Uses "cmake" or migrates
     * data from previous versions
     *
     */
    @Initializer(after = EXTENSIONS_AUGMENTED)
    public static void onLoaded() {

        Jenkins jenkinsInstance = Jenkins.getInstance();
        if (jenkinsInstance == null) {
            return;
        }
        DescriptorImpl descriptor = (DescriptorImpl) jenkinsInstance
                .getDescriptor(CmakeTool.class);
        CmakeTool[] installations = getInstallations(descriptor);

        if (installations != null && installations.length > 0) {
            // No need to initialize if there's already something
            return;
        }

        CmakeTool tool = new CmakeTool(DEFAULT, "cmake",
                Collections.<ToolProperty<?>> emptyList());
        descriptor.setInstallations(new CmakeTool[] { tool });
        descriptor.save();
    }

    private static CmakeTool[] getInstallations(DescriptorImpl descriptor) {
        CmakeTool[] installations = null;
        try {
            installations = descriptor.getInstallations();
        } catch (NullPointerException e) {
            installations = new CmakeTool[0];
        }
        return installations;
    }

    // //////////////////////////////////////////////////////////////////
    // inner classes
    // //////////////////////////////////////////////////////////////////

    @Extension
    public static class DescriptorImpl extends ToolDescriptor<CmakeTool> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "CMake";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json)
                throws FormException {
            // reject empty tool names...
            List<CmakeTool> cmakes = req.bindJSONToList(CmakeTool.class,
                    json.get("tool"));
            for (CmakeTool tool : cmakes) {
                if (Util.fixEmpty(tool.getName()) == null)
                    throw new FormException(getDisplayName()
                            + " installation requires a name", "_.name");
            }

            super.configure(req, json);
            save();
            return true;
        }

        /**
         * Overwritten to make cmake auto-installer a default option.
         */
        @Override
        public List<? extends ToolInstaller> getDefaultInstallers() {
            return Collections.singletonList(new CmakeInstaller(null));
        }

    } // DescriptorImpl
}
