/* Test adapter: merges the exact published extension bytes without pretending to
 * reproduce upstream's native TikTok settings-menu injection. Not distributed. */
package fixture;
import java.io.InputStream;import java.util.*;import java.util.function.Supplier;
import kotlin.Pair;import kotlin.Unit;import kotlin.jvm.functions.Function1;
import app.morphe.patcher.patch.BytecodePatch;import app.morphe.patcher.patch.BytecodePatchContext;
public final class ImportMetraFixture {
    // Desktop sorts selected patches by name, independently of -p order. These
    // two aliases intentionally straddle "TikTok..." so both real execution
    // orders are exercised and verified by the observation below.
    public static final BytecodePatch BEFORE=make("Fixture Metra extension");
    public static final BytecodePatch AFTER=make("ZZZ Fixture Metra extension");
    private static BytecodePatch make(String name){return new BytecodePatch(name, "Test only",false,
        (Set<Pair<String,Set<String>>>)null,Collections.emptySet(),Collections.emptySet(),new Supplier<InputStream>(){
            public InputStream get(){return ImportMetraFixture.class.getResourceAsStream("/extensions/published.mpe");}
        },new Function1<BytecodePatchContext,Unit>(){
            public Unit invoke(BytecodePatchContext context){
                System.out.println("IMPORT_ORDER_JAPANESE_PRESENT="+(context.classDefByOrNull("Lapp/hiro/tiktokjapanese/runtime/JapaneseTranslations;")!=null));
                return Unit.INSTANCE;
            }
        },null);}
}
