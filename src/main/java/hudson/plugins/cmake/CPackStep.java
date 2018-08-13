/*
 * The MIT License
 *
 * Copyright 2018 Martin Weber
 */
package hudson.plugins.cmake;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;

/**
 * Provides a pipeline build step that allows to invoke {@code cpack} with
 * arbitrary arguments.<br>
 * Similar to {@code CToolBuilder}, but pipeline compatible.
 *
 * @author Martin Weber
 */
public class CPackStep extends AbstractToolStep {
    private static final long serialVersionUID = 1L;

    /**
     * Minimal constructor.
     *
     * @param installation
     *            the name of the cmake tool installation from the global config
     *            page.
     */
    @DataBoundConstructor
    public CPackStep(String installation) {
        super(installation);
    }

    protected String getCommandBasename() {
        return "cpack"; //$NON-NLS-1$
    }

    @Extension(optional = true)
    public static class DescriptorImpl
            extends AbstractToolStep.DescriptorImpl {

        @Override
        public String getFunctionName() {
            return "cpack"; //$NON-NLS-1$
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return Messages.getString("CPackStep.Descriptor.DisplayName"); //$NON-NLS-1$
        }

    }
}
