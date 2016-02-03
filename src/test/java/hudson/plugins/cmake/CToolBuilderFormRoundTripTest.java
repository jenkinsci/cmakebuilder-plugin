package hudson.plugins.cmake;

import hudson.model.FreeStyleProject;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class CToolBuilderFormRoundTripTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void checkValidation() throws Exception {

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
