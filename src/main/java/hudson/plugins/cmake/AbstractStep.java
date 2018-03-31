/*
 * The MIT License
 *
 * Copyright 2018 Martin Weber
 */
package hudson.plugins.cmake;

import java.io.Serializable;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;

import com.google.common.collect.ImmutableSet;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;

/**
 * A Step that holds information about a cmake installation,
 *
 * @author Martin weber
 */
public abstract class AbstractStep extends Step implements Serializable {
    private static final long serialVersionUID = 1L;

    /** the name of the cmake tool installation to use for this build step */
    private String installation;

    /**
     * Minimal constructor.
     *
     * @param installation
     *            the name of the cmake tool installation from the global config
     *            page.
     */
    public AbstractStep(String installation) {
        this.installation = Util.fixEmptyAndTrim(installation);
    }

    /** Gets the name of the cmake installation to use for this build step */
    public String getInstallation() {
        return this.installation;
    }

    /**
     * Gets the basename of the command to run (cmake, cpack or ctest).
     *
     * @return {@code "cmake"}
     */
    protected String getCommandBasename() {
        return "cmake";
    }

    /**
     * Finds the cmake tool installation to use for this build among all
     * installations configured in the Jenkins administration
     *
     * @return selected CMake installation or {@code null} if none could be
     *         found
     */
    protected CmakeTool getSelectedInstallation() {
        return InstallationUtils.getInstallationByName(installation);
    }

    // //////////////////////////////////////////////////////////////////
    // inner classes
    // //////////////////////////////////////////////////////////////////
    /**
     * Descriptor for {@link AbstractStep}. Used as a singleton. The class
     * is marked as public so that it can be accessed from views.
     */
    public static abstract class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(TaskListener.class, Launcher.class, EnvVars.class,
                    Node.class, FilePath.class);
        }

        /**
         * Determines the values of the Cmake installation drop-down list box.
         */
        public ListBoxModel doFillInstallationItems() {
            return InstallationUtils.doFillInstallationNameItems();
        }
    }
}