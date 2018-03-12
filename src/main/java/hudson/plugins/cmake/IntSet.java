package hudson.plugins.cmake;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.iterators.IteratorChain;

/**
 * A set of int values that can be expressed as a String.
 *
 * @author Martin Weber
 */
public class IntSet {
    /** values in this Set or {@code null} if empty */
    private Set<IntRange> ranges;

    /**
     * Constructs a new object that does not contain any number.
     */
    public IntSet() {
    }

    /**
     * Constructs a new object that contains the values from the given range
     * specification string.
     *
     * @param rangeSpecification
     *            the range, expressed as a String. Allowed numbers can be
     *            specified as
     *            <ul>
     *            <li>a comma separated list, e.g. {@code 20,21,50},</li>
     *            <li>a range, e.g. {@code 50-70},</li>
     *            <li>a combination of the above,</li>
     *            <li>{@code null} for an empty range</li>
     *            <li>the empty string for empty range.</li>
     *            </ul>
     * @throws IllegalArgumentException
     *             if the given range specification string is invalid
     * @see #toSpecificationString()
     */
    public IntSet(String rangeSpecification) {
        setValues(rangeSpecification);
    }

    /**
     * Parses the given range specification string and sets the values in the
     * set accordingly.
     *
     * @param rangeSpecification
     *            the range, expressed as a String. Allowed numbers can be
     *            specified as
     *            <ul>
     *            <li>a comma separated list, e.g. {@code 20,21,50},</li>
     *            <li>a range, e.g. {@code 50-70},</li>
     *            <li>a combination of the above,</li>
     *            <li>{@code null} for an empty range</li>
     *            <li>the empty string for empty range.</li>
     *            </ul>
     * @throws IllegalArgumentException
     *             if the given range specification string is invalid
     * @see #toSpecificationString()
     */
    public void setValues(String rangeSpecification) {
        SortedSet<IntRange> ranges0 = new TreeSet<IntRange>();
        // e.g. "20,21,50-70,99"
        if (rangeSpecification == null
                || (rangeSpecification = rangeSpecification.trim())
                        .length() == 0) {
            // empty..
            ranges = null;
        } else {
            final Matcher matcher = Pattern
                    .compile("(?:\\s*)(\\d+)(?:(?:-)(\\d+))?(?:\\s*)")
                    .matcher("");
            // split at ','
            for (String expr : rangeSpecification.split(",")) {
                // no need to check for negative values here, our regex already
                // rejects these
                matcher.reset(expr);
                if (matcher.matches()) {
                    // int groupCount = matcher.groupCount();
                    // for (int i = 0; i < groupCount; i++)
                    // System.err.println(i + ":" + matcher.group(i + 1));
                    final String hi = matcher.group(2);
                    int lowest = Integer.parseInt(matcher.group(1));
                    checkUpperLimit(lowest);
                    if (hi == null) {
                        // not a range
                        ranges0.add(new IntRange(lowest));
                    } else {
                        int highest = Integer.parseInt(matcher.group(2));
                        checkUpperLimit(highest);
                        if (lowest > highest) {
                            throw new IllegalArgumentException(
                                    "Invalid range specification: " + lowest
                                            + " > " + highest);
                        }
                        ranges0.add(new IntRange(lowest, highest));
                    }
                } else {
                    throw new IllegalArgumentException(
                            "Invalid range specification: '" + expr + "'");
                }
            }

            if (ranges0.isEmpty()) {
                // empty..
                ranges = null;
            } else {
                ranges = ranges0;
            }
        }
    }

    /**
     * Gets whether this IntSet is empty.
     *
     * @see #setValues(String)
     * @see #iterator()
     * @return {@code true} if this IntSet is empty, otherwise
     *         {@code false}
     */
    public boolean isEmpty() {
        return ranges == null;
    }

