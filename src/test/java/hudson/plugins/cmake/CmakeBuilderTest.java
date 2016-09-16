package hudson.plugins.cmake;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Tests the CmakeBuilder`s getters and setters.
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
}
