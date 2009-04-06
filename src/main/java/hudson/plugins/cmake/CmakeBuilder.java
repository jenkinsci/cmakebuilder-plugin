package hudson.plugins.cmake;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.util.FormFieldValidator;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link CmakeBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #name})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(Build, Launcher, BuildListener)} method
 * will be invoked. 
 *
 * @author Kohsuke Kawaguchi
 */
public class CmakeBuilder extends Builder {

	private static final String CMAKE = "cmake";
	private static final String MAKE = "make";
	private static final String MAKE_INSTALL = "make install";
	
	private String sourceDir;
    private String buildDir;
    private String installDir;
    private String buildType;
    private String cmakeArgs;
    private boolean cleanBuild;

    private CmakeBuilderImpl builderImpl;
    
    @DataBoundConstructor
    public CmakeBuilder(String sourceDir, String buildDir, String installDir, String buildType, String cmakeArgs) {
    	this.sourceDir = sourceDir;
		this.buildDir = buildDir;
		this.installDir = installDir;
		this.buildType = buildType;
		this.cmakeArgs = cmakeArgs;
		this.cleanBuild = false;
		builderImpl = new CmakeBuilderImpl();		
    }

    public String getSourceDir() {
    	return this.sourceDir;
    }
    
    public String getBuildDir() {
		return this.buildDir;
	}

    public String getInstallDir() {
    	return this.installDir;
    }

    public String getBuildType() {
    	return this.buildType;
    }
    
    public String getCmakeArgs() {
    	return this.cmakeArgs;
    }

    public boolean getCleanBuild() {
    	return this.cleanBuild;
    }
    
    public boolean perform(Build build, Launcher launcher, BuildListener listener) {
    	listener.getLogger().println("MODULE: " + build.getProject().getModuleRoot());
    	
    	if (builderImpl == null) {
    		builderImpl = new CmakeBuilderImpl();
    	}
    	String theSourceDir = ""; 
    	String theInstallDir = ""; 
    	try {
//    		if (this.cleanBuild) {
//    			build.getProject().getWorkspace().deleteRecursive();
//    		}
    		builderImpl.preparePath(build.getEnvVars(), this.buildDir, 
    				CmakeBuilderImpl.PreparePathOptions.CREATE_NEW_IF_EXISTS);
    		theSourceDir = builderImpl.preparePath(build.getEnvVars(), this.sourceDir,
    				CmakeBuilderImpl.PreparePathOptions.CHECK_PATH_EXISTS);
    		theInstallDir = builderImpl.preparePath(build.getEnvVars(), this.installDir,
    				CmakeBuilderImpl.PreparePathOptions.CREATE_NEW_IF_EXISTS);
    	} catch (IOException ioe) {
    		listener.getLogger().println(ioe.getMessage());
    		return false;
    	} 
//    	catch (InterruptedException e) {
//    		listener.getLogger().println(e.getMessage());
//			return false;
//		}
    	
    	String cmakeBin = CMAKE;
    	if (DESCRIPTOR.cmakePath() != null && DESCRIPTOR.cmakePath().length() > 0) {
    		cmakeBin = DESCRIPTOR.cmakePath();
    	}
    	String cmakeCall = builderImpl.buildCMakeCall(cmakeBin, theSourceDir, theInstallDir, buildType, cmakeArgs);
    	FilePath workDir = new FilePath(build.getProject().getWorkspace(), this.buildDir); 
    	listener.getLogger().println("CMake call : " + cmakeCall);

    	try {
    		Proc proc = launcher.launch(cmakeCall, build.getEnvVars(), listener.getLogger(), workDir);
    		int result = proc.join();
    		if (result != 0) {
    			return false;
    		}
    		
    		proc = launcher.launch(MAKE, build.getEnvVars(), listener.getLogger(), workDir);
    		result = proc.join();
    		if (result != 0) {
    			return false;
    		}
    		proc = launcher.launch(MAKE_INSTALL, build.getEnvVars(), listener.getLogger(), workDir);
    		result = proc.join();
    		return (result == 0);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
		return false;
    }

    public Descriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }

    /**
     * Descriptor should be singleton.
     */
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    /**
     * Descriptor for {@link CmakeBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>views/hudson/plugins/hello_world/CmakeBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    public static final class DescriptorImpl extends Descriptor<Builder> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private String cmakePath;
        private transient List<String> allowedBuildTypes;
        private transient String errorMessage;
        
        DescriptorImpl() {
            super(CmakeBuilder.class);
            load();
            this.allowedBuildTypes = new ArrayList<String>();            
            this.allowedBuildTypes.add("Debug");
            this.allowedBuildTypes.add("Release");
            this.allowedBuildTypes.add("RelWithDebInfo");
            this.allowedBuildTypes.add("MinSizeRel");
            this.errorMessage = "Must be one of Debug, Release, RelWithDebInfo, MinSizeRel";
        }
        
        public void doCheckSourceDir(StaplerRequest req, StaplerResponse rsp, @QueryParameter final String value) throws IOException, ServletException {
        	new FormFieldValidator.WorkspaceFilePath(req, rsp, true, false).process();
        }
        
        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This receives the current value of the field.
         */
        public void doCheckBuildDir(StaplerRequest req, StaplerResponse rsp, @QueryParameter final String value) throws IOException, ServletException {
            new FormFieldValidator(req,rsp,null) {
                /**
                 * The real check goes here. In the end, depending on which
                 * method you call, the browser shows text differently.
                 */
                protected void check() throws IOException, ServletException {
                    if(value.length()==0)
                        error("Please set a build directory");
                    else
                    if(value.length() < 1)
                        warning("Isn't the name too short?");
                    else {
                    	File file = new File(value);
                    	if (file.isFile()) {
                    		error("build dir is a file");
                    	} else { 
                    		//TODO add more checks
                    		ok();
                    	}
                    }
                }
            }.process();
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This receives the current value of the field.
         */
        public void doCheckBuildType(StaplerRequest req, StaplerResponse rsp, @QueryParameter final String value) throws IOException, ServletException {
            new FormFieldValidator(req,rsp,null) {
                /**
                 * The real check goes here. In the end, depending on which
                 * method you call, the browser shows text differently.
                 */
                protected void check() throws IOException, ServletException {
                    for (String allowed : DescriptorImpl.this.allowedBuildTypes) {
                    	if (value.equals(allowed)) {
                    		ok();
                    		return;
                    	}
                    }
                    if (value.length() > 0) {
                    	error(DescriptorImpl.this.errorMessage);
                    }
                }
            }.process();
        }
        
        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "CMake Build";
        }

        public boolean configure(StaplerRequest req, JSONObject o) throws FormException {
            // to persist global configuration information,
            // set that to properties and call save().
            cmakePath = o.getString("cmakePath");
            save();
            return super.configure(req, o);
        }

        public String cmakePath() {
        	return cmakePath;
        }
    }
}

