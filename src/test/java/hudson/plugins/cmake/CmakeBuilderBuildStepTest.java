package hudson.plugins.cmake;

import java.util.Locale;

import org.apache.geode.test.junit.ConditionalIgnore;
import org.apache.geode.test.junit.rules.ConditionalIgnoreRule;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.Label;
import hudson.model.Result;

/**
 * Tests the CmakeBuilderStep in a running job. Similar to
 * {@code CmakeBuilderBuildTest}, but for pipeline.
 *
 * @author Martin Weber
 */
public class CmakeBuilderBuildStepTest {

    @Rule
    public ConditionalIgnoreRule conditionalIgnoreRule = new ConditionalIgnoreRule();
    @Rule
    public JenkinsRule j = new JenkinsRule();
    @ClassRule
    public static BuildWatcher bw = new BuildWatcher();

    /**
     * Verify that it works on a master.
     */
    @Test
    @ConditionalIgnore(value = "SKIPPED: cmake tool not installed", condition = CmakeNotInstalledIgnoreCondition.class)
    public void testOnMaster() throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class);
        String cMakeLists = "cmake_minimum_required(VERSION 3.10)\n"
                + "project(test C)\n" + "message(STATUS \"success!\")\n";
        String script = String.format(Locale.ENGLISH,
                "node {\n" + "  writeFile text: '''\n" + "%s\n''',\n"
                        + "     file: 'CMakeLists.txt'\n"
                        // + " sh 'cat CMakeLists.txt'\n"
                        + "  cmakeBuild buildDir: 'build',"
                        + "      buildType: 'Debug',\n"
                        + "      installation: '%s',\n" + "      sourceDir: '',"
                        + "      cleanBuild: true,\n"
                        + "      steps: [[args: 'all']]\n" + "}",
                cMakeLists, CmakeTool.DEFAULT);

        p.setDefinition(new CpsFlowDefinition(script, true));
        WorkflowRun build = p.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.SUCCESS, build);
    }

    /**
     * Verify that it works on a slave.
     */
    @Test
    @ConditionalIgnore(value = "SKIPPED: cmake tool not installed", condition = CmakeNotInstalledIgnoreCondition.class)
    public void testOnSlave() throws Exception {
        final String NODE_NAME = "cmakebuilder-test-slave";

        WorkflowJob p = j.createProject(WorkflowJob.class);
        String cMakeLists = "cmake_minimum_required(VERSION 3.10)\n"
                + "project(test C)\n" + "add_executable(main main.c)\n";
        String cCode = "int main(int argc, char** argv) { return 0; }";

        String script = String.format(Locale.ENGLISH,
                "node ('%s') {\n" + "  writeFile text: '''\n" + "%s\n''',\n"
                        + "     file: 'CMakeLists.txt'\n"
                        + "  writeFile text: '''\n" + "%s\n''',\n"
                        + "     file: 'main.c'\n"
                        // + " sh 'cat CMakeLists.txt'\n"
                        + "  cmakeBuild buildDir: 'build',"
                        + "      buildType: 'Debug',\n"
                        + "      installation: '%s',\n" + "      sourceDir: '',"
                        + "      cleanBuild: true,\n"
                        + "      steps: [[args: 'all']]\n" + "}",
                        NODE_NAME, cMakeLists, cCode, CmakeTool.DEFAULT);

        p.setDefinition(new CpsFlowDefinition(script, true));
        // create a slave with a given label to execute projectB on
        j.createOnlineSlave(Label.get(NODE_NAME));
        WorkflowRun build = p.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.SUCCESS, build);
    }

    /**
     * Verifies that environment variable are passed to cmake.
     */
    @Test
    @ConditionalIgnore(value = "SKIPPED: cmake tool not installed", condition = CmakeNotInstalledIgnoreCondition.class)
    public void testEnvToCmake_1() throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class);
        String cMakeLists = "cmake_minimum_required(VERSION 3.10)\n"
                + "project(test C)\n"
                + "message(STATUS \"LIB value = $ENV{LIB}\")\n";
        // this will of course make cmake fail since CC=TOOL_NOT_INSTALLED isnt
        // there
        String script = String.format(Locale.ENGLISH,
                "node {\n" + "  writeFile text: '''\n" + "%s\n''',\n"
                        + "     file: 'CMakeLists.txt'\n"
                        // + " sh 'cat CMakeLists.txt'\n"
                        + "  withEnv(['CC=TOOL_NOT_INSTALLED', 'CXX=TOOL_NOT_INSTALLED++']) {\n"
                        + "    cmakeBuild buildDir: 'build',"
                        + "               buildType: 'Debug',\n"
                        + "               installation: '%s',\n"
                        + "               sourceDir: '',"
                        + "               cleanBuild: true,\n"
                        + "               steps: [[args: 'all']]\n" + "  }\n"
                        + "}",
                cMakeLists, CmakeTool.DEFAULT);

        p.setDefinition(new CpsFlowDefinition(script, true));
        WorkflowRun build = p.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, build);
        j.assertLogContains("CMake Error: CMAKE_C_COMPILER not set", build);
    }

    /**
     * Verifies that environment variable are passed to cmake.
     */
    @Test
    @ConditionalIgnore(value = "SKIPPED: cmake tool not installed", condition = CmakeNotInstalledIgnoreCondition.class)
    public void testEnvToCmake_2() throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class);
        String envValue = "XYZabcDEF";
        String cMakeLists = "cmake_minimum_required(VERSION 3.10)\n"
                + "project(test C)\n"
                + "message(STATUS \"ENVVAR value= $ENV{ENVVAR}\")\n";
        // this will of course make cmake fail since CC=TOOL_NOT_INSTALLED isnt
        // there
        String script = String.format(Locale.ENGLISH, "node {\n"
                + "  writeFile text: '''\n" + "%s\n''',\n"
                + "     file: 'CMakeLists.txt'\n"
//                + "  sh 'cat CMakeLists.txt'\n"
                + "  withEnv(['ENVVAR=%s']) {\n"
                + "    cmakeBuild buildDir: 'build',"
                + "               buildType: 'Debug',\n"
                + "               installation: '%s',\n"
                + "               sourceDir: '',"
                + "               cleanBuild: true,\n"
                + "               steps: [[args: 'all', envVars: 'DESTDIR=balh/Blah']]\n"
                + "  }\n" + "}", cMakeLists, envValue, CmakeTool.DEFAULT);

        p.setDefinition(new CpsFlowDefinition(script, true));
        WorkflowRun build = p.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.SUCCESS, build);
        j.assertLogContains("ENVVAR value= " + envValue, build);
    }

    /**
     * Verify that build tool invocations work.
     */
    @Test
    @ConditionalIgnore(value = "SKIPPED: cmake tool not installed", condition = CmakeNotInstalledIgnoreCondition.class)
    public void testBuildToolStep() throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class);
        // let the build invoke 'make clean all'..
        final String makeTargets = "clean all";
        // let the build invoke 'make rebuild_cache'..
        String makeTargets2 = "rebuild_cache";

        String cMakeLists = "cmake_minimum_required(VERSION 3.10)\n"
                + "project(test C)\n" + "add_executable(main main.c)\n";
        String cCode = "int main(int argc, char** argv) { return 0; }";

        // @formatter:off
        String script = String.format(Locale.ENGLISH, "node {\n"
                + "  writeFile text: '''\n" + "%s\n''',\n"
                + "     file: 'CMakeLists.txt'\n"
                + "  writeFile text: '''\n" + "%s\n''',\n"
                + "     file: 'main.c'\n"
//                + " sh 'cat CMakeLists.txt'\n"
//                + " sh 'cat main.c'\n"
                + "  cmakeBuild buildDir: 'build',"
                + "      buildType: 'Debug',\n" + "      installation: '%s',\n"
                + "      sourceDir: ''," + "      cleanBuild: true,\n"
                + "      steps: [\n"
                + "          [args: '%s'],\n"
                + "          [args: '%s']\n"
                + "      ]\n" + "}",
                // @formatter:on
                cMakeLists, cCode, CmakeTool.DEFAULT, makeTargets, makeTargets2);

        p.setDefinition(new CpsFlowDefinition(script, true));
        WorkflowRun build = p.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.SUCCESS, build);

        j.assertLogContains(makeTargets, build);
        j.assertLogContains(makeTargets2, build);
    }
}
