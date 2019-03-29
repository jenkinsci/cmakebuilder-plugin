package hudson.plugins.cmake;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.geode.test.junit.ConditionalIgnore;
import org.apache.geode.test.junit.rules.ConditionalIgnoreRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SingleFileSCM;
import org.jvnet.hudson.test.TestBuilder;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;
import hudson.slaves.DumbSlave;

/**
 * Tests the CmakeBuilder in a running job.
 *
 * @author Martin Weber
 */
public class CmakeBuilderBuildTest {

    @Rule
    public ConditionalIgnoreRule conditionalIgnoreRule = new ConditionalIgnoreRule();
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
    @ConditionalIgnore(value="SKIPPED: cmake tool not installed",condition = CmakeNotInstalledIgnoreCondition.class)
    public void testOnMaster() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.setScm(scm);

        CmakeBuilder cmb = new CmakeBuilder(CmakeTool.DEFAULT);
        cmb.setCleanBuild(true);
        cmb.setSourceDir("src");
        cmb.setBuildDir("src");
        p.getBuildersList().add(cmb);

        FreeStyleBuild build = p.scheduleBuild2(0).get();
        System.out.println(JenkinsRule.getLog(build));
        j.assertBuildStatusSuccess(build);
    }

    /**
     * Verify that it works on a slave.
     */
    @Test
    @ConditionalIgnore(value="SKIPPED: cmake tool not installed",condition = CmakeNotInstalledIgnoreCondition.class)
    public void testOnSlave() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.setScm(scm);

        // create a slave with a given label to execute projectB on
        DumbSlave slave = j.createOnlineSlave(Label
                .get("cmakebuilder-test-slave"));
        p.setAssignedLabel(slave.getSelfLabel());

        CmakeBuilder cmb = new CmakeBuilder(CmakeTool.DEFAULT);
        cmb.setCleanBuild(true);
        cmb.setSourceDir("src");
        cmb.setBuildDir("src");
        p.getBuildersList().add(cmb);

        FreeStyleBuild build = p.scheduleBuild2(0).get();
        System.out.println(JenkinsRule.getLog(build));
        j.assertBuildStatusSuccess(build);
    }

    /**
     * Verifies that build variable get expanded.
     */
    @Test
    @ConditionalIgnore(value="SKIPPED: cmake tool not installed",condition = CmakeNotInstalledIgnoreCondition.class)
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

        CmakeBuilder cmb = new CmakeBuilder(CmakeTool.DEFAULT);
        cmb.setBuildType("${BUILDTYPE}");
        cmb.setCleanBuild(true);
        cmb.setSourceDir("${SOURCEDIR}");
        cmb.setBuildDir("${BUILDDIR}");
        cmb.setArguments("${CMAKEARGS}");
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

    /**
     * Verifies that the build-tool variable gets injected.
     */
    @Test
    @ConditionalIgnore(value="SKIPPED: cmake tool not installed",condition = CmakeNotInstalledIgnoreCondition.class)
    public void testBuildToolVariableInjected() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.setScm(scm);
        CmakeBuilder cmb = new CmakeBuilder(CmakeTool.DEFAULT);
        cmb.setCleanBuild(true);
        cmb.setSourceDir("src");
        cmb.setBuildDir("build");
        p.getBuildersList().add(cmb);
        GetEnvVarBuilder gevb = new GetEnvVarBuilder();
        p.getBuildersList().add(gevb);

        FreeStyleBuild build = p.scheduleBuild2(0).get();
        System.out.println(JenkinsRule.getLog(build));
        j.assertBuildStatusSuccess(build);

        assertNotNull(CmakeBuilder.ENV_VAR_NAME_CMAKE_BUILD_TOOL, gevb.value);
    }

    /**
     * Verify that direct build tool invocations work.
     */
    @Test
    @ConditionalIgnore(value="SKIPPED: cmake tool not installed",condition = CmakeNotInstalledIgnoreCondition.class)
    public void testBuildToolStep() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.setScm(scm);

        CmakeBuilder cmb = new CmakeBuilder(CmakeTool.DEFAULT);
        cmb.setCleanBuild(true);
        cmb.setSourceDir("src");
        cmb.setBuildDir("build/debug");
        p.getBuildersList().add(cmb);
        ArrayList<BuildToolStep> steps = new ArrayList<BuildToolStep>(2);
        // let the build invoke 'make clean all'..
        BuildToolStep step = new BuildToolStep();
        final String makeTargets = "clean all";
        step.setArgs(makeTargets);
        steps.add(step);
        // let the build invoke 'make rebuild_cache'..
        step = new BuildToolStep();
        String makeTargets2 = "rebuild_cache";
        step.setArgs(makeTargets2);
        steps.add(step);
        cmb.setSteps(steps);

        FreeStyleBuild build = p.scheduleBuild2(0).get();
        System.out.println(JenkinsRule.getLog(build));
        j.assertLogContains(makeTargets, build);
        j.assertLogContains(makeTargets2, build);

        j.assertBuildStatusSuccess(build);
    }

    /**
     * Verify that build tool invocations with 'cmake --build' work.
     * Test will fail with cmake >= 2.8
     */
    @Test
    @ConditionalIgnore(value="SKIPPED: cmake tool not installed",condition = CmakeNotInstalledIgnoreCondition.class)
    public void testBuildToolStepWithCmake() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.setScm(scm);

        CmakeBuilder cmb = new CmakeBuilder(CmakeTool.DEFAULT);
        cmb.setCleanBuild(true);
        cmb.setSourceDir("src");
        cmb.setBuildDir("build/debug");
        p.getBuildersList().add(cmb);
        ArrayList<BuildToolStep> steps = new ArrayList<BuildToolStep>(2);
        // let the build invoke 'cmake --build <dir> clean all'..
        BuildToolStep step = new BuildToolStep();
        step.setWithCmake(true);
        final String makeTargets = "--target all";
        step.setArgs(makeTargets);
        steps.add(step);
        // let the build invoke 'cmake --build <dir> rebuild_cache'..
        step = new BuildToolStep();
        step.setWithCmake(true);
        String makeTargets2 = "--target rebuild_cache";
        step.setArgs(makeTargets2);
        steps.add(step);
        cmb.setSteps(steps);

        FreeStyleBuild build = p.scheduleBuild2(0).get();
        System.out.println(JenkinsRule.getLog(build));
        j.assertLogContains(makeTargets, build);
        j.assertLogContains(makeTargets2, build);

        j.assertBuildStatusSuccess(build);
    }

    // //////////////////////////////////////////////////////////////////
    // inner classes
    // //////////////////////////////////////////////////////////////////
    private static class GetEnvVarBuilder extends TestBuilder {
        String value;

        /*-
         * @see org.jvnet.hudson.test.TestBuilder#perform(hudson.model.AbstractBuild, hudson.Launcher, hudson.model.BuildListener)
         */
        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                BuildListener listener) throws InterruptedException,
                IOException {
            value = build.getEnvironment(listener).get(
                    CmakeBuilder.ENV_VAR_NAME_CMAKE_BUILD_TOOL);
            return true;
        }

    }
}
