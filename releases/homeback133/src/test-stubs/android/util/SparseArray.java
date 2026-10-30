package android.util;

/** Sorted-key host representation of the Android cache used by profile tabs. */
public class SparseArray<E> {
    private final java.util.SortedMap<Integer,E> values=new java.util.TreeMap<Integer,E>();
    public SparseArray() {}
    public SparseArray(int initialCapacity) {}
    public E get(int key){return values.get(key);}
    public E get(int key,E fallback){return values.containsKey(key)?values.get(key):fallback;}
    public void put(int key,E value){values.put(key,value);}
    public void remove(int key){values.remove(key);}
    public void clear(){values.clear();}
    public int size(){return values.size();}
    public int keyAt(int index){return new java.util.ArrayList<Integer>(values.keySet()).get(index);}
    public E valueAt(int index){return values.get(keyAt(index));}
}
