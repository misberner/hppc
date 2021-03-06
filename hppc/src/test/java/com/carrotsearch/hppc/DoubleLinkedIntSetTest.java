package com.carrotsearch.hppc;

import static com.carrotsearch.hppc.TestUtils.*;

import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import com.carrotsearch.hppc.cursors.IntCursor;
import com.carrotsearch.hppc.predicates.IntPredicate;
import com.carrotsearch.randomizedtesting.RandomizedTest;

/**
 * Unit tests for {@link DoubleLinkedIntSet}.
 */
public class DoubleLinkedIntSetTest<KType> extends RandomizedTest
{
    /**
     * Per-test fresh initialized instance.
     */
    public DoubleLinkedIntSet set;

    int key1 = 1;
    int key2 = 2;
    int defaultValue = 0;

    /**
     * Require assertions for all tests.
     */
    @Rule
    public MethodRule requireAssertions = new RequireAssertionsRule();

    /* */
    @Before
    public void initialize()
    {
        set = new DoubleLinkedIntSet();
    }

    /* */
    @Test
    public void testInitiallyEmpty()
    {
        assertEquals(0, set.size());
    }

    /* */
    @Test
    public void testAdd()
    {
        assertTrue(set.add(key1));
        assertFalse(set.add(key1));
        assertEquals(1, set.size());

        assertTrue(set.contains(key1));
        assertFalse(set.contains(key2));
    }

    /* */
    @Test
    public void testAdd2()
    {
        set.add(key1, key1);
        assertEquals(1, set.size());
        assertEquals(1, set.add(key1, key2));
        assertEquals(2, set.size());
    }

    /* */
    @Test
    public void testAddVarArgs()
    {
        set.add(0, 1, 2, 1, 0);
        assertEquals(3, set.size());
        assertSortedListEquals(set.toArray(), 0, 1, 2);
    }

    /* */
    @Test
    public void testAddAll()
    {
        DoubleLinkedIntSet set2 = new DoubleLinkedIntSet();
        set2.add(1, 2);
        set.add(0, 1);

        assertEquals(1, set.addAll(set2));
        assertEquals(0, set.addAll(set2));

        assertEquals(3, set.size());
        assertSortedListEquals(set.toArray(), 0, 1, 2);
    }

    /* */
    @Test
    public void testRemove()
    {
        set.add(0, 1, 2, 3, 4);

        assertTrue(set.remove(2));
        assertFalse(set.remove(2));
        assertEquals(4, set.size());
        assertSortedListEquals(set.toArray(), 0, 1, 3, 4);
    }

    /* */
    @Test
    public void testEnsureCapacity()
    {
        TightRandomResizingStrategy resizer = new TightRandomResizingStrategy(0);
        DoubleLinkedIntSet set = new DoubleLinkedIntSet(0, 0, resizer);

        // Add some elements.
        final int max = rarely() ? 0 : randomIntBetween(0, 1000);
        for (int i = 0; i < max; i++) {
          set.add(i);
        }

        final int additions = randomIntBetween(max, max + 5000);
        set.ensureCapacity(additions + set.size(), additions - 1);
        final int before = resizer.growCalls;
        for (int i = 0; i < additions; i++) {
          set.add(i);
        }
        assertEquals(before, resizer.growCalls);

        // Should be outside of sparse capacity.
        set.add(additions);
        assertEquals(set.sparse.length, additions + 1);
    }

    /* */
    @Test
    public void testInitialCapacityAndGrowth()
    {
        for (int i = 0; i < 256; i++)
        {
            DoubleLinkedIntSet set = new DoubleLinkedIntSet(i, i);

            for (int j = 0; j < i; j++)
            {
                set.add(/* intrinsic:ktypecast */ j);
            }

            assertEquals(i, set.size());
        }
    }

    /* */
    @Test
    public void testRemoveAllFromLookupContainer()
    {
        set.add(0, 1, 2, 3, 4);

        IntOpenHashSet list2 = new IntOpenHashSet();
        list2.addAll(1, 3, 5);

        assertEquals(2, set.removeAll(list2));
        assertEquals(3, set.size());
        assertSortedListEquals(set.toArray(), 0, 2, 4);
    }

    /* */
    @Test
    public void testRemoveAllWithPredicate()
    {
        set.add(0, key1, key2);

        assertEquals(1, set.removeAll(new IntPredicate()
        {
            public boolean apply(int v)
            {
                return v == key1;
            };
        }));

        assertSortedListEquals(set.toArray(), 0, key2);
    }

    /* */
    @Test
    public void testRetainAllWithPredicate()
    {
        set.add(0, key1, key2, 3, 4, 5);

        assertEquals(4, set.retainAll(new IntPredicate()
        {
            public boolean apply(int v)
            {
                return v == key1 || v == key2;
            };
        }));

        assertSortedListEquals(set.toArray(), key1, key2);
    }

