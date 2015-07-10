/*******************************************************************************
 * Copyright (c) 2015 Martin Weber.
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package hudson.plugins.cmake;

import java.io.IOException;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.model.Node;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;

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
}