package com.carrotsearch.hppc;

import static com.carrotsearch.hppc.TestUtils.*;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import com.carrotsearch.hppc.cursors.KTypeCursor;
import com.carrotsearch.hppc.mutables.IntHolder;
import com.carrotsearch.hppc.predicates.KTypePredicate;

/**
 * Unit tests for {@link KTypeOpenHashSet}.
 */
/*! ${TemplateOptions.generatedAnnotation} !*/
public class KTypeOpenHashSetTest<KType> extends AbstractKTypeTest<KType>
{
    /**
     * Per-test fresh initialized instance.
     */
    public KTypeOpenHashSet<KType> set;

    public final KType EMPTY_KEY = Intrinsics.<KType> empty();

    /* */
    @Before
    public void initialize()
    {
        set = new KTypeOpenHashSet<>();
    }

    @Test
    public void testIndexMethods()
    {
      set.add(keyE);
      set.add(key1);

      Assertions.assertThat(set.indexOf(keyE)).isNotNegative();
      Assertions.assertThat(set.indexOf(key1)).isNotNegative();
      Assertions.assertThat(set.indexOf(key2)).isNegative();

      Assertions.assertThat(set.indexExists(set.indexOf(keyE))).isTrue();
      Assertions.assertThat(set.indexExists(set.indexOf(key1))).isTrue();
      Assertions.assertThat(set.indexExists(set.indexOf(key2))).isFalse();

      Assertions.assertThat(set.indexGet(set.indexOf(keyE))).isEqualTo(keyE);
      Assertions.assertThat(set.indexGet(set.indexOf(key1))).isEqualTo(key1);
      try {
        set.indexGet(set.indexOf(key2));
        fail();
      } catch (AssertionError e) {
        // Expected.
      }

      Assertions.assertThat(set.indexReplace(set.indexOf(keyE), keyE)).isEqualTo(keyE);
      Assertions.assertThat(set.indexReplace(set.indexOf(key1), key1)).isEqualTo(key1);

      set.indexInsert(set.indexOf(key2), key2);
      Assertions.assertThat(set.indexGet(set.indexOf(key2))).isEqualTo(key2);
      Assertions.assertThat(set.size()).isEqualTo(3);
    }

    /* */
    @Test
    public void testEmptyKey()
    {
        KTypeOpenHashSet<KType> set = new KTypeOpenHashSet<KType>();
        set.add(EMPTY_KEY);

        Assertions.assertThat(set.size()).isEqualTo(1);
        Assertions.assertThat(set.isEmpty()).isFalse();
        Assertions.assertThat(set.toArray()).containsOnly(EMPTY_KEY);
        Assertions.assertThat(set.contains(EMPTY_KEY)).isTrue();

        set.remove(EMPTY_KEY);

        Assertions.assertThat(set.size()).isEqualTo(0);
        Assertions.assertThat(set.isEmpty()).isTrue();
        Assertions.assertThat(set.toArray()).isEmpty();
        Assertions.assertThat(set.contains(EMPTY_KEY)).isFalse();
    }

    /* */
    @Test
    public void testEnsureCapacity()
    {
        final IntHolder expands = new IntHolder();
        KTypeOpenHashSet<KType> set = new KTypeOpenHashSet<KType>(0) {
          @Override
          protected void allocateBuffers(int arraySize) {
            super.allocateBuffers(arraySize);
            expands.value++;
          }
        };

        // Add some elements.
        final int max = rarely() ? 0 : randomIntBetween(0, 250);
        for (int i = 0; i < max; i++) {
          set.add(cast(i));
        }

        final int additions = randomIntBetween(max, max + 5000);
        set.ensureCapacity(additions + set.size());
        final int before = expands.value;
        for (int i = 0; i < additions; i++) {
          set.add(cast(i));
        }
        assertEquals(before, expands.value);
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
    }

    /* */
    @Test
    public void testAdd2()
    {
        set.addAll(key1, key1);
        assertEquals(1, set.size());
        assertEquals(1, set.addAll(key1, key2));
        assertEquals(2, set.size());
    }

    /* */
    @Test
    public void testAddVarArgs()
    {
        set.addAll(asArray(0, 1, 2, 1, 0));
        assertEquals(3, set.size());
        assertSortedListEquals(set.toArray(), 0, 1, 2);
    }

