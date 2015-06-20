package hudson.plugins.cmake;

import org.junit.Test;

public class FieldValidationTest {

    @Test
    public void checkSourceDirGetter() throws Exception {
        CmakeBuilder c = new CmakeBuilder("trunk/CMakeModules/3rdparty",
                "Buildarea/cmake/3rdparty/Debug", "", "Debug", false, false,
                "Unix Makefiles", "make", "", "", "", "");

        assert (c.getSourceDir() == "trunk/CMakeModules/3rdparty");

    }

    @Test
    public void checkBuildDirGetter() throws Exception {
        CmakeBuilder c = new CmakeBuilder("trunk/CMakeModules/3rdparty",
                "Buildarea/cmake/3rdparty/Debug", "", "Debug", false, false,
                "Unix Makefiles", "make", "", "", "", "");

        assert (c.getBuildDir() == "Buildarea/cmake/3rdparty/Debug");

    }

    @Test
    public void checkInstallDirGetter() throws Exception {
        CmakeBuilder c = new CmakeBuilder("trunk/CMakeModules/3rdparty",
                "Buildarea/cmake/3rdparty/Debug", "", "Debug", false, false,
                "Unix Makefiles", "make", "", "", "", "");

        assert (c.getInstallDir() == "");

    }

}
