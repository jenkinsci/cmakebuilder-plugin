package hudson.plugins.cmake;

import static org.junit.Assert.assertNotNull;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;
import hudson.slaves.DumbSlave;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SingleFileSCM;
import org.jvnet.hudson.test.TestBuilder;

/**
 * Tests the CToolBuilder in a running job.
 *
 * @author Martin Weber
 */
public class CToolBuilderBuildTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    /**
     * Verify that it works on a master.
     */
    @Test
    public void testOnMaster() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        CToolBuilder cmb = new CToolBuilder(CmakeTool.DEFAULT);
        cmb.setArguments("-version");
        cmb.setWorkingDir("workdir");
        p.getBuildersList().add(cmb);

        FreeStyleBuild build = p.scheduleBuild2(0).get();
        System.out.println(JenkinsRule.getLog(build));
        j.assertBuildStatusSuccess(build);
    }

    /**
     * Verify that it works on a slave.
     */
    @Test
    public void testOnSlave() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        // create a slave with a given label to execute projectB on
        DumbSlave slave = j
                .createOnlineSlave(Label.get("cmakebuilder-test-slave"));
        p.setAssignedLabel(slave.getSelfLabel());

        CToolBuilder cmb = new CToolBuilder(CmakeTool.DEFAULT);
        cmb.setArguments("-version");
        cmb.setWorkingDir("workdir");
        p.getBuildersList().add(cmb);

        FreeStyleBuild build = p.scheduleBuild2(0).get();
        System.out.println(JenkinsRule.getLog(build));
        j.assertBuildStatusSuccess(build);
    }

    /**
     * Verifies that build variable get expanded.
     */
    @Test
    public void testBuildVariables() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        StringParameterDefinition pd1 = new StringParameterDefinition("WORKDIR",
                "build/Debug");
        StringParameterDefinition pd2 = new StringParameterDefinition(
                "CMAKEARGS",
                "-G \"Unix Makefiles\" --warn-unused-vars -Wno-dev");
        ParametersDefinitionProperty pdp = new ParametersDefinitionProperty(pd1,
                pd2);
        p.addProperty(pdp);

        CToolBuilder cmb = new CToolBuilder(CmakeTool.DEFAULT);
        cmb.setArguments("${CMAKEARGS}");
        cmb.setWorkingDir("${WORKDIR}");

        p.getBuildersList().add(cmb);

        FreeStyleBuild build = p.scheduleBuild2(0).get();
        System.out.println(JenkinsRule.getLog(build));

        // verify in the log
        List<String> varNames = pdp.getParameterDefinitionNames();
        for (String varName : varNames) {
            j.assertLogNotContains("${" + varName + "}", build);
        }
    }

    /**
     * Verifies that the build-tool variable gets injected.
     */
    @Test
    public void testBuildToolVariableInjected() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        CToolBuilder cmb = new CToolBuilder(CmakeTool.DEFAULT);
        cmb.setArguments("-version");
        cmb.setWorkingDir("workdir");
        p.getBuildersList().add(cmb);

        FreeStyleBuild build = p.scheduleBuild2(0).get();
        System.out.println(JenkinsRule.getLog(build));
        j.assertBuildStatusSuccess(build);

    }
}
