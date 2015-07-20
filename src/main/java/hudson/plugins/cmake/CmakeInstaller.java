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
import hudson.model.DownloadService.Downloadable;
import hudson.model.Node;
import hudson.remoting.Callable;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Automatic Cmake installer from cmake.org.
 */
public class CmakeInstaller extends DownloadFromUrlInstaller {
    private transient String[] nodeProperties;

    @DataBoundConstructor
    public CmakeInstaller(String id) {
        super(id);
    }

    @Override
    public FilePath performInstallation(ToolInstallation tool, Node node,
            TaskListener log) throws IOException, InterruptedException {
        // TODO Auto-generated function stub
        // Gather properties for the node to install on
        this.nodeProperties = node.getChannel().call(
                new GetSystemProperties("os.name", "os.arch"));

        return super.performInstallation(tool, node, log);
    }

    /**
     * Overwritten to fill in the OS-ARCH specific URL.
     *
     * @return null if no such ID is found.
     */
    public Installable getInstallable() throws IOException {
        try {
            List<CmakeInstallable> installables = ((DescriptorImpl) getDescriptor())
                    .getInstallables();

            for (CmakeInstallable inst : installables)
                if (id.equals(inst.id)) {
                    // Filter variants to install by system-properties
                    final String nodeOsName = nodeProperties[0];
                    final String nodeOsArch = nodeProperties[1];
                    // for the node to install on
                    for (CmakeVariant variant : inst.variants) {
                        if (variant.appliesTo(nodeOsName, nodeOsArch)) {
                            CmakeInstallable inst2 = new CmakeInstallable(inst,
                                    variant);
                            return inst2;
                        }
                    }
                }
            return null;
        } finally {
            nodeProperties = null; // for GC
        }
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

        /**
         * List of installable tools.
         *
         * <p>
         * The UI uses this information to populate the drop-down.
         *
         * @return never null.
         */
        public List<CmakeInstallable> getInstallables() throws IOException {
            JSONObject d = Downloadable.get(getId()).getData();
            if (d == null)
                return Collections.emptyList();
            Map<String, Class<?>> classMap = new HashMap<String, Class<?>>();
            // classMap.put("versions", CmakeVariant.class);
            classMap.put("variants", CmakeVariant.class);
            return Arrays.asList(((CmakeInstallableList) JSONObject.toBean(d,
                    CmakeInstallableList.class, classMap)).list);
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

    // //////////////////////////////////////////////////////////////////
    // JSON desrialization
    // //////////////////////////////////////////////////////////////////
    /**
     * Represents the de-serialized JSON data file containing all installable
     * Cmake versions. See file hudson.plugins.cmake.CmakeInstaller
     */
    @Extension
    public static final class CmakeInstallableList {
        // initialize with an empty array just in case JSON doesn't have the
        // list field (which shouldn't happen.)
        // Public for JSON deserialisation
        public CmakeInstallable[] list = new CmakeInstallable[0];
    } // CmakeInstallableList

    // Needs to be public for JSON deserialisation
    public static class CmakeVariant {
        public String url;
        // these come frome the JSON file and finally from cmake´s update site
        // URLs
        public String os = "";
        public String arch = "";

        /**
         * Checks whether an installation of this CmakeVariant will work on the
         * given node. This checks the given JVM system properties of a node.
         *
         * @param nodeOsName
         *            the value of the JVM system property "os.name" of the node
         * @param nodeOsArch
         *            the value of the JVM system property "os.arch" of the node
         */
        public boolean appliesTo(String nodeOsName, String nodeOsArch) {
            if ("Linux".equals(nodeOsName)) {
                if (os.equals(nodeOsName)) {
                    if (nodeOsArch.equals("i386") && arch.equals("i386")) {
                        return true;
                    }
                    if (nodeOsArch.equals("amd64")
                            && (arch.equals("i386") || arch.equals("x86_64"))) {
                        return true; // allow both i386 and x86_64
                    }
                }
            } else if (nodeOsName.startsWith("Windows")) {
                nodeOsName = "win32";
                if (os.equals(nodeOsName)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * @return the value of the JVM system property "os.name" of a node
         */
        private String osToJvmProp() {
            if ("Linux".equalsIgnoreCase(os)) {
                return "Linux";
            } else if ("win32".equalsIgnoreCase(os)) {
                return "win32";
            } else {
                // no replacement known yet, users should file a change request
                return os;
            }
        }
    }

    // Needs to be public for JSON deserialisation
    public static class CmakeInstallable extends Installable {
        public CmakeVariant[] variants = new CmakeVariant[0];

        /**
         * Default ctor for JSON desrialization.
         */
        public CmakeInstallable() {
        }

        /**
         * @param id
         *            Used internally to uniquely identify the name.
         * @param name
         *            This is the human readable name.
         */
        public CmakeInstallable(CmakeInstallable installable,
                CmakeVariant variant) {
            super.id = installable.id;
            super.name = installable.name;
            super.url = variant.url;
        }

    }
}