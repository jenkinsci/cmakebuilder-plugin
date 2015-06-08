package hudson.plugins.cmake;


import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PathUtilTest {
  @Test
  public void checkBuildCmakeCallPreloadWhitespace() throws Exception {
    CmakeBuilderImpl c = new CmakeBuilderImpl(); 
    
    String res = c.buildCMakeCall(
      "/usr/bin/cmake",
      "Unix Makefiles",
      "",
      "trunk/Modules/AllProjects",
      "path/to/install/dir",
      "Debug",
      ""
      ).trim();

    String desired = new String("/usr/bin/cmake  -G \"Unix Makefiles\" -DCMAKE_INSTALL_PREFIX=path/to/install/dir -DCMAKE_BUILD_TYPE=Debug  \"trunk/Modules/AllProjects\"");

    assertTrue("'" + desired + "' != '" + res + "'",desired.equals(res)); 

  }

  @Test
  public void checkBuildCmakeCallPreloadNull() throws Exception {
    CmakeBuilderImpl c = new CmakeBuilderImpl(); 
    
    String res = c.buildCMakeCall(
      "/usr/bin/cmake",
      "Unix Makefiles",
      null,
      "trunk/Modules/AllProjects",
      "path/to/install/dir",
      "Debug",
      ""
      ).trim();

    String desired = new String("/usr/bin/cmake  -G \"Unix Makefiles\" -DCMAKE_INSTALL_PREFIX=path/to/install/dir -DCMAKE_BUILD_TYPE=Debug  \"trunk/Modules/AllProjects\"");

    assertTrue("'" + desired + "' != '" + res + "'",desired.equals(res)); 

  }

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

  @Test
  public void checkBuildCmakeCallNoBuildType() throws Exception {
    CmakeBuilderImpl c = new CmakeBuilderImpl(); 
    
    String res = c.buildCMakeCall(
      "/usr/bin/cmake",
      "Unix Makefiles",
      "trunk/sscy/overlays/myproj.cmake",
      "trunk/Modules/AllProjects",
      "path/to/install/dir",
      "",
      ""
      ).trim();

    String desired = new String("/usr/bin/cmake  -C \"trunk/sscy/overlays/myproj.cmake\" -G \"Unix Makefiles\" -DCMAKE_INSTALL_PREFIX=path/to/install/dir  \"trunk/Modules/AllProjects\"");

    assertTrue("'" + desired + "' != '" + res + "'",desired.equals(res)); 

  }

  @Test
  public void checkBuildCmakeCallNoInstallDir() throws Exception {
    CmakeBuilderImpl c = new CmakeBuilderImpl(); 
    
    String res = c.buildCMakeCall(
      "/usr/bin/cmake",
      "Unix Makefiles",
      "trunk/sscy/overlays/myproj.cmake",
      "trunk/Modules/AllProjects",
      "",
      "Debug",
      ""
      ).trim();

    String desired = new String("/usr/bin/cmake  -C \"trunk/sscy/overlays/myproj.cmake\" -G \"Unix Makefiles\" -DCMAKE_BUILD_TYPE=Debug  \"trunk/Modules/AllProjects\"");

    assertTrue("'" + desired + "' != '" + res + "'",desired.equals(res)); 

  }


}
