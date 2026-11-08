import com.google.common.collect.ImmutableList;
import java.util.ArrayList;

/** Host stand-in for verified Lbken.j override precedence, not an Android test. */
public final class bken {
    public static int calls;
    public static boolean throwForTest;
    public static bkje j(ImmutableList<?> overrides, bkje original) {
        calls++;
        if (throwForTest) throw new IllegalStateException("Fixture resolver failure");
        if (original.c == null || overrides.isEmpty()) return original;
        ArrayList<bkjc> out = new ArrayList<bkjc>();
        for (bkjc item : original.c) {
            bkjc result = item;
            for (int i = overrides.size() - 1; item.d != 0 && i >= 0; i--) {
                chue override = (chue)overrides.get(i);
                if (override.c != item.d) continue;
                result = new bkjc();
                result.d = 0;
                if ((override.b & 4) != 0) result.b = "";
                else if ((override.b & 1024) != 0) {
                    result.b = override.f;
                    if (!result.b.startsWith("http://") && !result.b.startsWith("https://")
                            && !result.b.startsWith("//") && !result.b.startsWith("data:")) {
                        result.b = "icon/name=" + result.b + "&scale=4";
                    }
                } else result.b = "";
                break;
            }
            out.add(result);
        }
        bkje value = new bkje();
        value.c = ImmutableList.copyOf(out);
        return value;
    }
}
