/*
 * The MIT License
 *
 * Copyright 2018 Martin Weber
 */
package hudson.plugins.cmake;

import java.io.IOException;

import javax.servlet.ServletException;

import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;

/**
 * Provides a pipeline build step that allows to invoke selected tools of the
 * cmake-suite ({@code cmake}, {@code cpack} and {@code ctest}) with arbitrary
 * arguments.<br>
 * Similar to {@code CToolBuilder}, but pipeline compatible.<br>
 * NOTE: Actually, this class is NOT abstract, but we want to re-use the
 * {@code @DataBoundSetter} methods defined here.
 *
 * @author Martin Weber
 */
public class AbstractToolStep extends AbstractStep {
    private static final long serialVersionUID = 1L;

    /**
     * Exit codes of the tool that indicate a failure but should be ignored,
     * thus causing the build to proceed.<br>
     */
    private String ignoredExitCodes;

    /**
     * Parsed and cached exit codes to ignore.
     */
    private transient IntSet ignoredExitCodesParsed;

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
    public AbstractToolStep(String installation) {
        super(installation);
    }

    /**
     * Gets the exit codes of the tool that indicate a failure but should be
     * ignored, thus causing the build to proceed.
     *
     * @return the ignoredExitCodes property value or <code>null</code>
     */
    public String getIgnoredExitCodes() {
        return ignoredExitCodes;
    }

    @DataBoundSetter
    public void setIgnoredExitCodes(String ignoredExitCodes) {
        this.ignoredExitCodes = Util.fixEmptyAndTrim(ignoredExitCodes);
        ignoredExitCodesParsed = null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.jenkinsci.plugins.workflow.steps.Step#start(org.jenkinsci.plugins.
     * workflow.steps.StepContext)
     */
    @Override
    public StepExecution start(StepContext context) throws Exception {
        super.start(context);
        return new Execution(this, context);
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

    // //////////////////////////////////////////////////////////////////
    // inner classes
    // //////////////////////////////////////////////////////////////////
    private static class Execution
            extends SynchronousNonBlockingStepExecution<Integer> {
        private static final long serialVersionUID = 1L;

        private final AbstractToolStep step;

        Execution(AbstractToolStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        /**
         * @see org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution#run()
         *
         * @return the exit code of the cmake/cpack/ctest process
         */
        @Override
        protected Integer run() throws Exception {
            final StepContext context = getContext();
            final TaskListener listener = context.get(TaskListener.class);
            final Launcher launcher = context.get(Launcher.class);
            final Node node = context.get(Node.class);
            final EnvVars env = context.get(EnvVars.class);

            CmakeTool installToUse = step.getSelectedInstallation();
            // Raise an error if the cmake installation isn't found
            if (installToUse == null) {
                throw new AbortException(
                        Messages.getString("No_installation_selected")); //$NON-NLS-1$
            }

            // Get the CMake version for this node, installing it if necessary
            installToUse = installToUse.forNode(node, listener)
                    .forEnvironment(context.get(EnvVars.class));
            /* Determine remote working directory path. Create it. */
            final FilePath workSpace = context.get(FilePath.class);
            final String workDir = step.getWorkingDir();
            final FilePath theWorkDir = LaunchUtils.makeRemotePath(workSpace,
                    workDir);
            if (workDir != null) {
                theWorkDir.mkdirs();
            }

            /* Invoke tool in working dir */
            ArgumentListBuilder cmakeCall = LaunchUtils
                    .buildCommandline(
                            installToUse.getAbsoluteCommand(node,
                                    step.getCommandBasename()),
                            step.getArguments());
            final int exitCode;
            if (0 == (exitCode = launcher.launch().pwd(theWorkDir).envs(env)
                    .stdout(listener).cmds(cmakeCall).join())) {
                return Integer.valueOf(exitCode);
            }
            // should this failure be ignored?
            if (step.ignoredExitCodesParsed == null) {
                step.ignoredExitCodesParsed = new IntSet(step.ignoredExitCodes);
            }
            if (step.ignoredExitCodesParsed.contains(exitCode)) {
                // ignore this failure exit code
                // ignore this failure exit code
                listener.getLogger().printf(
                        Messages.getString("Exited_with_error_code_ignored"), //$NON-NLS-1$
                        step.getCommandBasename(), exitCode);
                return Integer.valueOf(exitCode); // no failure
            }
            // invocation failed, not ignored
            throw new AbortException(
                    String.format(Messages.getString("Exited_with_error_code"), //$NON-NLS-1$
                            step.getCommandBasename(), exitCode));
        }
    } // Execution

    protected abstract static class DescriptorImpl
            extends AbstractStep.DescriptorImpl {

        /**
         * Performs on-the-fly validation of the form field 'ignoredExitCodes'.
         *
         * @param value
         */
        @Restricted(NoExternalUse.class) // Only for UI calls
        public FormValidation doCheckIgnoredExitCodes(
                @QueryParameter final String value)
                throws IOException, ServletException {
            try {
                new IntSet().setValues(Util.fixEmptyAndTrim(value));
            } catch (IllegalArgumentException iae) {
                return FormValidation.error(iae.getLocalizedMessage());
            }
            return FormValidation.ok();
        }
    }
}
