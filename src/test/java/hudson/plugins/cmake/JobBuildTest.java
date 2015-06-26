package hudson.plugins.cmake;

import hudson.model.FreeStyleBuild;
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

/**
 * Tests the CmakeBuilder in a running job.
 *
 * @author Martin Weber
 */
public class JobBuildTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private MultiFileSCM scm;

    @Before
    public void setup() throws IOException {
        List<SingleFileSCM> files = new ArrayList<SingleFileSCM>(2);
        files.add(new SingleFileSCM("src/CMakeLists.txt", getClass()
                .getResource("CMakeLists.txt")));
        files.add(new SingleFileSCM("src/tester.cpp", getClass().getResource(
                "tester.cpp")));
        scm = new MultiFileSCM(files);
    }

    /**
     * Verify that it works on a master.
     */
    @Test
    public void testOnMaster() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.setScm(scm);

        CmakeBuilder cmb = new CmakeBuilder(CmakeTool.DEFAULT,
                "Unix Makefiles", "src", "build/Debug", "make");
        cmb.setCleanBuild(true);
        cmb.setCleanInstallDir(true);
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
        p.setScm(scm);

        // create a slave with a given label to execute projectB on
        DumbSlave slave = j.createOnlineSlave(Label
                .get("cmakebuilder-test-slave"));
        p.setAssignedLabel(slave.getSelfLabel());

        CmakeBuilder cmb = new CmakeBuilder(CmakeTool.DEFAULT,
                "Unix Makefiles", "src", "build/Debug", "make");
        cmb.setCleanBuild(true);
        cmb.setCleanInstallDir(true);
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
        p.setScm(scm);

        StringParameterDefinition pd1 = new StringParameterDefinition(
                "SOURCEDIR", "src");
        StringParameterDefinition pd2 = new StringParameterDefinition(
                "BUILDDIR", "build/Debug");
        StringParameterDefinition pd3 = new StringParameterDefinition(
                "BUILDTYPE", "Release");
        StringParameterDefinition pd4 = new StringParameterDefinition(
                "BUILDGENERATOR", "Unix Makefiles");
        StringParameterDefinition pd5 = new StringParameterDefinition(
                "BUILDTOOL", "make");
        StringParameterDefinition pd6 = new StringParameterDefinition(
                "PRESCRIPT", "setup/cmake-cache-preload.txt");
        StringParameterDefinition pd7 = new StringParameterDefinition(
                "CMAKEARGS", "--warn-unused-vars -Wno-dev");
        ParametersDefinitionProperty pdp = new ParametersDefinitionProperty(
                pd1, pd2, pd3, pd4, pd5, pd6, pd7);
        p.addProperty(pdp);

        CmakeBuilder cmb = new CmakeBuilder(CmakeTool.DEFAULT,
                "${BUILDGENERATOR}", "${SOURCEDIR}", "${BUILDDIR}", "make");
        cmb.setBuildType("${BUILDTYPE}");
        cmb.setCleanBuild(true);
        cmb.setCleanInstallDir(true);
        cmb.setCmakeArgs("${CMAKEARGS}");
        cmb.setPreloadScript("${PRESCRIPT}");

        p.getBuildersList().add(cmb);

        FreeStyleBuild build = p.scheduleBuild2(0).get();
        System.out.println(JenkinsRule.getLog(build));

        // verify in the log
        List<String> varNames = pdp.getParameterDefinitionNames();
        for (String varName : varNames) {
            j.assertLogNotContains("${" + varName + "}", build);
        }
    }

}
