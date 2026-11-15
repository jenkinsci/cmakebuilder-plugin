package hudson.plugins.cmake;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import hudson.FilePath;
import hudson.util.ArgumentListBuilder;

/**
 * Tests the CmakeBuilder's getters, setters, and command line generation.
 *
 * @author Martin Weber
 */
public class CmakeBuilderTest {

    @Test
    public void testSetGenerator() throws Exception {
        CmakeBuilder cmb = new CmakeBuilder(CmakeTool.DEFAULT);
        final String setGenerator = "null: gipsnich";
        cmb.setGenerator(setGenerator);
        assertEquals(setGenerator, cmb.getGenerator());
        cmb.setGenerator(null);
        assertNull(cmb.getGenerator());
    }

    @Test
    public void testSetPreset() throws Exception {
        CmakeBuilder cmb = new CmakeBuilder(CmakeTool.DEFAULT);
        assertNull(cmb.getPreset());
        cmb.setPreset("  my-release  ");
        assertEquals("my-release", cmb.getPreset());
        cmb.setPreset("   ");
        assertNull(cmb.getPreset());

        CmakeBuilderStep step = new CmakeBuilderStep(CmakeTool.DEFAULT);
        assertNull(step.getPreset());
        step.setPreset("my-release");
        assertEquals("my-release", step.getPreset());

        BuildToolStep toolStep = new BuildToolStep();
        assertNull(toolStep.getPreset());
        toolStep.setPreset("my-release");
        assertEquals("my-release", toolStep.getPreset());
    }

    @Test
    public void testIsMultiConfigGenerator() {
        assertTrue(LaunchUtils.isMultiConfigGenerator("Visual Studio 17 2022"));
        assertTrue(LaunchUtils.isMultiConfigGenerator("visual studio 16 2019"));
        assertTrue(LaunchUtils.isMultiConfigGenerator("Xcode"));
        assertTrue(LaunchUtils.isMultiConfigGenerator("Ninja Multi-Config"));
        assertTrue(LaunchUtils.isMultiConfigGenerator("Green Hills MULTI"));
        assertFalse(LaunchUtils.isMultiConfigGenerator("Unix Makefiles"));
        assertFalse(LaunchUtils.isMultiConfigGenerator("Ninja"));
        assertFalse(LaunchUtils.isMultiConfigGenerator(null));
    }

    @Test
    public void testBuildCMakeCallSingleVsMultiConfig() {
        FilePath sourceDir = new FilePath(new File("/workspace/src"));
        // Single-config generator includes -D CMAKE_BUILD_TYPE=Release
        ArgumentListBuilder singleCall = LaunchUtils.buildCMakeCall("cmake",
                "Ninja", null, null, sourceDir, "Release", "-DFOO=BAR");
        assertArrayEquals(
                new String[] { "cmake", "-G", "Ninja", "-D",
                        "CMAKE_BUILD_TYPE=Release", "-DFOO=BAR",
                        sourceDir.getRemote() },
                singleCall.toCommandArray());

        // Multi-config generator (Visual Studio) omits CMAKE_BUILD_TYPE at configure time (JENKINS-58351)
        ArgumentListBuilder multiCall = LaunchUtils.buildCMakeCall("cmake",
                "Visual Studio 17 2022", null, null, sourceDir, "Release",
                "-DFOO=BAR");
        assertArrayEquals(
                new String[] { "cmake", "-G", "Visual Studio 17 2022",
                        "-DFOO=BAR", sourceDir.getRemote() },
                multiCall.toCommandArray());
    }

    @Test
    public void testBuildCMakeCallWithPreset() {
        // Preset without explicit sourceDir (JENKINS-70500)
        ArgumentListBuilder presetCall = LaunchUtils.buildCMakeCall("cmake",
                null, null, "my-release", null, null, null);
        assertArrayEquals(new String[] { "cmake", "--preset", "my-release" },
                presetCall.toCommandArray());
    }

    @Test
    public void testBuildToolCallWithCmakeConfigAndPreset() {
        FilePath buildDir = new FilePath(new File("/workspace/build"));

        // Standard cmake --build with buildType passes --config (JENKINS-58351)
        ArgumentListBuilder configCall = LaunchUtils
                .buildBuildToolCallWithCmake("cmake", buildDir, "Release", null,
                        "--target", "install");
        assertArrayEquals(
                new String[] { "cmake", "--build", buildDir.getRemote(),
                        "--config", "Release", "--target", "install" },
                configCall.toCommandArray());

        // Explicit --config in toolArgs is not duplicated
        ArgumentListBuilder explicitConfigCall = LaunchUtils
                .buildBuildToolCallWithCmake("cmake", buildDir, "Release", null,
                        "--config", "Debug", "--target", "all");
        assertArrayEquals(
                new String[] { "cmake", "--build", buildDir.getRemote(),
                        "--config", "Debug", "--target", "all" },
                explicitConfigCall.toCommandArray());

        // Preset on BuildToolStep omits buildDir and passes --preset (JENKINS-70500)
        ArgumentListBuilder presetCall = LaunchUtils
                .buildBuildToolCallWithCmake("cmake", buildDir, null,
                        "my-release", "-j", "4", "--target", "install");
        assertArrayEquals(
                new String[] { "cmake", "--build", "--preset", "my-release",
                        "-j", "4", "--target", "install" },
                presetCall.toCommandArray());

        // --preset inside toolArgs also omits buildDir (JENKINS-70500)
        ArgumentListBuilder inlinePresetCall = LaunchUtils
                .buildBuildToolCallWithCmake("cmake", buildDir, null, null,
                        "--preset", "my-release", "-j", "4");
        assertArrayEquals(
                new String[] { "cmake", "--build", "--preset", "my-release",
                        "-j", "4" },
                inlinePresetCall.toCommandArray());
    }
}
