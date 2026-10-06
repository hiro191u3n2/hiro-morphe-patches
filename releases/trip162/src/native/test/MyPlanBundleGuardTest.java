import app.hiro.tripcom.extension.MyPlanBundleGuard;
import android.content.Context;
import ctrip.foundation.FoundationContextHolder;
import ctrip.android.pkg.util.Un7zUtil;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public final class MyPlanBundleGuardTest {
    static final String PRODUCT = "rn_xtaro_ibu_schedule";
    static final String DIRECTORY = PRODUCT + "_hiro_v1_10_15_0194baa9f2e6";
    static final String BUNDLE = "rn_business.jsbundle";
    static final List<String> passed = new ArrayList<String>();
    static Path root;
    static void req(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    static void pass(String message) { passed.add(message); }
    static void reset() throws Exception {
        Field field = MyPlanBundleGuard.class.getDeclaredField("readyDirectory");
        field.setAccessible(true); field.set(null, null);
        Un7zUtil.calls = 0; Un7zUtil.mode = "ok";
    }
    static Path prepare(String name) throws Exception {
        reset();
        Path data = root.resolve(name);
        FoundationContextHolder.context = new Context(data.toFile());
        Path path = data.resolve("app_ctripwebapp6_8.54.2_30041599").resolve(PRODUCT);
        Files.createDirectories(path);
        Files.write(path.resolve(BUNDLE), "cached old JS".getBytes("UTF-8"));
        Files.write(path.resolve("rn_business.hbcbundle"), new byte[]{9,8,7});
        Files.write(path.resolve("_crn_config_v6"), "online HBC".getBytes("UTF-8"));
        Path dependency = path.getParent().resolve("rn_ibu_common");
        Files.createDirectories(dependency);
        Files.write(dependency.resolve("keep"), new byte[]{42});
        return path;
    }
    static void preserved(Path original) throws Exception {
        req("cached old JS".equals(new String(Files.readAllBytes(original.resolve(BUNDLE)), "UTF-8")), "Old normal JS changed");
        req(Arrays.equals(new byte[]{9,8,7}, Files.readAllBytes(original.resolve("rn_business.hbcbundle"))), "Normal HBC changed");
        req(Files.exists(original.resolve("_crn_config_v6")), "Normal config removed");
        req(Files.readAllBytes(original.getParent().resolve("rn_ibu_common/keep"))[0] == 42, "Dependency modified");
    }
    public static void main(String[] args) throws Exception {
        root = Paths.get(args[0]); Files.createDirectories(root);
        Un7zUtil.fixture = Paths.get(args[1]);
        Path original = prepare("initial");
        String path = original.toString();
        req(path.equals(MyPlanBundleGuard.loadPath(path, "rn_ibu_flight")), "Other product redirected");
        req(MyPlanBundleGuard.loadPath(null, PRODUCT) == null, "Null changed");
        req(MyPlanBundleGuard.businessPath("https://example.com/" + PRODUCT + "/_crn_config?x=1") == null, "Remote path redirected");
        req(MyPlanBundleGuard.businessPath(original.getParent() + "/rn_other/_crn_config?product=" + PRODUCT) == null, "Query-only match redirected");
        req(Un7zUtil.calls == 0, "Unrelated paths caused extraction");
        pass("Unrelated products, nulls, network URLs, and query-only matches remain unchanged");
        FoundationContextHolder.context = null;
        req(path.equals(MyPlanBundleGuard.loadPath(path, PRODUCT)), "Missing context did not fall back");
        req(MyPlanBundleGuard.businessPath(path + "/_crn_config") == null, "Missing context business fallback");
        req(Un7zUtil.calls == 0, "Missing context extracted");
        FoundationContextHolder.context = new Context(root.resolve("initial").toFile());
        pass("Early calls without application context safely retain original behavior");
        String mapped = MyPlanBundleGuard.loadPath(path, PRODUCT);
        Path isolated = Paths.get(mapped);
        req(!mapped.equals(path), "First load retained stale path");
        req(isolated.getParent().equals(original.getParent()), "Common dependency parent changed");
        req(isolated.getFileName().toString().equals(DIRECTORY), "Wrong revision directory");
        req(Arrays.equals(Files.readAllBytes(Un7zUtil.fixture), Files.readAllBytes(isolated.resolve(BUNDLE))), "Bundle differs from shipped fixture");
        req(!Files.exists(isolated.resolve("_crn_config_v6")) && !Files.exists(isolated.resolve("rn_business.hbcbundle")), "Isolated directory reused old HBC");
        req(Un7zUtil.calls == 1, "First load extraction count differs");
        req(Un7zUtil.archive.equals("webapp/rn_xtaro_ibu_schedule-431156475-30041599.7z"), "Wrong asset archive");
        req(MyPlanBundleGuard.isPinnedPath(mapped, PRODUCT), "Pinned identity was not captured");
        req(!MyPlanBundleGuard.isPinnedPath(path, PRODUCT), "Normal cached path claimed to be pinned");
        req(!MyPlanBundleGuard.isPinnedPath(mapped, "rn_ibu_flight"), "Other product claimed pinned identity");
        for (int status : new int[]{-6002, -1, 0, 1, 2}) {
            req(MyPlanBundleGuard.resourceKeySucceeded(status, false) == (status == 0), "Original result semantics changed");
            req(MyPlanBundleGuard.resourceKeySucceeded(status, true) == (status == 0 || status == 1), "Pinned resource result semantics wrong");
        }
        pass("Native fallback result1 initializes resources only for the confirmed revision while all original result codes remain unchanged");
        preserved(original);
        pass("First load extracts exact reviewed asset to revision sibling while preserving cached JS, HBC, config, and common libraries");
        for (String url : Arrays.asList(path + "/_crn_config?CRNModuleName=xtaro&foo=1", "file://" + path + "/_crn_config?x=1#part", path + "/main.is#part", path + "/rn_business.hbcbundle")) {
            req(isolated.resolve(BUNDLE).toString().equals(MyPlanBundleGuard.businessPath(url)), "Module identity did not use same reviewed raw bundle");
        }
        req(mapped.equals(MyPlanBundleGuard.loadPath(path, PRODUCT)), "Warm load differs");
        req(Un7zUtil.calls == 1, "Warm paths extracted repeatedly");
        pass("Cold and warm CRN module paths resolve to the same raw bundle independently of cached Hermes choice");
        byte[] mutated = Files.readAllBytes(isolated.resolve(BUNDLE)); mutated[0] ^= 1;
        Files.write(isolated.resolve(BUNDLE), mutated);
        req(mapped.equals(MyPlanBundleGuard.loadPath(path, PRODUCT)), "Changed warm revision not repaired");
        req(Un7zUtil.calls == 2, "Same-size changed warm revision bypassed integrity validation");
        req(Arrays.equals(Files.readAllBytes(Un7zUtil.fixture), Files.readAllBytes(isolated.resolve(BUNDLE))), "Warm corruption survived");
        pass("Every warm load verifies SHA-256, including same-size in-process changes");
        reset(); Un7zUtil.mode = "fail";
        req(mapped.equals(MyPlanBundleGuard.loadPath(path, PRODUCT)), "Valid revision failed after process-cache reset");
        req(Un7zUtil.calls == 0, "Already-valid revision unnecessarily extracted");
        pass("Verified persisted revision remains usable across simulated process restart without extraction");
        original = prepare("corrupt"); path = original.toString();
        Un7zUtil.mode = "corrupt";
        req(path.equals(MyPlanBundleGuard.loadPath(path, PRODUCT)), "Corrupt bytes accepted");
        req(!Files.exists(original.getParent().resolve(DIRECTORY)), "Corrupt bytes published");
        preserved(original);
        Un7zUtil.mode = "ok";
        req(!path.equals(MyPlanBundleGuard.loadPath(path, PRODUCT)), "Failed extraction could not recover");
        req(Un7zUtil.calls == 2, "Recovery did not retry once");
        pass("Same-length corrupt bundle is rejected by SHA-256 and a later valid extraction recovers");
        for (String mode : Arrays.asList("fail", "throw", "extra")) {
            original = prepare(mode); path = original.toString(); Un7zUtil.mode = mode;
            req(path.equals(MyPlanBundleGuard.loadPath(path, PRODUCT)), "Extractor failure did not retain path " + mode);
            req(!Files.exists(original.getParent().resolve(DIRECTORY)), "Failed/extra extraction published " + mode);
            preserved(original);
        }
        pass("Extractor failure, exception, and unexpected HBC members retain original paths without touching normal cache");
        original = prepare("repair"); path = original.toString();
        Path partial = original.getParent().resolve(DIRECTORY); Files.createDirectories(partial);
        Files.write(partial.resolve(BUNDLE), new byte[]{0});
        req(!path.equals(MyPlanBundleGuard.loadPath(path, PRODUCT)), "Partial owned revision not repaired");
        preserved(original);
        pass("Interrupted partial revision is repaired using only the implementation-owned single-file directory");
        original = prepare("symlink"); path = original.toString();
        Files.createSymbolicLink(original.getParent().resolve(DIRECTORY), original);
        req(path.equals(MyPlanBundleGuard.loadPath(path, PRODUCT)), "Symlinked revision followed");
        req(Un7zUtil.calls == 0, "Symlinked revision caused writes"); preserved(original);
        pass("A revision-directory symlink cannot redirect cleanup into the normal application cache");
        original = prepare("concurrent"); final String concurrentPath = original.toString();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<String>> calls = new ArrayList<Future<String>>();
        for (int index = 0; index < 24; index++) calls.add(pool.submit(() -> MyPlanBundleGuard.loadPath(concurrentPath, PRODUCT)));
        Set<String> values = new HashSet<String>();
        for (Future<String> value : calls) values.add(value.get());
        pool.shutdown();
        req(values.size() == 1 && !values.contains(concurrentPath), "Concurrent callers diverged");
        req(Un7zUtil.calls == 1, "Concurrent callers extracted more than once"); preserved(original);
        pass("Concurrent preload and UI callers receive one complete atomic revision with one extraction");
        original = prepare("scope");
        Path outside = root.resolve("external/app_ctripwebapp6/" + PRODUCT); Files.createDirectories(outside);
        req(outside.toString().equals(MyPlanBundleGuard.loadPath(outside.toString(), PRODUCT)), "External directory accepted");
        Path unrelatedRoot = root.resolve("scope/other_folder/" + PRODUCT); Files.createDirectories(unrelatedRoot);
        req(unrelatedRoot.toString().equals(MyPlanBundleGuard.loadPath(unrelatedRoot.toString(), PRODUCT)), "Unrelated private directory accepted");
        req(Un7zUtil.calls == 0, "Rejected locations caused writes");
        pass("File operations are restricted to an application-private CRN work root");
        original = prepare("module-first"); path = original.toString();
        String firstModule = MyPlanBundleGuard.businessPath(path + "/_crn_config?x=1");
        req(firstModule != null && firstModule.endsWith("/" + DIRECTORY + "/" + BUNDLE), "Module resolver first-call extraction failed");
        req(firstModule.equals(MyPlanBundleGuard.loadPath(path, PRODUCT) + "/" + BUNDLE), "Module-first and native-load paths disagree");
        req(Un7zUtil.calls == 1, "Module-first extracted twice"); preserved(original);
        pass("Module-path resolution can initialize the revision before the native load without path divergence");
        System.out.println("{\"result\":\"PASS\",\"scenario_count\":" + passed.size() + ",\"android_device_tested\":false,\"scenarios\":[");
        for (int index = 0; index < passed.size(); index++) {
            System.out.println("\"" + passed.get(index) + "\"" + (index + 1 == passed.size() ? "" : ","));
        }
        System.out.println("]}");
    }
}
