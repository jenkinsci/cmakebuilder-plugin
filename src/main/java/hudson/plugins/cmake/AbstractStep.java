/*
 * The MIT License
 *
 * Copyright 2018 Martin Weber
 */
package hudson.plugins.cmake;

import java.io.Serializable;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.google.common.collect.ImmutableSet;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.model.Descriptor.FormException;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

/**
 * A pipeline Step that holds information about a cmake installation.<br>
 * NOTE: Actually, this class is NOT abstract, since we want to re-use the
 * {@code @DataBoundSetter} methods defined here.
 *
 * @author Martin weber
 */
public class AbstractStep extends Step implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final int MAX_LABEL_LENGTH = 100;

    /** the name of the cmake tool installation to use for this build step */
    private String installation;

    private String label;

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

    /**
     * Gets the basename of the command to run (cmake, cpack or ctest).
     *
     * @return {@code "cmake"}
     */
    protected String getCommandBasename() {
        return "cmake"; //$NON-NLS-1$
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

    @DataBoundSetter
    public void setLabel(String label) {
        this.label = Util.fixEmptyAndTrim(label);
    }

    public String getLabel() {
        return label;
    }

    /**
     * Implemented to just add the label string to the build log.<br>
     * Sub-classes should override and call super.
     *
     * @return always <code>null</code>
     */
    @Override
    public StepExecution start(StepContext context) throws Exception {
        if (this.label != null) {
            context.get(FlowNode.class).addAction(
                    new LabelAction(StringUtils.left(label, MAX_LABEL_LENGTH)));
        }
        return null;
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

        public FormValidation doCheckLabel(@QueryParameter String label) {
            if (label != null && label.length() > MAX_LABEL_LENGTH) {
                return FormValidation.error(Messages.getString(
                        "AbstractStep.Descriptor.FormValidation.Label_too_long"), //$NON-NLS-1$
                        MAX_LABEL_LENGTH);
            }
            return FormValidation.ok();
        }
    }
}