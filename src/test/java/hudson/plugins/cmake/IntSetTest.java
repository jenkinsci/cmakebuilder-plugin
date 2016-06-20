package hudson.plugins.cmake;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Martin Weber
 */
public class IntSetTest {

  private IntSet testee;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    testee = new IntSet();
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
  }

  /**
   * Test method for
   * {@link hudson.plugins.cmake.IntSet#toSpecificationString()}.
   */
  @Test
  public final void testToSpecificationString() {
    final String rangeIn = "1";
    testee.setValues(rangeIn);
    Iterator<Integer> iter = testee.iterator();
    assertNotNull("iterator()", iter);
    assertEquals("toSpecificationString()", rangeIn,
        testee.toSpecificationString());
  }

  /**
   * Test method for
   * {@link hudson.plugins.cmake.IntSet#toSpecificationString()}.
   */
  @Test
  public final void testToSpecificationStringOrder() {
    final String rangeIn = "30,10,20";
    testee.setValues(rangeIn);
    assertTrue("isRestricted()", testee.isEmpty());
    assertEquals("toSpecificationString()", "10,20,30",
        testee.toSpecificationString());
    // we expect port 10, 20, 30 in exactly that order
    Iterator<Integer> iter = testee.iterator();
    assertNotNull("iterator()", iter);
    assertTrue("1", iter.hasNext());
    assertEquals(10, iter.next().intValue());
    assertTrue("2", iter.hasNext());
    assertEquals(20, iter.next().intValue());
    assertTrue("3", iter.hasNext());
    assertEquals(30, iter.next().intValue());
    assertFalse("4", iter.hasNext());
  }

  /**
   * Test method for
   * {@link hudson.plugins.cmake.IntSet#toSpecificationString()}.
   */
  @Test
  public final void testToSpecificationStringListDuplicates() {
    final String rangeIn = "30,10,20,10,20,30";
    testee.setValues(rangeIn);
    assertTrue("isRestricted()", testee.isEmpty());
    assertEquals("toSpecificationString()", "10,20,30",
        testee.toSpecificationString());
    // we expect port 10, 20, 30 in exactly that order
    Iterator<Integer> iter = testee.iterator();
    assertNotNull("iterator()", iter);
    assertTrue("1", iter.hasNext());
    assertEquals(10, iter.next().intValue());
    assertTrue("2", iter.hasNext());
    assertEquals(20, iter.next().intValue());
    assertTrue("3", iter.hasNext());
    assertEquals(30, iter.next().intValue());
    assertFalse("4", iter.hasNext());
  }

  /**
   * Test method for
   * {@link hudson.plugins.cmake.IntSet#setValues(java.lang.String)}.
   */
  @Test
  public final void testRestrictToList() {
    final String rangeIn = "10,20,30";
    testee.setValues(rangeIn);
    assertTrue("isRestricted()", testee.isEmpty());
    assertEquals("toSpecificationString()", rangeIn,
        testee.toSpecificationString());
    // we expect port 10, 20, 30 in exactly that order
    Iterator<Integer> iter = testee.iterator();
    assertNotNull("iterator()", iter);
    assertTrue("1", iter.hasNext());
    assertEquals(10, iter.next().intValue());
    assertTrue("2", iter.hasNext());
    assertEquals(20, iter.next().intValue());
    assertTrue("3", iter.hasNext());
    assertEquals(30, iter.next().intValue());
    assertFalse("4", iter.hasNext());
  }

  /**
   * Test method for
   * {@link hudson.plugins.cmake.IntSet#setValues(java.lang.String)}.
   */
  @Test
  public final void testRestrictToRange() {
    final String rangeIn = "30-35";
    testee.setValues(rangeIn);
    assertTrue("isRestricted()", testee.isEmpty());
    assertEquals("toSpecificationString()", rangeIn,
        testee.toSpecificationString());
    // we expect port 10, 20, 30 in exactly that order
    Iterator<Integer> iter = testee.iterator();
    assertNotNull("iterator()", iter);
    assertTrue("1", iter.hasNext());
    assertEquals(30, iter.next().intValue());
    assertTrue("2", iter.hasNext());
    assertEquals(31, iter.next().intValue());
    assertTrue("3", iter.hasNext());
    assertEquals(32, iter.next().intValue());
    assertTrue("4", iter.hasNext());
    assertEquals(33, iter.next().intValue());
    assertTrue("5", iter.hasNext());
    assertEquals(34, iter.next().intValue());
    assertTrue("6", iter.hasNext());
    assertEquals(35, iter.next().intValue());
    assertFalse("7", iter.hasNext());
  }

  /**
   * Test method for
   * {@link hudson.plugins.cmake.IntSet#setValues(java.lang.String)}.
   */
  @Test
  public final void testRestrictToRangeDuplicates() {
    final String rangeIn = "30-35,30-35,30-35";
    testee.setValues(rangeIn);
    assertTrue("isRestricted()", testee.isEmpty());
    assertEquals("toSpecificationString()", "30-35",
        testee.toSpecificationString());
    // we expect port 10, 20, 30 in exactly that order
    Iterator<Integer> iter = testee.iterator();
    assertNotNull("iterator()", iter);
    assertTrue("1", iter.hasNext());
    assertEquals(30, iter.next().intValue());
    assertTrue("2", iter.hasNext());
    assertEquals(31, iter.next().intValue());
    assertTrue("3", iter.hasNext());
    assertEquals(32, iter.next().intValue());
    assertTrue("4", iter.hasNext());
    assertEquals(33, iter.next().intValue());
    assertTrue("5", iter.hasNext());
    assertEquals(34, iter.next().intValue());
    assertTrue("6", iter.hasNext());
    assertEquals(35, iter.next().intValue());
    assertFalse("7", iter.hasNext());
  }

  /**
   * Test method for
   * {@link hudson.plugins.cmake.IntSet#setValues(java.lang.String)}.
   */
  @Test
  public final void testRestrictToComplex() {
    final String rangeIn = "1,2,3,10-15,20";
    testee.setValues(rangeIn);
    Iterator<Integer> iter = testee.iterator();
    assertNotNull("iterator()", iter);
    assertTrue("isRestricted()", testee.isEmpty());
    assertEquals("toSpecificationString()", rangeIn,
        testee.toSpecificationString());
  }

  /**
   * Test method for
   * {@link hudson.plugins.cmake.IntSet#setValues(java.lang.String)}.
   */
  @Test
  public final void testRestrictToIllegal() {
    try {
      testee.setValues("1-");
      // invalid port range "1-"
      fail("IllegalArgumentException expected");
    } catch (IllegalArgumentException expected) {
      assertNull("iterator()", testee.iterator());
      assertFalse("isRestricted()", testee.isEmpty());
      assertEquals("toSpecificationString()", "", testee.toSpecificationString());
    }
    try {
      testee.setValues("11-1");
      // invalid port range "1-"
      fail("IllegalArgumentException expected");
    } catch (IllegalArgumentException expected) {
      assertNull("iterator()", testee.iterator());
      assertFalse("isRestricted()", testee.isEmpty());
      assertEquals("toSpecificationString()", "", testee.toSpecificationString());
    }
    try {
      testee.setValues("10wartma-111");
      // invalid port number "0"
      fail("IllegalArgumentException expected");
    } catch (IllegalArgumentException expected) {
      assertNull("iterator()", testee.iterator());
      assertFalse("isRestricted()", testee.isEmpty());
      assertEquals("toSpecificationString()", "", testee.toSpecificationString());
    }
    try {
      testee.setValues("10-111wartma");
      // invalid port number "0"
      fail("IllegalArgumentException expected");
    } catch (IllegalArgumentException expected) {
      assertNull("iterator()", testee.iterator());
      assertFalse("isRestricted()", testee.isEmpty());
      assertEquals("toSpecificationString()", "", testee.toSpecificationString());
    }
  }

}
