package com.google.common.collect;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Minimal host shape of the APK's List-implementing Guava immutable collection. */
public final class ImmutableList<E> extends AbstractList<E> {
    private final List<E> values;
    private ImmutableList(Collection<? extends E> input) {
        values = Collections.unmodifiableList(new ArrayList<E>(input));
    }
    public static <E> ImmutableList<E> copyOf(Collection<? extends E> input) {
        return new ImmutableList<E>(input);
    }
    public E get(int index) { return values.get(index); }
    public int size() { return values.size(); }
}