    /**
     * Gets whether the specified value is contained in this set of values.
     */
    public boolean contains(int value) {
        if (ranges == null) {
            return false;
        }
        for (Iterator<Integer> iter = iterator(); iter.hasNext();) {
            if (value == iter.next()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets this object as a range specification string which can safely be
     * parsed by {@link #setValues(String)}.
     *
     * @return a range specification string, never {@code null}. The empty
     *         string is returned when this set is empty.
     * @see #setValues(String)
     */
    public String toSpecificationString() {
        if (ranges == null) {
            return ""; // no values
        }
        final StringBuilder sb = new StringBuilder();
        for (Iterator<IntRange> iter = ranges.iterator(); iter.hasNext();) {
            final IntRange range = iter.next();
            sb.append(range.toSpecificationString());
            if (iter.hasNext()) {
                sb.append(',');
            }
        }
        return sb.toString();
    }

    /*
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "IntSet [" + this.toSpecificationString() + "]";
    }

    /**
     * Gets an iterator over all values.
     *
     * @see #setValues(String)
     * @see #isEmpty()
     * @return the iterator
     */
    @SuppressWarnings("unchecked")
    public Iterator<Integer> iterator() {
        if (ranges == null) {
            return Collections.emptyIterator(); // no values
        }

        IteratorChain chain = new IteratorChain();
        for (IntRange range : ranges) {
            chain.addIterator(range.iterator());
        }
        return chain;
    }

    /**
     * Check whether the specified number is less than or equal to 65535.
     *
     * @throws IllegalArgumentException
     *             if number > 65535
     */
    private static void checkUpperLimit(int number)
            throws IllegalArgumentException {
        if (number > 0xFFFF) {
            throw new IllegalArgumentException(
                    "Invalid number: " + number + " > " + 0xFFFF);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // inner classes
    ////////////////////////////////////////////////////////////////////
    /**
     * Represents a single range of int values.
     *
     * @author Martin Weber
     */
    private static class IntRange
            implements Comparable<IntRange>, Iterable<Integer> {
        /** lowest number (including) */
        private final int lowest;
        /** highest number (including) */
        private final int highest;

        /**
         * Constructs an object that represents a consecutive range of integer
         * numbers. Does not validate any parameters!
         *
         * @param lowest
         *            the lowest number (including)
         * @param highest
         *            the highest number (including)
         */
        public IntRange(int lowest, int highest) {
            if (lowest > highest)
                throw new IllegalArgumentException("lowest > highest");
            this.lowest = lowest;
            this.highest = highest;
        }

        /**
         * Constructs an object that represents a single numbers. Does not
         * validate any parameters!
         *
         * @param number
         *            the allowed number
         */
        public IntRange(int number) {
            this(number, number);
        }

        /**
         * Gets this object as a integer number range specification string which
         * can safely be parsed by {@link #setValues(String)};
         *
         * @return a range specification string, never {@code null}
         */
        public String toSpecificationString() {
            if (lowest == highest) {
                return Integer.toString(lowest);
            }
            return lowest + "-" + highest;
        }

        /*
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "IntRange [" + this.toSpecificationString() + "]";
        }

        /*
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = prime + this.highest;
            result = prime * result + this.lowest;
            return result;
        }

        /*
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            IntRange other = (IntRange) obj;
            if (this.highest != other.highest) {
                return false;
            }
            if (this.lowest != other.lowest) {
                return false;
            }
            return true;
        }

        /*
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        @Override
        public int compareTo(IntRange o) {
            if (this.lowest == o.lowest) {
                return this.highest - o.highest;
            }
            return this.lowest - o.lowest;
        }

        /*
         * @see java.lang.Iterable#iterator()
         */
        @Override
        public Iterator<Integer> iterator() {
            return new RangeIterator(this);
        }
    } // IntRange

    private static class RangeIterator implements Iterator<Integer> {
        private final IntRange range;
        private int next;

        RangeIterator(IntRange range) {
            this.range = range;
            next = range.lowest;
        }

        /*
         * @see java.util.Iterator#hasNext()
         */
        @Override
        public boolean hasNext() {
            return next <= range.highest;
        }

        /*
         * @see java.util.Iterator#next()
         */
        @Override
        public Integer next() {
            if (!(next <= range.highest)) {
                throw new NoSuchElementException();
            }
            return Integer.valueOf(next++);
        }

        /*
         * @see java.util.Iterator#remove()
         */
        @Override
        public void remove() {
            throw new java.lang.UnsupportedOperationException("remove");
        }

    } // RangeIterator
}
