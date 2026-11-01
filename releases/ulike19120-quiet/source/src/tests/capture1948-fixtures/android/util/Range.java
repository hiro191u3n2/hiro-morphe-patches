package android.util;
public final class Range<T extends Comparable<? super T>> {final T lower,upper;public Range(T l,T u){lower=l;upper=u;}public T getLower(){return lower;}public T getUpper(){return upper;}}

