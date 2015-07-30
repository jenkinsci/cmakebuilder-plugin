/*******************************************************************************
 * Copyright (c) 2015 Martin Weber.
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package hudson.plugins.cmake;

import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.model.DownloadService.Downloadable;
import hudson.model.Node;
import hudson.remoting.Callable;
import hudson.tools.ToolInstaller;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
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

    @DataBoundConstructor
    public CmakeInstaller(String id) {
        super(id);
    }

    @Override
    public FilePath performInstallation(ToolInstallation tool, Node node,
            TaskListener log) throws IOException, InterruptedException {
        // Gather properties for the node to install on
        String[] nodeProperties = node.getChannel().call(
                new GetSystemProperties("os.name", "os.arch"));

        FilePath toolPath = preferredLocation(tool, node);
        toolPath = fixPreferredLocation(toolPath, id);

        Installable inst = getInstallable(nodeProperties[0], nodeProperties[1]);
        if (inst == null) {
            log.fatalError(
                    "%s [%s]: No tool download known for OS `%s` with arch `%s`.%n",
                    getDescriptor().getDisplayName(), tool.getName(),
                    nodeProperties[0], nodeProperties[1]);
            return toolPath;
        }

        if (!isUpToDate(toolPath, inst)) {
            if (toolPath.installIfNecessaryFrom(
                    new URL(inst.url),
                    log,
                    "Unpacking " + inst.url + " to " + toolPath + " on "
                            + node.getDisplayName())) {
                toolPath.child(".timestamp").delete(); // we don't use the
                                                       // timestamp
                // TODO remove unnecessary files (docs, man pages)..
                // ./doc ./cmake-*/
                FilePath base = findPullUpDirectory(toolPath);
                if (base != null && !base.equals(toolPath))
                    base.moveAllChildrenTo(toolPath);
                // leave a record for the next up-to-date check
                toolPath.child(".installedFrom").write(inst.url, "UTF-8");
                // TODO expected.act(new
                // ZipExtractionInstaller.ChmodRecAPlusX());
            }
        }

        return toolPath.child("bin/cmake");
    }

    /**
     * Overloaded to select the OS-ARCH specific variant and to fill in the
     * variant´s URL.
     *
     * @param nodeOsName
     *            the value of the JVM system property "os.name" of the node
     * @param nodeOsArch
     *            the value of the JVM system property "os.arch" of the node
     * @return null if no such matching variant is found.
     */
    public Installable getInstallable(String nodeOsName, String nodeOsArch)
            throws IOException {
        List<CmakeInstallable> installables = ((DescriptorImpl) getDescriptor())
                .getInstallables();

        for (CmakeInstallable inst : installables)
            if (id.equals(inst.id)) {
                // Filter variants to install by system-properties
                // for the node to install on
                OsFamily osFamily = OsFamily.valueOfOsName(nodeOsName);
                for (CmakeVariant variant : inst.variants) {
                    if (variant.appliesTo(osFamily, nodeOsArch)) {
                        // fill in URL for download machinery
                        inst.url = variant.url;
                        return inst;
                    }
                }
            }
        return null;
    }

    /**
     * Fixes the value returned by {@link ToolInstaller#preferredLocation} to
     * use the specified <strong>installer ID</strong> instead of the
     * ToolInstallation {@link ToolInstallation#getName name}. This fix avoids
     * unneccessary downloads when users change the name of the tool on the
     * global config page.
     *
     * @param location
     *            preferred location of the tool being installed
     * @param installerID
     *            usually the value of {@link DownloadFromUrlInstaller#id}
     *
     * @return the fixed file path, if {@code location} ends with
     *         {@link ToolInstallation#getName}, else the unchanged
     *         {@code location}
     */
    protected FilePath fixPreferredLocation(FilePath location,
            String installerID) {
        String name = Util.fixEmptyAndTrim(tool.getName());
        if (location.getName().equals(name)) {
            return location.sibling(sanitize(installerID));
        }
        return location;
    }

    private static String sanitize(String s) {
        return s != null ? s.replaceAll("[^A-Za-z0-9_.-]+", "_") : null;
    }

    /**
     * Overwritten since 3.x archives from cmake.org have more than the
     * "cmake-<version>" directory
     */
    @Override
    protected FilePath findPullUpDirectory(FilePath root) throws IOException,
            InterruptedException {
        FilePath newRoot = super.findPullUpDirectory(root);
        if (newRoot.equals(root)) {
            return root;// super found a directory
        }
        // 3.x archives from cmake.org have more than the "cmake-<version>"
        // directory
        final String prefix = "cmake-" + id + "-";
        List<FilePath> dirs = root.list(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                if (!pathname.isFile()
                        && pathname.toString().startsWith(prefix))
                    return true;
                return false;
            }
        });
        if (dirs.size() == 1)
            return dirs.get(0);
        return null;
    }

    // //////////////////////////////////////////////////////////////////
    // inner classes
    // //////////////////////////////////////////////////////////////////

    @Extension
    public static final class DescriptorImpl extends
            DownloadFromUrlInstaller.DescriptorImpl<CmakeInstaller> {
        @Override
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
        @Override
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
     */
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
        }
    } // GetSystemProperties

    private static enum OsFamily {
        Linux, Windows("win32"), OSX("Darwin"), SunOS, FreeBSD, IRIX("IRIX64"), AIX, HPUX(
                "HP-UX");
        private final String cmakeOrgName;

        /**
         * Gets the OS name as specified in the files on the cmake.org download
         * site.
         *
         * @return the current cmakeOrgName property.
         */
        public String getCmakeOrgName() {
            return cmakeOrgName != null ? cmakeOrgName : name();
        }

        private OsFamily() {
            this(null);
        }

        private OsFamily(String cmakeOrgName) {
            this.cmakeOrgName = cmakeOrgName;
        }

        /**
         * Gets the OS family from the value of the system property "os.name".
         *
         * @param osName
         *            the value of the system property "os.name"
         * @return the OsFalimly object or {@code null} if osName is unknown
         */
        public static OsFamily valueOfOsName(String osName) {
            if (osName != null) {
                if ("Linux".equals(osName)) {
                    return Linux;
                } else if (osName.startsWith("Windows")) {
                    return Windows;
                } else if (osName.contains("OS X")) {
                    return OSX;
                } else if ("SunOS".equals(osName)) {
                    return SunOS;// not verified
                } else if ("AIX".equals(osName)) {
                    return AIX;
                } else if ("HPUX".equals(osName)) {
                    return HPUX;
                } else if ("Irix".equals(osName)) {
                    return IRIX;
                } else if ("FreeBSD".equals(osName)) {
                    return FreeBSD; // not verified
                }
            }
            return null;
        }
    } // OsFamily

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
        // these come frome the JSON file and finally from cmake´s download site
        // URLs
        /** OS name as specified by the cmake.org download site */
        public String os_cm = "";
        /** OS architecture as specified by the cmake.org download site */
        public String arch_cm = "";

        /**
         * Checks whether an installation of this CmakeVariant will work on the
         * given node. This checks the given JVM system properties of a node.
         *
         * @param osFamily
         *            the OS family derived from the JVM system property
         *            "os.name" of the node
         * @param nodeOsArch
         *            the value of the JVM system property "os.arch" of the node
         */
        public boolean appliesTo(OsFamily osFamily, String nodeOsArch) {
            if (osFamily != null && osFamily.getCmakeOrgName().equals(os_cm)) {
                switch (osFamily) {
                case Linux:
                    if (nodeOsArch.equals("i386") && nodeOsArch.equals(arch_cm)) {
                        return true;
                    }
                    if (nodeOsArch.equals("amd64")
                            && (arch_cm.equals("i386") || arch_cm
                                    .equals("x86_64"))) {
                        return true; // allow both i386 and x86_64
                    }
                    return false;
                case OSX: // to be verified by the community..
                    // ..cmake.org has both Darwin and Darwin64
                    if (nodeOsArch.equals("i386") && nodeOsArch.equals(arch_cm)) {
                        return true;
                    }
                    if (nodeOsArch.equals("amd64")
                            && (arch_cm.equals("universal") || arch_cm
                                    .equals("x86_64"))) {
                        return true; // allow both i386 and x86_64
                    }
                    return false;
                case Windows:
                case AIX:
                case HPUX:
                    return true; // only one arch is provided by cmake.org
                case IRIX:// to be verified by the community
                    // cmake.org provides arches "n32" & "64"
                    return true;
                default:
                    break;
                }
            }
            return false;
        }
    }

    // Needs to be public for JSON deserialisation
    public static class CmakeInstallable extends Installable {
        public CmakeVariant[] variants = new CmakeVariant[0];

        /**
         * Default ctor for JSON de-serialization.
         */
        public CmakeInstallable() {
        }

    }
}