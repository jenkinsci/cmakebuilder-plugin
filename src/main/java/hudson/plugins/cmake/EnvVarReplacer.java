package hudson.plugins.cmake;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

class StringLengthComparator implements Comparator<String>
{
	@Override
	public int compare(String lhs, String rhs)
	{
		return rhs.length() - lhs.length();
	}
}

public class EnvVarReplacer {
	
	public static String replace(String stringContainingEnvVars, Map<String, String> envVars) {
		List<String> keys = new ArrayList<String>(envVars.keySet());
		Collections.sort(keys, new StringLengthComparator());

    	for (String key : keys) {
    		stringContainingEnvVars = 
    			stringContainingEnvVars.replaceAll("\\$" + key, envVars.get(key));
    	}
    	return stringContainingEnvVars;
	}

}
