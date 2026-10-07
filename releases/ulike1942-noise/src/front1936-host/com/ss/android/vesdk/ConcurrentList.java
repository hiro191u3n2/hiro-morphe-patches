package com.ss.android.vesdk;
import java.util.*;
public class ConcurrentList<T> {
    public final List<T> list=new ArrayList<>();
    public boolean isEmpty(){return list.isEmpty();}
    public List<T> getImmutableList(){return new ArrayList<>(list);}
}