    /* */
    @Test
    public void testAddAll()
    {
        KTypeOpenHashSet<KType> set2 = new KTypeOpenHashSet<KType>();
        set2.addAll(asArray(1, 2));
        set.addAll(asArray(0, 1));

        assertEquals(1, set.addAll(set2));
        assertEquals(0, set.addAll(set2));

        assertEquals(3, set.size());
        assertSortedListEquals(set.toArray(), 0, 1, 2);
    }

    /* */
    @Test
    public void testRemove()
    {
        set.addAll(asArray(0, 1, 2, 3, 4));

        assertTrue(set.remove(k2));
        assertFalse(set.remove(k2));
        assertEquals(4, set.size());
        assertSortedListEquals(set.toArray(), 0, 1, 3, 4);
    }

    /* */
    @Test
    public void testInitialCapacityAndGrowth()
    {
        for (int i = 0; i < 256; i++)
        {
            KTypeOpenHashSet<KType> set = new KTypeOpenHashSet<KType>(i);
            
            for (int j = 0; j < i; j++)
            {
                set.add(cast(j));
            }

            assertEquals(i, set.size());
        }
    }

    /* */
    @Test
    public void testBug_HPPC73_FullCapacityGet()
    {
        final IntHolder reallocations = new IntHolder();
        final int elements = 0x7F;
        set = new KTypeOpenHashSet<KType>(elements, 1f) {
          @Override
          protected double verifyLoadFactor(double loadFactor) {
            // Skip load factor sanity range checking.
            return loadFactor;
          }
          
          @Override
          protected void allocateBuffers(int arraySize) {
            super.allocateBuffers(arraySize);
            reallocations.value++;
          }
        };

        int reallocationsBefore = reallocations.value;
        assertEquals(reallocationsBefore, 1);
        for (int i = 1; i <= elements; i++)
        {
            set.add(cast(i));
        }

        // Non-existent key.
        KType outOfSet = cast(elements + 1);
        set.remove(outOfSet);
        assertFalse(set.contains(outOfSet));
        assertEquals(reallocationsBefore, reallocations.value);

        // Should not expand because we're replacing an existing element.
        assertFalse(set.add(k1));
        assertEquals(reallocationsBefore, reallocations.value);

        // Remove from a full set.
        set.remove(k1);
        assertEquals(reallocationsBefore, reallocations.value);
        set.add(k1);

        // Check expand on "last slot of a full map" condition.
        set.add(outOfSet);
        assertEquals(reallocationsBefore + 1, reallocations.value);
    }

    
    /* */
    @Test
    public void testRemoveAllFromLookupContainer()
    {
        set.addAll(asArray(0, 1, 2, 3, 4));

        KTypeOpenHashSet<KType> list2 = new KTypeOpenHashSet<KType>();
        list2.addAll(asArray(1, 3, 5));

        assertEquals(2, set.removeAll(list2));
        assertEquals(3, set.size());
        assertSortedListEquals(set.toArray(), 0, 2, 4);
    }