    /* */
    @Test
    public void testClear()
    {
        set.add(1, 2, 3);
        set.clear();
        assertEquals(0, set.size());
        assertEquals(0, set.toArray().length);
    }

    /* */
    @Test
    public void testIterable()
    {
        set.add(1, 2, 2, 3, 4);
        set.remove(2);
        assertEquals(3, set.size());

        int count = 0;
        for (IntCursor cursor : set)
        {
            count++;
            assertTrue(set.contains(cursor.value));
        }
        assertEquals(count, set.size());

        set.clear();
        assertFalse(set.iterator().hasNext());
    }

    /* */
    @Test
    public void testConstructorFromContainer()
    {
        IntOpenHashSet list2 = new IntOpenHashSet();
        list2.addAll(1, 3, 5);

        set = new DoubleLinkedIntSet(list2);
        assertEquals(3, set.size());
        assertSortedListEquals(list2.toArray(), set.toArray());
    }

    /* */
    @Test
    public void testFromMethod()
    {
        IntOpenHashSet list2 = new IntOpenHashSet();
        list2.addAll(1, 3, 5);

        DoubleLinkedIntSet s1 = DoubleLinkedIntSet.from(1, 3, 5);
        DoubleLinkedIntSet s2 = DoubleLinkedIntSet.from(1, 3, 5);

        assertSortedListEquals(list2.toArray(), s1.toArray());
        assertSortedListEquals(list2.toArray(), s2.toArray());
    }

    /* */
    @Test
    public void testToString()
    {
        assertEquals("[1, 3, 5]", DoubleLinkedIntSet.from(1, 3, 5).toString());
    }
    
    /* */
    @Test
    public void testClone()
    {
        set.add(1, 2, 3);
        
        DoubleLinkedIntSet cloned = set.clone();
        cloned.remove(1);

        assertSortedListEquals(set.toArray(), 1, 2, 3);
        assertSortedListEquals(cloned.toArray(), 2, 3);
    }
    
    /**
     * Run some random insertions/ deletions and compare the results
     * against <code>java.util.HashSet</code>.
     */
    @Test
    public void testAgainstHashMap()
    {
        final java.util.Random rnd = new java.util.Random(randomLong());
        final java.util.HashSet<Integer> other = new java.util.HashSet<Integer>();

        for (int size = 1000; size < 20000; size += 4000)
        {
            other.clear();
            set.clear();

            for (int round = 0; round < size * 20; round++)
            {
                Integer key = rnd.nextInt(size);

                if (rnd.nextBoolean())
                {
                    assertEquals(other.add(key), set.add(key));
                    assertTrue(set.contains(key));
                }
                else
                {
                    assertEquals(other.remove(key), set.remove(key));
                }

                assertEquals(other.size(), set.size());
            }
            
            int [] actual = set.toArray();
            int [] expected = new int [other.size()];
            int i = 0;
            for (Integer v : other)
                expected[i++] = v;
            Arrays.sort(expected);
            Arrays.sort(actual);
            assertArrayEquals(expected, actual);
        }
    }

    /* */
    @Test
    public void testSameKeys()
    {
        DoubleLinkedIntSet l1 = DoubleLinkedIntSet.from(1, 2, 3);
        IntOpenHashSet l2 = IntOpenHashSet.from(2, 3, 1);

        Assertions.assertThat(l1.sameKeys(l2));
    }

    /* */
    @Test
    public void testEqualsSameClass()
    {
        DoubleLinkedIntSet l1 = DoubleLinkedIntSet.from(1, 2, 3);
        DoubleLinkedIntSet l2 = DoubleLinkedIntSet.from(1, 2, 3);
        DoubleLinkedIntSet l3 = DoubleLinkedIntSet.from(1, 2, 4);

        Assertions.assertThat(l1).isEqualTo(l2);
        Assertions.assertThat(l1.hashCode()).isEqualTo(l2.hashCode());
        Assertions.assertThat(l1).isNotEqualTo(l3);
    }

    /* */
    @Test
    public void testEqualsSubClass()
    {
        class Sub extends DoubleLinkedIntSet {
        };

        DoubleLinkedIntSet l1 = DoubleLinkedIntSet.from(1, 2, 3);
        DoubleLinkedIntSet l2 = new Sub();
        DoubleLinkedIntSet l3 = new Sub();
        l2.addAll(l1);
        l3.addAll(l1);

        Assertions.assertThat(l2).isEqualTo(l3);
        Assertions.assertThat(l1).isNotEqualTo(l2);
    }
}
