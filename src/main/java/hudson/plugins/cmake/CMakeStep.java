/*
 * The MIT License
 *
 * Copyright 2018 Martin Weber
 */
package hudson.plugins.cmake;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;

/**
 * Provides a pipeline build step that allows to invoke {@code cmake} with
 * arbitrary arguments.<br>
 * Similar to {@code CToolBuilder}, but pipeline compatible.
 *
 * @author Martin Weber
 */
public class CMakeStep extends AbstractToolStep {
    private static final long serialVersionUID = 1L;

    /**
     * Minimal constructor.
     *
     * @param installation
     *            the name of the cmake tool installation from the global config
     *            page.
     */
    @DataBoundConstructor
    public CMakeStep(String installation) {
        super(installation);
    }

    @Extension(optional = true)
    public static class DescriptorImpl
            extends AbstractToolStep.DescriptorImpl {

        @Override
        public String getFunctionName() {
            return "cmake"; //$NON-NLS-1$
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return Messages.getString("CMakeStep.Descriptor.DisplayName"); //$NON-NLS-1$
        }

    }
}
