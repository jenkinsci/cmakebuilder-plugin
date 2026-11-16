package hudson.plugins.cmake;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import hudson.model.FreeStyleProject;

@WithJenkins
class CmakeBuilderFormRoundTripTest {

    @Test
    void checkValidation(JenkinsRule j) throws Exception {

        FreeStyleProject p = j.createFreeStyleProject();
        CmakeBuilder before = new CmakeBuilder(CmakeTool.DEFAULT);
        p.getBuildersList().add(before);

        j.submit(j.createWebClient().getPage(p, "configure")
                .getFormByName("config"));

        CmakeBuilder after = p.getBuildersList().get(CmakeBuilder.class);

        j.assertEqualBeans(
                before,
                after,
                "installationName,generator,sourceDir,buildType,cleanBuild,preloadScript,preset,cmakeArgs");
        assertEquals("build", after.getBuildDir());
    }

}
