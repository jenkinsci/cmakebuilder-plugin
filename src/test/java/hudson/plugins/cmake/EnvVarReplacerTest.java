package hudson.plugins.cmake;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Test;

public class EnvVarReplacerTest {

  @Test
  public void checkReplacement() throws Exception {
    String strWithEnvVars = "Is $HOME in $PATH?";
    String desired = "Is /home/jonnyro in /usr/bin/;/usr/local/bin/?";
    HashMap<String,String> envVars = new HashMap<String,String>();

    envVars.put("HOME","/home/jonnyro");
    envVars.put("PATH","/usr/bin/;/usr/local/bin/");

    String res = EnvVarReplacer.replace(strWithEnvVars, envVars);

    assertTrue("'" + res + "'!='" + desired + "'", res.equals(desired) );
  }

  @Test
  // @Issue("JENKINS-27203")
  public final void testExpandLengthDependent() {
    String strWithEnvVars = "Is $HOME in? $NODE $NODE_ID";
    String expected = "Is $HOME in? abc123 987";

    HashMap<String,String> envVars = new HashMap<String,String>();
    envVars.put("NODE","abc123");
    envVars.put("NODE_ID","987");
    String res = EnvVarReplacer.replace(strWithEnvVars, envVars);
    assertEquals(expected,res);
  }

  @Test
  public final void testBracketedFrorm() {
    HashMap<String,String> envVars = new HashMap<String,String>();
    envVars.put("NODE","abc123");
    envVars.put("NODE_ID","987");
    // test whether the usual syntax of jenkins is supported
    String strWithEnvVars = "Is ${HOME} in? ${NODE} ${NODE_ID}";
    String res = EnvVarReplacer.replace(strWithEnvVars, envVars);
    assertEquals("Is ${HOME} in? abc123 987",res);
  }
}
