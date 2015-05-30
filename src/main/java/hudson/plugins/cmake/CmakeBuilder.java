package hudson.plugins.cmake;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.matrix.MatrixProject;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;
import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Executes <tt>cmake</tt> as the build process.
 *
 *
 * @author Volker Kaiser
 */
public class CmakeBuilder extends Builder {

	private static final String CMAKE_EXECUTABLE = "CMAKE_EXECUTABLE";

	private static final String CMAKE = "cmake";

	private String sourceDir;
    private String buildDir;
    private String installDir;
    private String buildType;
    private String generator;
    private String makeCommand;
    private String installCommand;
    private String preloadScript;
    private String cmakeArgs;
    private String projectCmakePath;
    private boolean cleanBuild;
    private boolean cleanInstallDir;

    private CmakeBuilderImpl builderImpl;

    @DataBoundConstructor
    public CmakeBuilder(String sourceDir,
    		String buildDir,
    		String installDir,
    		String buildType,
    		boolean cleanBuild,
    		boolean cleanInstallDir,
    		String generator,
    		String makeCommand,
    		String installCommand,
    		String preloadScript,
    		String cmakeArgs,
    		String projectCmakePath) {
    	this.sourceDir = sourceDir;
		this.buildDir = buildDir;
		this.installDir = installDir;
		this.buildType = buildType;
		this.cleanBuild = cleanBuild;
		this.cleanInstallDir = cleanInstallDir;
		this.generator = generator;
		this.makeCommand = makeCommand;
		this.installCommand = installCommand;
		this.cmakeArgs = cmakeArgs;
		this.projectCmakePath = projectCmakePath;
		this.preloadScript = preloadScript;
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

    public boolean getCleanBuild() {
    	return this.cleanBuild;
    }

    public boolean getCleanInstallDir() {
    	return this.cleanInstallDir;
    }

    public String getGenerator() {
    	return this.generator;
    }

    public String getMakeCommand() {
    	return this.makeCommand;
    }

    public String getInstallCommand() {
    	return this.installCommand;
    }

    public String getPreloadScript() {
    	return this.preloadScript;
    }

    public String getCmakeArgs() {
    	return this.cmakeArgs;
    }

    public String getProjectCmakePath() {
    	return this.projectCmakePath;
    }

    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
    	listener.getLogger().println("MODULE: " + build.getModuleRoot());

        final EnvVars envs = build.getEnvironment(listener);
//        final Set<String> keys = envs.keySet();
//    	for (String key : keys) {
//    		listener.getLogger().println("Key : " + key);
//    		cmakeCall   = cmakeCall.replaceAll("\\$" + key, envs.get(key));
//    	}

        final FilePath workSpace = build.getWorkspace();

        String theSourceDir;
    	String theInstallDir;
    	String theBuildDir = this.buildDir;
    	try {
    		theBuildDir = prepareBuildDir(listener, envs, workSpace);
    		theSourceDir = prepareSourceDir(envs, workSpace);
    		theInstallDir = prepareInstallDir(listener, envs, workSpace);
    	} catch (IOException ioe) {
    		listener.getLogger().println(ioe.getMessage());
    		return false;
    	}
        String theBuildType = prepareBuildType();

    	listener.getLogger().println("Build   dir  : " + theBuildDir.toString());
    	listener.getLogger().println("Source  dir  : " + theSourceDir.toString());
    	listener.getLogger().println("Install dir  : " + theInstallDir.toString());
    	String cmakeCall = prepareCmakeCall(build, listener, envs,
				theSourceDir, theInstallDir, theBuildType);
    	listener.getLogger().println("CMake call : " + cmakeCall);

    	final CmakeLauncher cmakeLauncher =
    		new CmakeLauncher(launcher, envs, workSpace, listener, theBuildDir);

    	try {
    		if (!cmakeLauncher.launchCmake(cmakeCall)) {
    			return false;
    		}

    		if (!cmakeLauncher.launchMake(getMakeCommand())) {
    			return false;
    		}

    		return cmakeLauncher.launchInstall(installDir, getInstallCommand());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
		return false;
    }

	private String prepareCmakeCall(AbstractBuild<?, ?> build,
			BuildListener listener, EnvVars envs, String theSourceDir,
			String theInstallDir, String theBuildType) throws IOException,
			InterruptedException {
		String cmakeBin = checkCmake(build.getBuiltOn(), listener, envs);
    	String cmakeCall =
    		builderImpl.buildCMakeCall(cmakeBin,
    				this.generator,
    				this.preloadScript,
    				theSourceDir,
    				theInstallDir,
    				theBuildType, EnvVarReplacer.replace(cmakeArgs, envs));
    	return EnvVarReplacer.replace(cmakeCall, envs);
	}

	private String prepareBuildType() {
		return this.buildType;
	}

	private String prepareInstallDir(BuildListener listener, EnvVars envs,
			final FilePath workSpace) throws IOException {
		if (this.cleanInstallDir) {
			listener.getLogger().println("Wiping out install Dir... " + this.installDir);
			return getCmakeBuilderImpl().preparePath(workSpace, envs, this.installDir,
					CmakeBuilderImpl.PreparePathOptions.CREATE_NEW_IF_EXISTS);
		}
		return getCmakeBuilderImpl().preparePath(workSpace, envs, this.installDir,
				CmakeBuilderImpl.PreparePathOptions.CREATE_IF_NOT_EXISTING);
	}

	private String prepareSourceDir(EnvVars envs, final FilePath workSpace)
			throws IOException {
		return getCmakeBuilderImpl().preparePath(workSpace, envs, this.sourceDir,
				CmakeBuilderImpl.PreparePathOptions.CHECK_PATH_EXISTS);
	}

	private String prepareBuildDir(BuildListener listener, EnvVars envs,
			final FilePath workSpace) throws IOException {
		if (this.cleanBuild) {
			listener.getLogger().println("Cleaning build Dir... " + this.buildDir);
			return getCmakeBuilderImpl().preparePath(workSpace, envs, this.buildDir,
					CmakeBuilderImpl.PreparePathOptions.CREATE_NEW_IF_EXISTS);
		}
		return getCmakeBuilderImpl().preparePath(workSpace, envs, this.buildDir,
				CmakeBuilderImpl.PreparePathOptions.CREATE_IF_NOT_EXISTING);
	}

	private CmakeBuilderImpl getCmakeBuilderImpl() {
		if (builderImpl == null) {
    		builderImpl = new CmakeBuilderImpl();
    	}
		return builderImpl;
	}

	private String checkCmake(Node node, BuildListener listener, EnvVars envs) throws IOException,
			InterruptedException {
		String cmakeBin = CMAKE;
        String cmakePath = getDescriptor().cmakePath();
        if (cmakePath != null && cmakePath.length() > 0) {
    		cmakeBin = cmakePath;
    	}
        if (this.getProjectCmakePath() != null && this.getProjectCmakePath().length() > 0) {
        	cmakeBin = EnvVarReplacer.replace(this.getProjectCmakePath(), envs);
        }
        if (envs.containsKey(CMAKE_EXECUTABLE)) {
        	cmakeBin = envs.get(CMAKE_EXECUTABLE);
        }
        node.createLauncher(listener).launch().stdout(listener).cmds(cmakeBin ,"-version")
        .pwd(node.getRootPath()).join();
	return cmakeBin;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * Descriptor for {@link CmakeBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>views/hudson/plugins/hello_world/CmakeBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private String cmakePath;
        private transient String errorMessage;

        public DescriptorImpl() {
            super(CmakeBuilder.class);
            load();
            this.errorMessage = "Build type can be empty or a single word containing any alphabetical letter. Generally this will be one of '','Debug', 'Release', 'RelWithDebInfo', 'MinSizeRel'";
        }

        public FormValidation doCheckSourceDir(@AncestorInPath AbstractProject<?, ?> project, @QueryParameter final String value) throws IOException, ServletException {
            FilePath ws = project.getSomeWorkspace();
            if(ws==null) return FormValidation.ok();
            return ws.validateRelativePath(value,true,false);
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         */
        public FormValidation doCheckBuildDir(@QueryParameter final String value) throws IOException, ServletException {
            if(value.length()==0)
                return FormValidation.error("Please set a build directory");
            if(value.length() < 1)
                return FormValidation.warning("Isn't the name too short?");

            File file = new File(value);
            if (file.isFile())
                return FormValidation.error("build dir is a file");

            //TODO add more checks
            return FormValidation.ok();
        }

        /**
         * Performs on-the-fly validation of the form field 'buildType'.
         *
         * @param value
         */
        public FormValidation doCheckBuildType(@QueryParameter final String value) throws IOException, ServletException {
            if (value.matches("^[a-zA-Z]*$"))
            {
              return FormValidation.ok();
            }
            return FormValidation.error(DescriptorImpl.this.errorMessage);

        }

        /**
         * Performs on-the-fly validation of the form field 'makeCommand'.
         *
         * @param value
         */
        public FormValidation doCheckMakeCommand(@QueryParameter final String value) throws IOException, ServletException {
            if (value.length() == 0) {
            	return FormValidation.error("Please set make command");
            }
            return FormValidation.validateExecutable(value);
        }


        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "CMake Build";
        }

        @Override
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

        @Override
        public Builder newInstance(StaplerRequest req, JSONObject formData) throws FormException {
        	return req.bindJSON(CmakeBuilder.class, formData);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
        	return FreeStyleProject.class.isAssignableFrom(jobType)
                        || MatrixProject.class.isAssignableFrom(jobType);
        }
    }
}

