package hudson.plugins.cmake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

import org.jenkinsci.plugins.structs.SymbolLookup;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import hudson.EnvVars;
import hudson.model.Result;
import hudson.tools.InstallSourceProperty;
import io.jenkins.plugins.casc.ConfigurationAsCode;

/**
 * Verifies {@code @Symbol} annotations, JCasC configuration round-trip, and
 * Declarative Pipeline {@code tools { cmake ... }} support.
 */
@WithJenkins
class CmakeToolSymbolAndCascTest {

    @Test
    void testSymbolLookup(JenkinsRule j) {
        assertNotNull(SymbolLookup.get().findDescriptor(CmakeTool.class, "cmake"),
                "CmakeTool descriptor should expose @Symbol(\"cmake\")");
        assertNotNull(
                SymbolLookup.get().findDescriptor(CmakeTool.class, "cmakeTool"),
                "CmakeTool descriptor should preserve legacy \"cmakeTool\" symbol alias");
        assertNotNull(
                SymbolLookup.get().findDescriptor(CmakeInstaller.class,
                        "cmakeInstaller"),
                "CmakeInstaller descriptor should expose @Symbol(\"cmakeInstaller\")");
    }

    @Test
    void testLegacyJcascCmakeToolAlias(JenkinsRule j) throws Exception {
        String yamlUrl = getClass().getResource("casc-legacy.yaml")
                .toExternalForm();
        ConfigurationAsCode.get().configure(yamlUrl);

        CmakeTool.DescriptorImpl descriptor = j.jenkins
                .getDescriptorByType(CmakeTool.DescriptorImpl.class);
        CmakeTool[] installations = descriptor.getInstallations();
        assertEquals(1, installations.length);
        assertEquals("cmake-legacy-jcasc", installations[0].getName());
        assertEquals("/opt/cmake/bin", installations[0].getHome());
    }

    @Test
    void testJcascConfigureAndExport(JenkinsRule j) throws Exception {
        String yamlUrl = getClass().getResource("casc.yaml").toExternalForm();
        ConfigurationAsCode.get().configure(yamlUrl);

        CmakeTool.DescriptorImpl descriptor = j.jenkins
                .getDescriptorByType(CmakeTool.DescriptorImpl.class);
        CmakeTool[] installations = descriptor.getInstallations();
        assertEquals(2, installations.length);

        assertEquals("InSearchPath", installations[0].getName());
        assertEquals("cmake-3.31", installations[1].getName());

        InstallSourceProperty isp = installations[1].getProperties()
                .get(InstallSourceProperty.class);
        assertNotNull(isp);
        CmakeInstaller installer = isp.installers.get(CmakeInstaller.class);
        assertNotNull(installer);
        assertEquals("3.31.6", installer.id);

        ByteArrayOutputStream exported = new ByteArrayOutputStream();
        ConfigurationAsCode.get().export(exported);
        String exportedYaml = exported.toString(StandardCharsets.UTF_8);
        assertTrue(exportedYaml.contains("cmake:"), exportedYaml);
        assertTrue(exportedYaml.contains("cmakeInstaller:"), exportedYaml);
        assertTrue(exportedYaml.contains("cmake-3.31"), exportedYaml);
    }

    @Test
    void testDeclarativePipelineToolsBlock(JenkinsRule j) throws Exception {
        File customHome = Files.createTempDirectory("cmake-mock-bin").toFile();
        CmakeTool customTool = new CmakeTool("cmake-custom",
                customHome.getAbsolutePath(), Collections.emptyList());
        j.jenkins.getDescriptorByType(CmakeTool.DescriptorImpl.class)
                .setInstallations(customTool);

        EnvVars env = new EnvVars();
        customTool.buildEnvVars(env);
        assertEquals(customHome.getAbsolutePath(), env.get("PATH+CMAKE"));

        WorkflowJob job = j.createProject(WorkflowJob.class,
                "declarative-cmake-tools");
        job.setDefinition(new CpsFlowDefinition(
                "pipeline {\n"
                        + "  agent any\n"
                        + "  tools {\n"
                        + "    cmake 'cmake-custom'\n"
                        + "  }\n"
                        + "  stages {\n"
                        + "    stage('Check PATH') {\n"
                        + "      steps {\n"
                        + "        echo 'RESOLVED_PATH=' + env.PATH\n"
                        + "      }\n"
                        + "    }\n"
                        + "  }\n"
                        + "}\n",
                true));

        WorkflowRun build = j.assertBuildStatus(Result.SUCCESS,
                job.scheduleBuild2(0));
        j.assertLogContains(customHome.getAbsolutePath(), build);
    }
}