    /* */
    @Test
    public void testRemoveAllWithPredicate()
    {
        set.addAll(newArray(k0, k1, k2));

        assertEquals(1, set.removeAll(new KTypePredicate<KType>()
        {
            public boolean apply(KType v)
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
        set.addAll(newArray(k0, k1, k2, k3, k4, k5));

        assertEquals(4, set.retainAll(new KTypePredicate<KType>()
        {
            public boolean apply(KType v)
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
        set.addAll(asArray(1, 2, 3));
        set.clear();
        assertEquals(0, set.size());
    }

    /* */
    @Test
    public void testRelease()
    {
        set.addAll(asArray(1, 2, 3));
        set.release();
        assertEquals(0, set.size());
        set.addAll(asArray(1, 2, 3));
        assertEquals(3, set.size());
    }

    /* */
    @Test
    public void testIterable()
    {
        set.addAll(asArray(1, 2, 2, 3, 4));
        set.remove(k2);
        assertEquals(3, set.size());

        int count = 0;
        for (KTypeCursor<KType> cursor : set)
        {
            count++;
            assertTrue(set.contains(cursor.value));
        }
        assertEquals(count, set.size());

        set.clear();
        assertFalse(set.iterator().hasNext());
    }

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @Test
    public void testNullKey()
    {
        set.add((KType) null);
        assertEquals(1, set.size());
        assertTrue(set.contains(null));
        assertTrue(set.remove(null));
        assertEquals(0, set.size());
        assertFalse(set.contains(null));
    }
    /*! #end !*/
    
    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    /**
     * Run some random insertions/ deletions and compare the results
     * against <code>java.util.HashSet</code>.
     */
    @Test
    public void testAgainstHashMap()
    {
        final java.util.Random rnd = new java.util.Random();
        final java.util.HashSet<KType> other = new java.util.HashSet<KType>();

        for (int size = 1000; size < 20000; size += 4000)
        {
            other.clear();
            set.clear();

            for (int round = 0; round < size * 20; round++)
            {
                Integer key = rnd.nextInt(size);
                if (rnd.nextInt(50) == 0) {
                  key = Intrinsics.empty();
                }

                if (rnd.nextBoolean())
                {
                    other.add(cast(key));
                    set.add(cast(key));

                    assertTrue(set.contains(cast(key)));
                }
                else
                {
                    assertEquals(other.remove(key), set.remove(cast(key)));
                }

                assertEquals(other.size(), set.size());
            }
        }
    }
    /*! #end !*/

    /* */
    @Test
    public void testHashCodeEquals()
    {
        KTypeOpenHashSet<KType> l0 = new KTypeOpenHashSet<>();
        assertEquals(0, l0.hashCode());
        assertEquals(l0, new KTypeOpenHashSet<>());

        KTypeOpenHashSet<KType> l1 = KTypeOpenHashSet.from(k1, k2, k3);
        KTypeOpenHashSet<KType> l2 = KTypeOpenHashSet.from(k1, k2);
        l2.add(k3);

        assertEquals(l1.hashCode(), l2.hashCode());
        assertEquals(l1, l2);
    }

    /* */
    @Test
    public void testHashCodeEqualsForDifferentMix()
    {
        KTypeOpenHashSet<KType> l0 = new KTypeOpenHashSet<KType>(0, 0.5d, HashOrderMixing.constant(1));
        KTypeOpenHashSet<KType> l1 = new KTypeOpenHashSet<KType>(0, 0.5d, HashOrderMixing.constant(2));

        assertEquals(0, l0.hashCode());
        assertEquals(l0.hashCode(), l1.hashCode());
        assertEquals(l0, l1);

        l0.addAll(newArray(k1, k2, k3));
        l1.addAll(newArray(k1, k2, k3));

        assertEquals(l0.hashCode(), l1.hashCode());
        assertEquals(l0, l1);
    }

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @Test
    public void testHashCodeWithNulls()
    {
        KTypeOpenHashSet<KType> l1 = KTypeOpenHashSet.from(k1, null, k3);
        KTypeOpenHashSet<KType> l2 = KTypeOpenHashSet.from(k1, null);
        l2.add(k3);

        assertEquals(l1.hashCode(), l2.hashCode());
        assertEquals(l1, l2);
    }
    /*! #end !*/

    @Test
    public void testClone()
    {
        this.set.addAll(key1, key2, key3);

        KTypeOpenHashSet<KType> cloned = set.clone();
        cloned.removeAll(key1);

        assertSortedListEquals(set.toArray(), key1, key2, key3);
        assertSortedListEquals(cloned.toArray(), key2, key3);
    }
    
    /* */
    @Test
    public void testEqualsSameClass()
    {
      KTypeOpenHashSet<KType> l1 = KTypeOpenHashSet.from(k1, k2, k3);
      KTypeOpenHashSet<KType> l2 = KTypeOpenHashSet.from(k1, k2, k3);
      KTypeOpenHashSet<KType> l3 = KTypeOpenHashSet.from(k1, k2, k4);

        Assertions.assertThat(l1).isEqualTo(l2);
        Assertions.assertThat(l1.hashCode()).isEqualTo(l2.hashCode());
        Assertions.assertThat(l1).isNotEqualTo(l3);
    }

    /* */
    @Test
    public void testEqualsSubClass()
    {
        class Sub extends KTypeOpenHashSet<KType> {
        };

        KTypeOpenHashSet<KType> l1 = KTypeOpenHashSet.from(k1, k2, k3);
        KTypeOpenHashSet<KType> l2 = new Sub();
        KTypeOpenHashSet<KType> l3 = new Sub();
        l2.addAll(l1);
        l3.addAll(l1);

        Assertions.assertThat(l2).isEqualTo(l3);
        Assertions.assertThat(l1).isNotEqualTo(l2);
    }    
}
