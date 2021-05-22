/*
 * The MIT License
 *
 * Copyright 2018 Martin Weber
 */
package hudson.plugins.cmake;

import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

/**
 * Utility functions for installations of the cmake suite.
 *
 * @author Martin Weber
 */
class InstallationUtils {

    private InstallationUtils() {
    }

    /**
     * Determines the values of the Cmake installation drop-down list box.
     */
    static ListBoxModel doFillInstallationNameItems() {
        ListBoxModel items = new ListBoxModel();
        final Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins != null) {
            CmakeTool.DescriptorImpl descriptor = (CmakeTool.DescriptorImpl) jenkins
                    .getDescriptor(CmakeTool.class);
            if (descriptor != null)
                for (CmakeTool inst : descriptor.getInstallations()) {
                    items.add(inst.getName());
                }
        }
        return items;
    }
    /**
     * Finds the cmake tool installation among all
     * installations configured in the Jenkins administration by name.
     *
     * @param installationName the name of the installation to search for
     *
     * @return selected CMake installation or {@code null} if none could be
     *         found
     */
    static CmakeTool getInstallationByName(String installationName) {
        final Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins != null) {
            final CmakeTool.DescriptorImpl descriptor = (CmakeTool.DescriptorImpl) jenkins
                    .getDescriptor(CmakeTool.class);
            if (descriptor != null)
                for (CmakeTool i : descriptor.getInstallations()) {
                    if (installationName != null
                            && i.getName().equals(installationName))
                        return i;
                }
        }
        return null;
    }

}
