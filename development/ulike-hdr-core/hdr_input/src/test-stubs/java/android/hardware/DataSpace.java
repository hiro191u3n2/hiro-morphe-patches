package android.hardware;
public final class DataSpace {
    public static final int STANDARD_BT2020=6<<16, TRANSFER_HLG=7<<22;
    public static final int RANGE_FULL=1<<27, RANGE_LIMITED=2<<27;
    public static int getStandard(int d) { return d & (63<<16); }
    public static int getTransfer(int d) { return d & (31<<22); }
    public static int getRange(int d) { return d & (7<<27); }
}
