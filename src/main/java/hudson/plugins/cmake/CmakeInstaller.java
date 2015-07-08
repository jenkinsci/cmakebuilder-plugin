/*******************************************************************************
 * Copyright (c) 2015 Martin Weber.
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package hudson.plugins.cmake;

import hudson.Extension;
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
  }
} // CmakeInstaller