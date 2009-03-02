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
	
	String buildCMakeCall(String cmakeBin, String sourceDir, String installDir, String buildType, String cmakeArgs) {
		String cmakeCall = cmakeBin + " " + sourceDir + " "
		+ DCMAKE_INSTALL_PREFIX + installDir + " "
		+ DCMAKE_BUILD_TYPE + buildType + " "
		+ cmakeArgs;
		return cmakeCall;
	}
}
