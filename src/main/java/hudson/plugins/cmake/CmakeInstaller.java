/*******************************************************************************
 * Copyright (c) 2015 Martin Weber.
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package hudson.plugins.cmake;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.model.Node;
import hudson.remoting.Callable;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;

import java.io.IOException;

import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Automatic Cmake installer from cmake.org.
 */
public class CmakeInstaller extends DownloadFromUrlInstaller {
    @DataBoundConstructor
    public CmakeInstaller(String id) {
        super(id);
    }

    @Override
    public FilePath performInstallation(ToolInstallation tool, Node node,
            TaskListener log) throws IOException, InterruptedException {
        // TODO Auto-generated function stub
        return super.performInstallation(tool, node, log);
    }

    /**
     * Overwritten to fill in the OS-ARCH specific URL.
     *
     * @return null if no such ID is found.
     */
    public Installable getInstallable() throws IOException {
        for (Installable i : ((DescriptorImpl) getDescriptor())
                .getInstallables())
            if (id.equals(i.id)) {
                // Gather properties for the node to install on
                String[] properties = node.getChannel().call(
                        new GetSystemProperties("os.name", "os.arch",
                                "os.version"));
                return i;
            }
        return null;
    }

    // //////////////////////////////////////////////////////////////////
    // inner classes
    // //////////////////////////////////////////////////////////////////

    @Extension
    public static final class DescriptorImpl extends
            DownloadFromUrlInstaller.DescriptorImpl<CmakeInstaller> {
        public String getDisplayName() {
            return "Install from cmake.org";
        }

        @Override
        public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
            return toolType == CmakeTool.class;
        }
    } // DescriptorImpl

    /**
     * A Callable that gets the values of the given Java system properties from
     * the (remote) node.
     * */
    private static class GetSystemProperties implements
            Callable<String[], InterruptedException> {
        private static final long serialVersionUID = 1L;

        private final String[] properties;

        GetSystemProperties(String... properties) {
            this.properties = properties;
        }

        public String[] call() {
            String[] values = new String[properties.length];
            for (int i = 0; i < properties.length; i++) {
                values[i] = System.getProperty(properties[i]);
            }
            return values;
        }

        /*-
         * @see org.jenkinsci.remoting.RoleSensitive#checkRoles(org.jenkinsci.remoting.RoleChecker)
         */
        @Override
        public void checkRoles(RoleChecker checker) throws SecurityException {
            // TODO Auto-generated function stub
        }
    } // GetSystemProperties
}