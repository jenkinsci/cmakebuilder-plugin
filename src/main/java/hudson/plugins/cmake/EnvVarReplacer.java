package hudson.plugins.cmake;

import java.util.Map;

public class EnvVarReplacer {

	public static String replace(String stringContainingEnvVars, Map<String, String> envVars) {
	  return hudson.Util.replaceMacro(stringContainingEnvVars, envVars);
	}
}
