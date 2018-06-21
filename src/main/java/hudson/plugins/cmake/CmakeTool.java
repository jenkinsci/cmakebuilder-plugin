package hudson.plugins.cmake;

import static hudson.init.InitMilestone.EXTENSIONS_AUGMENTED;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.init.Initializer;
import hudson.model.Computer;
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
    private static final Logger LOGGER = Logger.getLogger(CmakeTool.class
     .getName());

    /**
     * Tool name of the default tool (usually found on the executable search
     * path). Do not use: Exposed here only for testing purposes.
     */
    public static transient final String DEFAULT = "InSearchPath";

    private static final long serialVersionUID = 1;

    /**
     * Constructor for CmakeTool.
     *
     * @param name
     *            Tool name (for example, "cmake2.6.4" or "cmake2.8.12")
     * @param home
     *            the parent directory of the {@code cmake} tool from the
     *            installation. This is used to determine the file system path
     *            of any of the tools of the cmake suite (cmake/cpack/ctest).
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
     * Gets the absolute file system path on the specified node with the
     * basename appended. Takes the node specific directory separators into
     * account when constructing the path.<br>
     * Should be invoked after {@link #forNode(Node, TaskListener)} was called.
     *
     * @param node
     *            Node that this tool is used in.
     * @param basename
     *            the basename of the command to run (cmake, cpack or ctest).
     *
     * @return the absolute file system path on the node or just the basename,
     *         if {@link #getHome()} returns null
     */
    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "if (computer != null && !computer.isUnix()): WTF")
    public String getAbsoluteCommand(Node node, String basename) {
        String home = getHome();
        if (home != null && !home.isEmpty()) {
            // append the node-specific dir-separator
            String sep = "/";
            final Computer computer = node.toComputer();
            if (computer != null && !computer.isUnix()) {
                sep = "\\";
            }
            if (!home.endsWith(sep))
                home += sep;
            return home += basename;
        }
        return basename;
    }

    /**
     * Overwritten to add the path to cmake`s bin directory, if tool was
     * downloaded.
     */
    @Override
    public void buildEnvVars(EnvVars env) {
        final String home = getHome();
        if (home != null && !home.isEmpty()) {
            env.put("PATH+CMAKE", home);
        }
    }

    public CmakeTool forNode(Node node, TaskListener log) throws IOException,
            InterruptedException {
        final String home = translateFor(node, log); // the home on the node!!!
        return new CmakeTool(getName(), home, getProperties().toList());
    }

    public CmakeTool forEnvironment(EnvVars environment) {
        return new CmakeTool(getName(), environment.expand(getHome()),
                getProperties().toList());
    }

    /**
     * Creates a default tool installation if needed. Uses "cmake" or migrates
     * data from previous versions
     *
     */
    @Initializer(after = EXTENSIONS_AUGMENTED)
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification = "Older Jenkins may return null here")
    public static void onLoaded() {

        Jenkins jenkinsInstance = Jenkins.getInstance();
        if (jenkinsInstance == null) {
            return;
        }
        DescriptorImpl descriptor = (DescriptorImpl) jenkinsInstance
                .getDescriptor(CmakeTool.class);
        CmakeTool[] installations = getInstallations(descriptor);
        if (installations.length > 0) {
            // migrate legacy InSearchPath installations
            tryMigrateLegacy(descriptor, installations);
        } else {
            // no installations configured yet, create a default installation
            CmakeTool tool = new CmakeTool(DEFAULT, null,
                    Collections.<ToolProperty<?>> emptyList());
            descriptor.setInstallations(new CmakeTool[] { tool });
            descriptor.save();
        }
    }

    /**
     * Migrates each legacy InSearchPath installations that have just 'cmake' as
     * home to one which has home == null and migrates non-automatically
     * installed ones that have a trailing command name ('cmake').<br>
     * NOTE: Jenkins does not invoke a {@link hudson.tools.ToolInstallation.ToolConverter}
     * for non-automatically installed tools.
     *
     * @since 2.6.0
     */
    private static void tryMigrateLegacy(DescriptorImpl descriptor,
            CmakeTool[] installations) {
        boolean mustSave = false;
        for (int i = 0; i < installations.length; i++) {
            CmakeTool inst = installations[i];
            if (inst.getProperties()
                    .get(InstallSourceProperty.class) == null) {
                // not automatically installed
                String home = inst.getHome();
                if (home != null) {
                    if (home.equals("cmake")) {
                        // the legacy DEFAULT
                        CmakeTool newInst = new CmakeTool(inst.getName(),
                                null, inst.getProperties());
                        installations[i] = newInst;
                        mustSave = true;
                    } else if (home.endsWith("/cmake")
                            || home.endsWith("\\cmake")) {
                        // user given path
                        // strip trailing '/cmake'
                        home = home.substring(0, home.length() - 6);
                        CmakeTool newInst = new CmakeTool(inst.getName(),
                                home, inst.getProperties());
                        installations[i] = newInst;
                        mustSave = true;
                    } else {
                        // migration impossible, log warning
                        String msg = String.format("Could not migrate CMake installation '%s'"
                                + " to a format compatible with the zip-installer. "
                                + "Please remove the command name in '%s' "
                                + "on the global tool configuration page.",
                                inst.getName(), inst.getHome());
                        LOGGER.warning(msg);
                    }
                }
            }
        }
        if (mustSave) {
            // save migrated
            descriptor.setInstallations(installations);
            descriptor.save();
        }
    }

    private static CmakeTool[] getInstallations(DescriptorImpl descriptor) {
        CmakeTool[] installations;
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
