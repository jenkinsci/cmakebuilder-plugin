/*******************************************************************************
 * Copyright (c) 2015 Martin Weber.
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package hudson.plugins.cmake;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import hudson.EnvVars;
import hudson.console.ConsoleNote;
import hudson.util.AbstractTaskListener;

/**
 * @author Martin Weber
 */
public class BuildToolStepTest {
    // @Rule
    // public JenkinsRule j = new JenkinsRule();

    /**
     * @author Martin Weber
     */
    private static class DummyTaskListener extends AbstractTaskListener {
        /**  */
        private static final long serialVersionUID = 1L;

        @Override
        public PrintStream getLogger() {
            return null;
        }

        @Override
        public PrintWriter fatalError(String format, Object... args) {
            return null;
        }

        @Override
        public PrintWriter fatalError(String msg) {
            return null;
        }

        @Override
        public PrintWriter error(String format, Object... args) {
            return null;
        }

        @Override
        public PrintWriter error(String msg) {
            return null;
        }

        @Override
        public void annotate(@SuppressWarnings("rawtypes") ConsoleNote ann)
                throws IOException {
        }
    }

    private BuildToolStep testee;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        testee = new BuildToolStep();
    }

    /**
     * Test method for
     * {@link hudson.plugins.cmake.BuildToolStep#getCommandArguments(hudson.EnvVars)}
     * .
     */
    @Test
    public final void testGetCommandArguments() {
        String arg1 = "dada";
        String arg2 = "wowo";
        testee.setArgs(arg1 + " " + arg2);
        String[] arguments = testee.getCommandArguments(new EnvVars());
        assertArrayEquals(new String[] { arg1, arg2 }, arguments);
    }

    /**
     * Test method for
     * {@link hudson.plugins.cmake.BuildToolStep#getEnvironmentVars(hudson.EnvVars, hudson.model.TaskListener)}
     * .
     */
    @Test
    public final void testGetEnvironmentVars() {
        final String var = "OTTOS_MOPS";
        final String value = "KOTZT";
        String envVars = var + "=" + value;

        testee.setEnvVars(envVars);
        Map<String, String> environmentVars = testee.getEnvironmentVars(
                new EnvVars(), new DummyTaskListener());
        assertTrue(environmentVars.containsKey(var));
        assertEquals(value, environmentVars.get(var));
    }

    /**
     * Test method for
     * {@link hudson.plugins.cmake.BuildToolStep#getEnvironmentVars(hudson.EnvVars, hudson.model.TaskListener)}
     * .
     */
    @Test
    public final void testGetEnvironmentVars_noKey() {
        final String var = "";
        final String value = "KOTZT";
        // at least, bash does not accecel 'set =KOTZT'
        String envVars = var + "=" + value;
        testee.setEnvVars(envVars);
        Map<String, String> environmentVars = testee.getEnvironmentVars(
                new EnvVars(), new DummyTaskListener());
        assertFalse(environmentVars.containsKey(var));
    }

    /**
     * Test method for
     * {@link hudson.plugins.cmake.BuildToolStep#getEnvironmentVars(hudson.EnvVars, hudson.model.TaskListener)}
     * .
     */
    @Test
    public final void testGetEnvironmentVars_noValue() {
        final String var = "OTTOS_MOPS";
        final String value = "";
        // at least, bash does not accept 'set OTTOS_MOPS='
        String envVars = var + "=" + value;
        testee.setEnvVars(envVars);
        Map<String, String> environmentVars = testee.getEnvironmentVars(
                new EnvVars(), new DummyTaskListener());
        assertFalse(environmentVars.containsKey(var));

        // at least, bash does not accept 'set OTTOS_MOPS'
        testee.setEnvVars(envVars);
        assertFalse(environmentVars.containsKey(var));
    }

    @Test
    public final void testGetEnvironmentVarsExpand() {
        final String var = "OTTOS_MOPS";
        final String exValue = "KOTZT";
        final String value = "tutwat";
        String envVars = var + "=${" + value + "}";

        testee.setEnvVars(envVars);
        Map<String, String> environmentVars = testee.getEnvironmentVars(
                new EnvVars(value, exValue), new DummyTaskListener());
        assertTrue(environmentVars.containsKey(var));
        assertEquals(exValue, environmentVars.get(var));
    }
}
