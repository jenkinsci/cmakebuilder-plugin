/*
 * The MIT License
 *
 * Copyright 2018 Martin Weber
 */
package hudson.plugins.cmake;

import java.io.Serializable;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.google.common.collect.ImmutableSet;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;

/**
 * A Step that holds information about a cmake installation, the working
 * directory for invocation and the arguments to pass to {@code cmake},<br>
 * NOTE: Actually, this class is NOT abstract, but we want to re-use the
 * {@code @DataBoundSetter} methods defined here.
 *
 * @author Martin weber
 */
public class AbstractStep extends Step implements Serializable {
    private static final long serialVersionUID = 1L;

    /** the name of the cmake tool installation to use for this build step */
    private String installation;
    private String workingDir;
    private String toolArgs;

    /**
     * Minimal constructor.
     *
     * @param installation
     *            the name of the cmake tool installation from the global config
     *            page.
     */
    @DataBoundConstructor
    public AbstractStep(String installation) {
        this.installation = Util.fixEmptyAndTrim(installation);
    }

    /** Gets the name of the cmake installation to use for this build step */
    public String getInstallation() {
        return this.installation;
    }

    @DataBoundSetter
    public void setWorkingDir(String workingDir) {
        this.workingDir = Util.fixEmptyAndTrim(workingDir);
    }

    public String getWorkingDir() {
        return this.workingDir;
    }

    @DataBoundSetter
    public void setArguments(String toolArgs) {
        this.toolArgs = Util.fixEmptyAndTrim(toolArgs);
    }

    public String getArguments() {
        return this.toolArgs;
    }

    /**
     * This should be overwritten. It is only implemented because of compile
     * error "@DataBoundConstructor may not be used on an abstract class"
     */
    @Override
    public StepExecution start(StepContext context) throws Exception {
        throw new UnsupportedOperationException(
                "Implemnetors MUST overwrite this in their sub-class");
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