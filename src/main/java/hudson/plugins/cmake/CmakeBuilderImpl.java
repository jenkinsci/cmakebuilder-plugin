package hudson.plugins.cmake;

import hudson.FilePath;
import hudson.Util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

public class CmakeBuilderImpl {

    private static final String DCMAKE_BUILD_TYPE = "-DCMAKE_BUILD_TYPE=";
    private static final String DCMAKE_INSTALL_PREFIX = "-DCMAKE_INSTALL_PREFIX=";
    private static final String BLANK = " ";

    public enum PreparePathOptions {
        CHECK_PATH_EXISTS() {
            @Override
            public void process(FilePath file) throws IOException {
                try {
                    if (!file.exists()) {
                        throw new FileNotFoundException(file.getRemote());
                    }
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        },

        CREATE_IF_NOT_EXISTING() {
            @Override
            public void process(FilePath file) throws IOException {
                try {
                    if (!file.exists()) {
                        file.mkdirs();
                    }
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        },

        CREATE_NEW_IF_EXISTS() {
            @Override
            public void process(FilePath file) throws IOException {
                try {
                    if (file.exists()) {
                        if (file.isDirectory()) {
                            file.deleteRecursive();
                        }
                    }
                } catch (InterruptedException e) {
                    // ignore
                }
                CREATE_IF_NOT_EXISTING.process(file);
            }
        };

        public abstract void process(FilePath file) throws IOException;
    };

    public CmakeBuilderImpl() {
        super();
    }

    String preparePath(FilePath workSpace, final Map<String, String> envVars,
            String path, PreparePathOptions ppOption) throws IOException {
        path = path.trim();
        if (path.isEmpty()) {
            return path;
        }
        path = Util.replaceMacro(path, envVars);
        FilePath file = workSpace.child(path);
        ppOption.process(file);
        return file.getRemote();
    }

    String buildCMakeCall(final String cmakeBin, final String generator,
            final String preloadScript, final String sourceDir,
            final String installDir, final String buildType,
            final String cmakeArgs) {
        StringBuilder builder = new StringBuilder().append(cmakeBin)
                .append(BLANK).append(createPreloadScriptArg(preloadScript))
                .append(BLANK).append("-G \"").append(generator).append("\"")
                .append(BLANK);
        if (!installDir.isEmpty()) {
            builder.append(DCMAKE_INSTALL_PREFIX).append(installDir)
                    .append(BLANK);
        }
        if (!buildType.isEmpty()) {
            builder.append(DCMAKE_BUILD_TYPE).append(buildType).append(BLANK);
        }
        builder.append(cmakeArgs).append(BLANK).append("\"").append(sourceDir)
                .append("\"").append(BLANK);
        return builder.toString();

    }

    private String createPreloadScriptArg(final String preloadScript) {
        if (preloadScript == null || preloadScript.trim().isEmpty()) {
            return "";
        }
        return " -C \"" + preloadScript.trim() + "\"";
    }
}
