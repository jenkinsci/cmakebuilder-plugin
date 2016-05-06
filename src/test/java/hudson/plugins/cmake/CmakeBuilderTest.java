package hudson.plugins.cmake;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests the CmakeBuilder`s getters and setters.
 *
 * @author Martin Weber
 */
public class CmakeBuilderTest {

    /**
     * Verify that a default generator is set.
     */
    @Test
    public void testDefaultGenerator() throws Exception {
        CmakeBuilder cmb = new CmakeBuilder(CmakeTool.DEFAULT);
        final String generator = cmb.getGenerator();
        assertNotNull(generator);
        assertTrue(generator.trim().length() > 0);
    }

    /**
     * Verify that a default generator is set even if set to null.
     */
    @Test
    public void testDefaultGeneratorSet() throws Exception {
        CmakeBuilder cmb = new CmakeBuilder(CmakeTool.DEFAULT);
        final String setGenerator = "null: gipsnich";
        cmb.setGenerator(setGenerator);
        assertEquals(setGenerator, cmb.getGenerator());
        cmb.setGenerator(null);
        String generator = cmb.getGenerator();
        assertNotNull(generator);
        assertTrue(generator.trim().length() > 0);
    }
}
