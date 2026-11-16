package hudson.plugins.cmake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Martin Weber
 */
class IntSetTest {

  private IntSet testee;

  /**
   * @throws java.lang.Exception
   */
  @BeforeEach
  void setUp() throws Exception {
    testee = new IntSet();
  }

  /**
   * Test method for
   * {@link hudson.plugins.cmake.IntSet#toSpecificationString()}.
   */
  @Test
  final void testToSpecificationString() {
    final String rangeIn = "1";
    testee.setValues(rangeIn);
    Iterator<Integer> iter = testee.iterator();
    assertNotNull(iter, "iterator()");
    assertEquals(rangeIn, testee.toSpecificationString(),
        "toSpecificationString()");
  }

  /**
   * Test method for
   * {@link hudson.plugins.cmake.IntSet#toSpecificationString()}.
   */
  @Test
  final void testToSpecificationStringOrder() {
    final String rangeIn = "30,10,20";
    testee.setValues(rangeIn);
    assertFalse(testee.isEmpty(), "isEmpty()");
    assertEquals("10,20,30", testee.toSpecificationString(),
        "toSpecificationString()");
    // we expect port 10, 20, 30 in exactly that order
    Iterator<Integer> iter = testee.iterator();
    assertNotNull(iter, "iterator()");
    assertTrue(iter.hasNext(), "1");
    assertEquals(10, iter.next().intValue());
    assertTrue(iter.hasNext(), "2");
    assertEquals(20, iter.next().intValue());
    assertTrue(iter.hasNext(), "3");
    assertEquals(30, iter.next().intValue());
    assertFalse(iter.hasNext(), "4");
  }

  /**
   * Test method for
   * {@link hudson.plugins.cmake.IntSet#toSpecificationString()}.
   */
  @Test
  final void testToSpecificationStringListDuplicates() {
    final String rangeIn = "30,10,20,10,20,30";
    testee.setValues(rangeIn);
    assertFalse(testee.isEmpty(), "isEmpty()");
    assertEquals("10,20,30", testee.toSpecificationString(),
        "toSpecificationString()");
    // we expect port 10, 20, 30 in exactly that order
    Iterator<Integer> iter = testee.iterator();
    assertNotNull(iter, "iterator()");
    assertTrue(iter.hasNext(), "1");
    assertEquals(10, iter.next().intValue());
    assertTrue(iter.hasNext(), "2");
    assertEquals(20, iter.next().intValue());
    assertTrue(iter.hasNext(), "3");
    assertEquals(30, iter.next().intValue());
    assertFalse(iter.hasNext(), "4");
  }

  /**
   * Test method for
   * {@link hudson.plugins.cmake.IntSet#setValues(java.lang.String)}.
   */
  @Test
  final void testRestrictToList() {
    final String rangeIn = "10,20,30";
    testee.setValues(rangeIn);
    assertFalse(testee.isEmpty(), "isEmpty()");
    assertEquals(rangeIn, testee.toSpecificationString(),
        "toSpecificationString()");
    // we expect port 10, 20, 30 in exactly that order
    Iterator<Integer> iter = testee.iterator();
    assertNotNull(iter, "iterator()");
    assertTrue(iter.hasNext(), "1");
    assertEquals(10, iter.next().intValue());
    assertTrue(iter.hasNext(), "2");
    assertEquals(20, iter.next().intValue());
    assertTrue(iter.hasNext(), "3");
    assertEquals(30, iter.next().intValue());
    assertFalse(iter.hasNext(), "4");
  }

  /**
   * Test method for
   * {@link hudson.plugins.cmake.IntSet#setValues(java.lang.String)}.
   */
  @Test
  final void testRestrictToRange() {
    final String rangeIn = "30-35";
    testee.setValues(rangeIn);
    assertFalse(testee.isEmpty(), "isEmpty()");
    assertEquals(rangeIn, testee.toSpecificationString(),
        "toSpecificationString()");
    // we expect port 10, 20, 30 in exactly that order
    Iterator<Integer> iter = testee.iterator();
    assertNotNull(iter, "iterator()");
    assertTrue(iter.hasNext(), "1");
    assertEquals(30, iter.next().intValue());
    assertTrue(iter.hasNext(), "2");
    assertEquals(31, iter.next().intValue());
    assertTrue(iter.hasNext(), "3");
    assertEquals(32, iter.next().intValue());
    assertTrue(iter.hasNext(), "4");
    assertEquals(33, iter.next().intValue());
    assertTrue(iter.hasNext(), "5");
    assertEquals(34, iter.next().intValue());
    assertTrue(iter.hasNext(), "6");
    assertEquals(35, iter.next().intValue());
    assertFalse(iter.hasNext(), "7");
  }

  /**
   * Test method for
   * {@link hudson.plugins.cmake.IntSet#setValues(java.lang.String)}.
   */
  @Test
  final void testRestrictToRangeDuplicates() {
    final String rangeIn = "30-35,30-35,30-35";
    testee.setValues(rangeIn);
    assertFalse(testee.isEmpty(), "isEmpty()");
    assertEquals("30-35", testee.toSpecificationString(),
        "toSpecificationString()");
    // we expect port 10, 20, 30 in exactly that order
    Iterator<Integer> iter = testee.iterator();
    assertNotNull(iter, "iterator()");
    assertTrue(iter.hasNext(), "1");
    assertEquals(30, iter.next().intValue());
    assertTrue(iter.hasNext(), "2");
    assertEquals(31, iter.next().intValue());
    assertTrue(iter.hasNext(), "3");
    assertEquals(32, iter.next().intValue());
    assertTrue(iter.hasNext(), "4");
    assertEquals(33, iter.next().intValue());
    assertTrue(iter.hasNext(), "5");
    assertEquals(34, iter.next().intValue());
    assertTrue(iter.hasNext(), "6");
    assertEquals(35, iter.next().intValue());
    assertFalse(iter.hasNext(), "7");
  }

  /**
   * Test method for
   * {@link hudson.plugins.cmake.IntSet#setValues(java.lang.String)}.
   */
  @Test
  final void testRestrictToComplex() {
    final String rangeIn = "1,2,3,10-15,20";
    testee.setValues(rangeIn);
    Iterator<Integer> iter = testee.iterator();
    assertNotNull(iter, "iterator()");
    assertFalse(testee.isEmpty(), "isEmpty()");
    assertEquals(rangeIn, testee.toSpecificationString(),
        "toSpecificationString()");
  }

  /**
   * Test method for
   * {@link hudson.plugins.cmake.IntSet#setValues(java.lang.String)}.
   */
  @Test
  final void testRestrictToIllegal() {
    for (String illegal : new String[] { "1-", "11-1", "10wartma-111", "10-111wartma" }) {
      assertThrows(IllegalArgumentException.class, () -> testee.setValues(illegal));
      assertTrue(testee.isEmpty(), "isEmpty()");
      assertEquals("", testee.toSpecificationString(), "toSpecificationString()");
    }
  }

  /**
   * Test method for
   * {@link hudson.plugins.cmake.IntSet#contains(int)}.
   */
  @Test
  final void testContains() {
      final String rangeIn = "30,10,20";
      testee.setValues(rangeIn);
      assertFalse(testee.isEmpty());
      assertTrue(testee.contains(30));
      assertTrue(testee.contains(10));
      assertTrue(testee.contains(20));
      assertFalse(testee.contains(1));

      testee.setValues("");
      assertTrue(testee.isEmpty());
      assertFalse(testee.contains(30));
      testee.setValues(null);
      assertTrue(testee.isEmpty());
      assertFalse(testee.contains(30));
  }

}
