package hudson.plugins.cmake;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import hudson.model.FreeStyleProject;

@WithJenkins
class CToolBuilderFormRoundTripTest {

    @Test
    void checkValidation(JenkinsRule j) throws Exception {

        FreeStyleProject p = j.createFreeStyleProject();
        CToolBuilder before = new CToolBuilder(CmakeTool.DEFAULT);
        p.getBuildersList().add(before);

        j.submit(j.createWebClient().getPage(p, "configure")
                .getFormByName("config"));

        CToolBuilder after = p.getBuildersList().get(CToolBuilder.class);
        assertEquals("cmake", after.getToolId());
        j.assertEqualBeans(
                before,
                after,
                "installationName,arguments,workingDir");

    }

}
