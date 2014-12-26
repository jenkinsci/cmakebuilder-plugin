package hudson.plugins.cmake;


import org.jvnet.hudson.test.JenkinsRule;
import hudson.model.*;
import org.junit.Test;
import org.junit.Rule;
import static org.junit.Assert.assertTrue;
import hudson.plugins.cmake.CmakeBuilderImpl;

import java.util.Map;
import java.util.HashMap;

public class PathUtilTest {

  @Test
  public void checkBuildCmakeCall() throws Exception {
    CmakeBuilderImpl c = new CmakeBuilderImpl(); 
    
    String res = c.buildCMakeCall(
      "/usr/bin/cmake",
      "Unix Makefiles",
      "trunk/sscy/overlays/myproj.cmake",
      "trunk/Modules/AllProjects",
      "path/to/install/dir",
      "Debug",
      ""
      ).trim();

    String desired = new String("/usr/bin/cmake  -C \"trunk/sscy/overlays/myproj.cmake\" -G \"Unix Makefiles\" -DCMAKE_INSTALL_PREFIX=path/to/install/dir -DCMAKE_BUILD_TYPE=Debug  \"trunk/Modules/AllProjects\"");

    assertTrue("'" + desired + "' != '" + res + "'",desired.equals(res)); 

  }


}
