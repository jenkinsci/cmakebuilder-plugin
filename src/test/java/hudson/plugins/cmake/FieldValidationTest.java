package hudson.plugins.cmake;

import static org.junit.Assert.*;

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

        assertNull(c.getInstallDir());

        CmakeBuilder c2 = new CmakeBuilder("trunk/CMakeModules/3rdparty",
                "Buildarea/cmake/3rdparty/Debug", ".INSTALL", "Debug", false,
                false, "Unix Makefiles", "make", "", "", "", "");

        assertEquals(".INSTALL", c2.getInstallDir());
    }

}
