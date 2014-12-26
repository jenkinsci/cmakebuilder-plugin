package hudson.plugins.cmake;


import org.jvnet.hudson.test.JenkinsRule;
import hudson.model.*;
import org.junit.Test;
import org.junit.Rule;
import static org.junit.Assert.assertTrue;
import hudson.plugins.cmake.EnvVarReplacer;

import java.util.Map;
import java.util.HashMap;

public class EnvVarReplacerTest {

  @Test
  public void checkReplacement() throws Exception {
    String strWithEnvVars = "Is $HOME in $PATH?";
    String desired = "Is /home/jonnyro in /usr/bin/;/usr/local/bin/?";
    HashMap<String,String> envVars = new HashMap<String,String>();

    envVars.put("HOME","/home/jonnyro"); 
    envVars.put("PATH","/usr/bin/;/usr/local/bin/");

    EnvVarReplacer e = new EnvVarReplacer();

    String res = e.replace(strWithEnvVars, envVars);
  
    assertTrue("'" + res + "'!='" + desired + "'", res.equals(desired) ); 

  }


}
