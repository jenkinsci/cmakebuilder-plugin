package hudson.plugins.cmake;

import java.util.Locale;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.BuildWatcherExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import hudson.model.Label;
import hudson.model.Result;

/**
 * Tests the CmakeBuilderStep in a running job. Similar to
 * {@code CmakeBuilderBuildTest}, but for pipeline.
 *
 * @author Martin Weber
 */
@WithJenkins
class CmakeBuilderBuildStepTest {

    @RegisterExtension
    private static final BuildWatcherExtension BUILD_WATCHER = new BuildWatcherExtension();

    @BeforeEach
    void setup() {
        CmakeNotInstalledIgnoreCondition.assumeCmakeInstalled();
    }

    /**
     * Verify that it works on a master.
     */
    @Test
    void testOnMaster(JenkinsRule j) throws Exception {
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
    void testOnSlave(JenkinsRule j) throws Exception {
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
    void testEnvToCmake_1(JenkinsRule j) throws Exception {
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
    void testEnvToCmake_2(JenkinsRule j) throws Exception {
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
    void testBuildToolStep(JenkinsRule j) throws Exception {
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
                + "  cmakeBuild buildDir: 'build',\n"
                + "      label: 'Take a walk on the wild side!',\n"
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
