package hudson.plugins.cmake;


import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Test;

public class EnvVarReplacerTest {

  @Test
  public void checkReplacement() throws Exception {
    String strWithEnvVars = "Is $HOME in $PATH? $NODE $NODE_ID";
    String desired = "Is /home/jonnyro in /usr/bin/;/usr/local/bin/? abc123 987";
    HashMap<String,String> envVars = new HashMap<String,String>();

    envVars.put("HOME","/home/jonnyro"); 
    envVars.put("PATH","/usr/bin/;/usr/local/bin/");
    envVars.put("NODE","abc123");
    envVars.put("NODE_ID","987");

    EnvVarReplacer e = new EnvVarReplacer();

    String res = e.replace(strWithEnvVars, envVars);
  
    assertTrue("'" + res + "'!='" + desired + "'", res.equals(desired) ); 

  }


}
