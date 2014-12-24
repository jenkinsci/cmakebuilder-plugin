
import org.jvnet.hudson.test.JenkinsRule;
import hudson.model.*;
import org.junit.Test;
import hudson.plugins.cmake.CmakeBuilder;


public class FieldValidationTest {

  @Test
  public void checkValidation() throws Exception {

  CmakeBuilder c = new CmakeBuilder(
        "trunk/CMakeModules/3rdparty",
        "Buildarea/cmake/3rdparty/Debug",
        "",
        "Debug",
        false,
        false,
        "Unix Makefiles",
        "make",
        "",
        "",
        "",
        "");

    assert(true);


  }


}
