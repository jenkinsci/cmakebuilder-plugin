package hudson.plugins.cmake;

import hudson.model.FreeStyleProject;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class CmakeBuilderFormRoundTripTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void checkValidation() throws Exception {

        FreeStyleProject p = j.createFreeStyleProject();
        CmakeBuilder before = new CmakeBuilder(CmakeTool.DEFAULT);
        p.getBuildersList().add(before);

        j.submit(j.createWebClient().getPage(p, "configure")
                .getFormByName("config"));

        CmakeBuilder after = p.getBuildersList().get(CmakeBuilder.class);

        j.assertEqualBeans(
                before,
                after,
                "installationName,generator,sourceDir,buildType,cleanBuild,preloadScript,cmakeArgs");
        assertEquals(after.getBuildDir(),"build");
    }

}
