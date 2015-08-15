/*******************************************************************************
 * Copyright (c) 2015 Martin Weber.
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package hudson.plugins.cmake;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.TaskListener;
import hudson.model.Descriptor;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Invokes the build tool configured by cmake.
 *
 * @author Martin Weber
 */
public class BuildToolStep extends AbstractDescribableImpl<BuildToolStep> {

    private String args;
    private String vars;
    private boolean withCmake;

    /**
     *
     */
    @DataBoundConstructor
    public BuildToolStep() {
    }

    /**
     * Gets the arguments to pass to the build tool.
     *
     * @param buildEnvironment
     *            the environement of the build step, for variable expansion.
     * @return the arguments, never {@code null}
     */
    public String[] getCommandArguments(EnvVars buildEnvironment) {
        if (args == null)
            return new String[0];
        String argsEx = Util.replaceMacro(args, buildEnvironment);
        return Util.tokenize(argsEx);
    }

    /**
     * Gets the extra environment variables to pass to the build tool.
     *
     * @param buildEnvironment
     *            the environement of the build step, for variable expansion.
     * @return the variables and values, never{@code null}
     */
    public Map<String, String> getEnvironmentVars(EnvVars buildEnvironment,
            TaskListener listener) {
        if (vars == null)
            return Collections.emptyMap();

        Map<String, String> env = new HashMap<String, String>(2, 1.0f);
        String varsEx = Util.replaceMacro(vars, buildEnvironment);
        LineNumberReader reader = new LineNumberReader(new StringReader(varsEx));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                int idx = line.indexOf('=');
                if (idx > 0 && idx < line.length() - 1) {
                    // split VAR=VALUE and add to map
                    env.put(line.substring(0, idx), line.substring(idx + 1));
                } else {
                    // log garbled expression
                    listener.error(
                            "Garbled env. variable expression '%s' (ignored)",
                            line);
                }
            }
        } catch (IOException ex) {
            // ignore, we have a string as input
        }
        return env;
    }

    /**
     * Overridden for better type safety.
     */
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * Gets whether to run the actual build tool directly (by expanding
     * <code>$CMAKE_BUILD_TOOL</code>) or to have <code>cmake</code> run the
     * build tool (by invoking <code>cmake --build</code>).
     */
    public boolean getWithCmake() {
        return withCmake;
    }

    /**
     * Sets whether to run the actual build tool directly (by expanding
     * <code>$CMAKE_BUILD_TOOL</code>) or to have <code>cmake</code> run the
     * build tool (by invoking <code>cmake --build</code>).
     */
    @DataBoundSetter
    public void setWithCmake(boolean withCmake) {
        this.withCmake = withCmake;
    }

    /**
     * Gets the content of the form field 'args'.
     */
    public String getArgs() {
        return args;
    }

    /**
     * Sets the content of the form field 'args'.
     */
    @DataBoundSetter
    public void setArgs(String args) {
        this.args = Util.fixEmptyAndTrim(args);
    }

    /**
     * Gets the content of the form field 'envVars'.
     */
    public String getEnvVars() {
        return vars;
    }

    /**
     * Sets the content of the form field 'envVars'.
     */
    @DataBoundSetter
    public void setEnvVars(String vars) {
        this.vars = Util.fixEmptyAndTrim(vars);
    }

    // //////////////////////////////////////////////////////////////////
    // inner classes
    // //////////////////////////////////////////////////////////////////
    /**
     * Descriptor for {@link CmakeBuilder}. Used as a singleton. The class is
     * marked as public so that it can be accessed from views.
     */
    @Extension
    public static final class DescriptorImpl extends Descriptor<BuildToolStep> {

        public DescriptorImpl() {
            super(BuildToolStep.class);
            load();
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Invoke Build tool";
        }
    } // DescriptorImpl
}
