package hudson.plugins.cmake;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.kohsuke.stapler.framework.io.IOException2;

public class CmakeBuilderImpl {

	private static final String DCMAKE_BUILD_TYPE = "-DCMAKE_BUILD_TYPE=";
	private static final String DCMAKE_INSTALL_PREFIX = "-DCMAKE_INSTALL_PREFIX=";
	private static final String BLANK = " ";

	public enum PreparePathOptions
	{
		CHECK_PATH_EXISTS() {
			@Override
			public void process(File file) throws IOException {
				if (!file.exists()) {
					throw new FileNotFoundException(file.getAbsolutePath());
				}
			}
		},
		
		CREATE_IF_NOT_EXISTING() {
			@Override
			public void process(File file) throws IOException {
				if (!file.exists()) {
					if (!file.mkdir()) {
						throw new IOException("Directory could not be created: " + file.getAbsolutePath());
					}
				}
			}
		},
		
		CREATE_NEW_IF_EXISTS() {
			@Override
			public void process(File file) throws IOException {
				if (file.exists()) {
					if (file.isDirectory()) {
						if (!deleteDirectory(file)) {
							throw new IOException("Unable to delete directory " + file.getAbsolutePath());
						}
					}
				}
				CREATE_IF_NOT_EXISTING.process(file);
			}
			
			private boolean deleteDirectory(File path) {
			    if( path.exists() ) {
			      File[] files = path.listFiles();
			      for(int i=0; i<files.length; i++) {
			         if(files[i].isDirectory()) {
			           deleteDirectory(files[i]);
			         }
			         else {
			           files[i].delete();
			         }
			      }
			    }
			    return( path.delete() );
			}
		};
		
		public abstract void process(File file) throws IOException;
	};
	
	public CmakeBuilderImpl() {
		super();
	}
	
	String preparePath(Map<String, String> envVars, String path, PreparePathOptions ppOption) throws IOException {
		path = path.trim();
    	Set<String> keys = envVars.keySet();
    	for (String key : keys) {
    		path   = path.replaceAll("\\$" + key, envVars.get(key));
    	}

    	File file = new File(path);
    	if (!file.isAbsolute()) {
    		path = envVars.get("WORKSPACE") + "/" + path;
    	}
    	file = new File(path);
    	ppOption.process(file);
    	return file.getPath();
	}
	
	String buildCMakeCall(final String cmakeBin, 
			final String generator,
			final String preloadScript,
			final String sourceDir, 
			final String installDir, 
			final String buildType, 
			final String cmakeArgs) {
		return new StringBuilder().append(cmakeBin).append(BLANK)
			.append(createPreloadScriptArg(preloadScript)).append(BLANK)
			.append("-G \"").append(generator).append("\"").append(BLANK)
			.append(DCMAKE_INSTALL_PREFIX).append(installDir).append(BLANK)
			.append(DCMAKE_BUILD_TYPE).append(buildType).append(BLANK)
			.append(cmakeArgs).append(BLANK)
			.append("\"").append(sourceDir).append("\"").append(BLANK).toString();
	}
	
	private String createPreloadScriptArg(final String preloadScript) {
		if (preloadScript == null || preloadScript.trim().isEmpty()) {
			return "";
		}
		return " -C \"" + preloadScript.trim() + "\"";
	}
}
