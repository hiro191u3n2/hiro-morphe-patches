package app.hiro.mapsstrict;

import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Filters a single Maps search response using the query belonging to that response.
 *
 * This adapter deliberately knows one verified Maps protobuf schema. It never scans
 * arbitrary text fields, changes a shared message, or stores a "current query".
 * Unsupported schemas return the original response before the host parser sees it.
 */
public final class SearchFilter {
    private static final String TAG = "HiroMapsStrict";
    private static final AtomicBoolean WARNED = new AtomicBoolean();
    private static final AtomicBoolean PIN_WARNING = new AtomicBoolean();
    private static volatile Schema cachedSchema;
    private static volatile ModelSchema cachedModelSchema;

    private SearchFilter() {}

    public static Object filterResponse(Object response, String actualQuery, Object model) {
        ModelSchema modelSchema = null;
        try {
            modelSchema = modelSchemaFor(model);
            // Invalidate earlier success even for a category, null response, or a
            // later failure on a reused request. This is patch-owned state only.
            modelSchema.invalidateLocked(model);
            if (response == null) return null;
            StrictNameMatcher.Query query = StrictNameMatcher.classify(actualQuery);
            if (!query.enabled) return response;
            if (response.getClass() != modelSchema.responseClass) throw unsupported();
            FilteredResponse filtered = schemaFor(response).filter(response, query);
            // Every response/list copy and build has succeeded before touching
            // the existing native layer. The host parser will then store this
            // exact returned object in model.S.
            modelSchema.commitLocked(model, filtered.response);
            if (!filtered.pinsComplete) warnPinsOnce();
            return filtered.response;
        } catch (Exception failure) {
            invalidateQuietly(modelSchema, model, true);
            warnOnce();
            return response;
        } catch (LinkageError failure) {
            invalidateQuietly(modelSchema, model, true);
            warnOnce();
            return response;
        }
    }

    /**
     * Called only on the accepted merge branch of Lavdv.O, after its native-layer
     * merge and while the host holds receiver's write lock and incoming's read
     * lock. The injection must precede the instruction that overwrites incoming.
     */
    public static void reconcileModel(Object receiver, Object incoming) {
        ModelSchema modelSchema = null;
        try {
            modelSchema = modelSchemaFor(receiver);
            Schema.requireType(incoming, modelSchema.modelClass);
            Object marker = modelSchema.appliedResponse.get(incoming);
            Object incomingResponse = modelSchema.response.get(incoming);
            if (marker != null && marker == incomingResponse) {
                // This marker controls native-layer reuse. O does not replace S,
                // so it is not proof that every historical row was refiltered.
                Object receiverResponse = modelSchema.response.get(receiver);
                // O has already merged the filtered native pin metadata. Keep
                // that layer, including earlier pages when this page is empty.
                modelSchema.markUnlocked(receiver, receiverResponse);
            } else {
                modelSchema.invalidateUnlocked(receiver);
            }
        } catch (Exception failure) {
            invalidateQuietly(modelSchema, receiver, false);
            warnOnce();
        } catch (LinkageError failure) {
            invalidateQuietly(modelSchema, receiver, false);
            warnOnce();
        }
    }

    private static void invalidateQuietly(ModelSchema schema, Object model, boolean useLock) {
        if (schema == null || model == null) return;
        try {
            if (useLock) schema.invalidateLocked(model);
            else schema.invalidateUnlocked(model);
        } catch (Exception ignored) {
            // No original protobuf or layer is changed by this cleanup.
        } catch (LinkageError ignored) {
            // The fixed diagnostic is emitted by the caller.
        }
    }

    private static void warnOnce() {
        if (WARNED.compareAndSet(false, true)) {
            // Do not log the query, response, exception text, or any place data.
            try {
                Log.w(TAG, "Strict search skipped: unsupported Maps response schema.");
            } catch (RuntimeException ignored) {
                // Diagnostics cannot change a committed result.
            } catch (LinkageError ignored) {
            }
        }
    }

    private static void warnPinsOnce() {
        if (PIN_WARNING.compareAndSet(false, true)) {
            try {
                Log.w(TAG, "Strict search lists filtered; native pins partly preserved because IDs could not be verified.");
            } catch (RuntimeException ignored) {
                // This diagnostic is separate from a successful list filter.
            } catch (LinkageError ignored) {
            }
        }
    }

    private static final class FilteredResponse {
        final Object response;
        final boolean pinsComplete;

        FilteredResponse(Object response, boolean pinsComplete) {
            this.response = response;
            this.pinsComplete = pinsComplete;
        }
    }

    private static final class NativeResult {
        final Object metadata;
        final boolean complete;

        NativeResult(Object metadata, boolean complete) {
            this.metadata = metadata;
            this.complete = complete;
        }
    }

    private static Schema schemaFor(Object response) throws Exception {
        Class<?> responseClass = response.getClass();
        if (!"cqgm".equals(responseClass.getName())) throw unsupported();
        Schema schema = cachedSchema;
        if (schema != null && schema.responseClass == responseClass) return schema;
        synchronized (SearchFilter.class) {
            schema = cachedSchema;
            if (schema == null || schema.responseClass != responseClass) {
                schema = new Schema(responseClass);
                cachedSchema = schema;
            }
        }
        return schema;
    }

    private static ModelSchema modelSchemaFor(Object model) throws Exception {
        if (model == null || !"avdv".equals(model.getClass().getName())) throw unsupported();
        Class<?> modelClass = model.getClass();
        ModelSchema schema = cachedModelSchema;
        if (schema != null && schema.modelClass == modelClass) return schema;
        synchronized (SearchFilter.class) {
            schema = cachedModelSchema;
            if (schema == null || schema.modelClass != modelClass) {
                schema = new ModelSchema(modelClass);
                cachedModelSchema = schema;
            }
        }
        return schema;
    }

    private static IllegalStateException unsupported() {
        return new IllegalStateException("Unsupported Maps protobuf contract");
    }

    private static final class ModelSchema {
        final Class<?> modelClass;
        final Class<?> responseClass;
        final Class<?> lockClass;
        final Field nativeLayer;
        final Field response;
        final Field appliedResponse;
        final Method writeLock;

        ModelSchema(Class<?> actualModelClass) throws Exception {
            ClassLoader loader = actualModelClass.getClassLoader();
            modelClass = Schema.load(loader, "avdv");
            responseClass = Schema.load(loader, "cqgm");
            Class<?> layerClass = Schema.load(loader, "awfr");
            Class<?> baseClass = Schema.load(loader, "onf");
            lockClass = Schema.load(loader, "one");
            if (modelClass != actualModelClass || !baseClass.isAssignableFrom(modelClass)
                    || !AutoCloseable.class.isAssignableFrom(lockClass)) throw unsupported();
            nativeLayer = privateField(modelClass, "ag", layerClass, false);
            response = privateField(modelClass, "S", responseClass, true);
            appliedResponse = privateField(modelClass, "hiroStrictSearchResponse", Object.class, true);
            writeLock = Schema.method(baseClass, "e", lockClass);
            Schema.method(lockClass, "close", void.class);
        }

        void invalidateLocked(Object model) throws Exception {
            AutoCloseable lock = acquire(model);
            try {
                invalidateUnlocked(model);
            } finally {
                release(lock);
            }
        }

        void commitLocked(Object model, Object filtered) throws Exception {
            AutoCloseable lock = acquire(model);
            try {
                commitUnlocked(model, filtered);
            } finally {
                // A release diagnostic must not turn an already committed
                // filtered model into a return of the original response.
                release(lock);
            }
        }

        void invalidateUnlocked(Object model) throws Exception {
            Schema.requireType(model, modelClass);
            appliedResponse.set(model, null);
        }

        void commitUnlocked(Object model, Object filtered) throws Exception {
            Schema.requireType(model, modelClass);
            if (filtered != null) Schema.requireType(filtered, responseClass);
            // The layer clear is the final potentially failing write. If the
            // marker write fails, the layer is unchanged; if the layer write
            // fails, the caller invalidates the patch-owned marker.
            appliedResponse.set(model, filtered);
            nativeLayer.set(model, null);
        }

        void markUnlocked(Object model, Object filtered) throws Exception {
            Schema.requireType(model, modelClass);
            if (filtered != null) Schema.requireType(filtered, responseClass);
            appliedResponse.set(model, filtered);
        }

        private AutoCloseable acquire(Object model) throws Exception {
            Schema.requireType(model, modelClass);
            Object lock = writeLock.invoke(model);
            Schema.requireType(lock, lockClass);
            return (AutoCloseable) lock;
        }

        private void release(AutoCloseable lock) {
            try {
                lock.close();
            } catch (Exception ignored) {
                warnOnce();
            } catch (LinkageError ignored) {
                warnOnce();
            }
        }

        private static Field privateField(Class<?> owner, String name, Class<?> type,
                boolean isTransient) throws Exception {
            Field field = owner.getDeclaredField(name);
            int access = field.getModifiers();
            if (field.getType() != type || !Modifier.isPrivate(access)
                    || Modifier.isStatic(access) || Modifier.isFinal(access)
                    || Modifier.isTransient(access) != isTransient) throw unsupported();
            // The DEX patch additionally verifies the marker's synthetic bit.
            field.setAccessible(true);
            return field;
        }
    }

    private static final class Schema {
        private static final int PLACES = 1;
        private static final int WRAPPERS = 2;
        private static final int GROUPS = 3;
        private static final int PINNED = 4;
        private static final int PROMOTED = 5;
        private static final int GROUP_ITEMS = 6;

        final Class<?> responseClass;
        final Class<?> messageClass;
        final Class<?> builderClass;
        final Class<?> listClass;
        final Class<?> placeClass;
        final Class<?> wrapperClass;
        final Class<?> alternativeClass;
        final Class<?> groupClass;
        final Class<?> groupItemClass;
        final Class<?> pinnedClass;
        final Class<?> promotedClass;
        final Method toBuilder;
        final Method copyOnWrite;
        final Method build;
        final Method isMutable;
        final Method copyList;
        final Method listIsMutable;
        final Field instance;
        final Field results;
        final Field modernResults;
        final Field supplements;
        final Field mapMetadata;
        final Field name;
        final Field wrapperTag;
        final Field wrapperPayload;
        final Field alternativeName;
        final Field mapPresence;
        final Field nativeLayer;
        final Field supplementalGroups;
        final Field supplementalPinned;
        final Field supplementalPromoted;
        final Field groupContainerItems;
        final Field pinnedContainerItems;
        final Field promotedContainerItems;
        final Field groupItems;
        final Field groupItemPlace;
        final Field pinnedPlace;
        final Field promotedPlace;
        final Class<?> nativePinClass;
        final Field nativePins;
        final Field nativePresence;
        final Field nativePinFeature;
        final Field featureCid;
        final Field placeIdentity;
        final Field identityCore;
        final Field identityText;
        final Field alternativeIdentity;
        final Field nativeSelected;
        final Field nativeSelectedPoint;
        final Field nativeSpecial;
        final Field nativeSpecialPoint;
        final Field nativeGeometry;
        final Field nativeGeometryPoint;

        Schema(Class<?> actualResponseClass) throws Exception {
            ClassLoader loader = actualResponseClass.getClassLoader();
            messageClass = load(loader, "cmzs");
            builderClass = load(loader, "cmzk");
            listClass = load(loader, "cnak");
            responseClass = proto(loader, "cqgm");
            if (responseClass != actualResponseClass || !listClass.isInterface()
                    || !List.class.isAssignableFrom(listClass)) throw unsupported();
            placeClass = proto(loader, "cqdn");
            wrapperClass = proto(loader, "chaq");
            alternativeClass = proto(loader, "ccfl");
            groupClass = proto(loader, "cfhm");
            groupItemClass = proto(loader, "cffs");
            pinnedClass = proto(loader, "cfgz");
            promotedClass = proto(loader, "cfmi");
            Class<?> supplementClass = proto(loader, "cffu");
            Class<?> groupsClass = proto(loader, "cfhn");
            Class<?> pinnedContainerClass = proto(loader, "cfha");
            Class<?> promotedContainerClass = proto(loader, "cfmj");
            Class<?> mapClass = proto(loader, "cjme");
            Class<?> nativeClass = proto(loader, "cbkh");

            toBuilder = method(messageClass, "toBuilder", builderClass);
            copyOnWrite = method(builderClass, "copyOnWrite", void.class);
            build = method(builderClass, "build", messageClass);
            isMutable = method(messageClass, "isMutable", boolean.class);
            copyList = method(listClass, "e", listClass, int.class);
            listIsMutable = method(listClass, "c", boolean.class);
            instance = field(builderClass, "instance", messageClass);
            results = field(responseClass, "d", listClass);
            modernResults = field(responseClass, "L", listClass);
            supplements = field(responseClass, "i", supplementClass);
            mapMetadata = field(responseClass, "o", mapClass);
            name = field(placeClass, "l", String.class);
            wrapperTag = field(wrapperClass, "c", int.class);
            wrapperPayload = field(wrapperClass, "d", Object.class);
            alternativeName = field(alternativeClass, "d", String.class);
            mapPresence = field(mapClass, "b", int.class);
            nativeLayer = field(mapClass, "c", nativeClass);
            supplementalGroups = field(supplementClass, "c", groupsClass);
            supplementalPinned = field(supplementClass, "d", pinnedContainerClass);
            supplementalPromoted = field(supplementClass, "g", promotedContainerClass);
            groupContainerItems = field(groupsClass, "b", listClass);
            pinnedContainerItems = field(pinnedContainerClass, "b", listClass);
            promotedContainerItems = field(promotedContainerClass, "b", listClass);
            groupItems = field(groupClass, "l", listClass);
            groupItemPlace = field(groupItemClass, "c", placeClass);
            pinnedPlace = field(pinnedClass, "b", placeClass);
            promotedPlace = field(promotedClass, "b", placeClass);

            nativePinClass = proto(loader, "cbki");
            Class<?> featureClass = proto(loader, "ccpa");
            Class<?> identityClass = proto(loader, "ckiu");
            Class<?> identityCoreClass = proto(loader, "ccff");
            Class<?> selectedClass = proto(loader, "cbkj");
            Class<?> specialClass = proto(loader, "cbke");
            Class<?> geometryClass = proto(loader, "cbkg");
            nativePins = field(nativeClass, "e", listClass);
            nativePresence = field(nativeClass, "c", int.class);
            nativePinFeature = field(nativePinClass, "c", featureClass);
            featureCid = field(featureClass, "d", long.class);
            placeIdentity = field(placeClass, "h", identityClass);
            identityCore = field(identityClass, "c", identityCoreClass);
            identityText = field(identityCoreClass, "c", String.class);
            alternativeIdentity = field(alternativeClass, "c", identityCoreClass);
            nativeSelected = field(nativeClass, "d", selectedClass);
            nativeSelectedPoint = field(selectedClass, "c", nativePinClass);
            nativeSpecial = privateDataField(nativeClass, "i", specialClass);
            nativeSpecialPoint = privateDataField(specialClass, "c", nativePinClass);
            nativeGeometry = privateDataField(nativeClass, "j", geometryClass);
            nativeGeometryPoint = privateDataField(geometryClass, "c", nativePinClass);
        }

        FilteredResponse filter(Object response, StrictNameMatcher.Query query) throws Exception {
            requireType(response, responseClass);
            requireImmutable(response);
            Edit edit = new Edit(response);
            // The host falls back to d whenever L becomes empty. Filter both.
            edit.replace(results, filterList(results.get(response), PLACES, query));
            edit.replace(modernResults, filterList(modernResults.get(response), WRAPPERS, query));
            Object extra = supplements.get(response);
            if (extra != null) edit.replace(supplements, filterSupplements(extra, query));

            boolean pinsComplete = true;
            Object map = mapMetadata.get(response);
            if (map != null) {
                requireType(map, mapMetadata.getType());
                NativeResult nativeResult = filterNativeMetadata(map, response, query);
                pinsComplete = nativeResult.complete;
                edit.replace(mapMetadata, nativeResult.metadata);
            }
            return new FilteredResponse(edit.finish(), pinsComplete);
        }

        private NativeResult filterNativeMetadata(Object map, Object originalResponse,
                StrictNameMatcher.Query query) {
            try {
                Object layer = nativeLayer.get(map);
                if (layer == null) return new NativeResult(map, true);
                requireType(layer, nativeLayer.getType());
                NativeSelection selection = collectSelection(originalResponse, query);
                Object sourcePins = nativePins.get(layer);
                requireType(sourcePins, listClass);
                List<?> pins = (List<?>) sourcePins;
                ArrayList<Object> kept = new ArrayList<Object>(pins.size());
                boolean changed = false;
                boolean incomplete = !selection.complete();
                for (Object pin : pins) {
                    requireType(pin, nativePinClass);
                    Long cid = pinCid(pin);
                    if (cid == null && !selection.confirmedEmpty()) incomplete = true;
                    if (selection.keep(cid)) kept.add(pin);
                    else changed = true;
                }

                Field[] wrappers = {nativeSelected, nativeSpecial, nativeGeometry};
                Field[] points = {nativeSelectedPoint, nativeSpecialPoint, nativeGeometryPoint};
                int[] masks = {0x2, 0x20, 0x1000};
                Object[] selected = new Object[wrappers.length];
                boolean[] remove = new boolean[wrappers.length];
                int originalCount = pins.size();
                int retainedCount = kept.size();
                for (int i = 0; i < wrappers.length; i++) {
                    selected[i] = wrappers[i].get(layer);
                    if (selected[i] == null) continue;
                    requireType(selected[i], wrappers[i].getType());
                    originalCount++;
                    Object pin = points[i].get(selected[i]);
                    Long cid = pin == null ? null : pinCid(pin);
                    if (cid == null && !selection.confirmedEmpty()) incomplete = true;
                    remove[i] = !selection.keep(cid);
                    if (!remove[i]) retainedCount++;
                    else changed = true;
                }

                // Native-only rows, missing/alternate IDs, and unknown payloads
                // must not silently turn a nonempty accepted list into no pins.
                if (selection.acceptedCount > 0 && originalCount > 0 && retainedCount == 0) {
                    return new NativeResult(map, false);
                }
                if (selection.candidateCount == 0 && originalCount > 0) {
                    return new NativeResult(map, false);
                }
                if (!changed) return new NativeResult(map, originalCount == 0 || !incomplete);

                Edit layerEdit = new Edit(layer);
                if (kept.size() != pins.size()) {
                    layerEdit.replace(nativePins, copyRetainedList(sourcePins, kept));
                }
                int flags = nativePresence.getInt(layer);
                for (int i = 0; i < wrappers.length; i++) {
                    if (remove[i]) {
                        layerEdit.replace(wrappers[i], null);
                        flags &= ~masks[i];
                    }
                }
                layerEdit.replaceInt(nativePresence, flags);
                Edit mapEdit = new Edit(map);
                mapEdit.replace(nativeLayer, layerEdit.finish());
                // Keep cjme's layer-present bit and every non-pin metadata field.
                return new NativeResult(mapEdit.finish(), !incomplete || selection.confirmedEmpty());
            } catch (Exception unsupportedNativeData) {
                // The list filter can succeed independently. Keep native metadata
                // unchanged when its ID or payload contract cannot be verified.
                return new NativeResult(map, false);
            } catch (LinkageError unsupportedNativeData) {
                return new NativeResult(map, false);
            }
        }

        private Object copyRetainedList(Object original, List<?> kept) throws Exception {
            requireType(original, listClass);
            if (Boolean.TRUE.equals(listIsMutable.invoke(original))) throw unsupported();
            List<?> source = (List<?>) original;
            Object copy = copyList.invoke(original, source.size());
            requireType(copy, listClass);
            if (copy == original || !Boolean.TRUE.equals(listIsMutable.invoke(copy))) throw unsupported();
            @SuppressWarnings("unchecked")
            List<Object> writable = (List<Object>) copy;
            writable.clear();
            writable.addAll(kept);
            return copy;
        }

        private NativeSelection collectSelection(Object response, StrictNameMatcher.Query query)
                throws Exception {
            NativeSelection selection = new NativeSelection();
            collectList(results.get(response), PLACES, query, selection);
            collectList(modernResults.get(response), WRAPPERS, query, selection);
            Object extra = supplements.get(response);
            if (extra != null) {
                Object groups = supplementalGroups.get(extra);
                if (groups != null) collectList(groupContainerItems.get(groups), GROUPS, query, selection);
                Object pinned = supplementalPinned.get(extra);
                if (pinned != null) collectList(pinnedContainerItems.get(pinned), PINNED, query, selection);
                Object promoted = supplementalPromoted.get(extra);
                if (promoted != null) collectList(promotedContainerItems.get(promoted), PROMOTED, query, selection);
            }
            return selection;
        }

        private void collectList(Object list, int kind, StrictNameMatcher.Query query,
                NativeSelection selection) throws Exception {
            requireType(list, listClass);
            for (Object item : (List<?>) list) collectItem(item, kind, query, selection);
        }

        private void collectItem(Object item, int kind, StrictNameMatcher.Query query,
                NativeSelection selection) throws Exception {
            if (kind == PLACES) {
                if (item == null) {
                    selection.add(null, true);
                    return;
                }
                requireType(item, placeClass);
                Object identity = placeIdentity.get(item);
                Object core = identity == null ? null : identityCore.get(identity);
                Long cid = core == null ? null : parseCid((String) identityText.get(core));
                selection.add(cid, keepName((String) name.get(item), query));
            } else if (kind == WRAPPERS) {
                requireType(item, wrapperClass);
                Object payload = wrapperPayload.get(item);
                switch (wrapperTag.getInt(item)) {
                    case 2: collectItem(payload, PLACES, query, selection); break;
                    case 5: collectItem(payload, GROUPS, query, selection); break;
                    case 6: collectItem(payload, PROMOTED, query, selection); break;
                    case 7: collectItem(payload, PINNED, query, selection); break;
                    case 8:
                        requireType(payload, alternativeClass);
                        Object core = alternativeIdentity.get(payload);
                        Long cid = core == null ? null : parseCid((String) identityText.get(core));
                        selection.add(cid, keepName((String) alternativeName.get(payload), query));
                        break;
                    default: break; // Non-place metadata is not an ID source.
                }
            } else if (kind == GROUPS) {
                requireType(item, groupClass);
                Object children = groupItems.get(item);
                requireType(children, listClass);
                if (((List<?>) children).isEmpty()) selection.add(null, true);
                collectList(children, GROUP_ITEMS, query, selection);
            } else if (kind == GROUP_ITEMS) {
                requireType(item, groupItemClass);
                collectItem(groupItemPlace.get(item), PLACES, query, selection);
            } else if (kind == PINNED) {
                requireType(item, pinnedClass);
                collectItem(pinnedPlace.get(item), PLACES, query, selection);
            } else if (kind == PROMOTED) {
                requireType(item, promotedClass);
                collectItem(promotedPlace.get(item), PLACES, query, selection);
            } else {
                throw unsupported();
            }
        }

        private Long pinCid(Object pin) throws Exception {
            requireType(pin, nativePinClass);
            Object feature = nativePinFeature.get(pin);
            if (feature == null) return null;
            long cid = featureCid.getLong(feature);
            return cid == 0 ? null : Long.valueOf(cid);
        }

        /** CID bit patterns, including unsigned decimal, without API-26 methods. */
        private static Long parseCid(String source) {
            if (source == null || source.length() == 0 || source.length() > 80) return null;
            try {
                BigInteger cid;
                int colon = source.indexOf(':');
                if (colon >= 0) {
                    if (source.indexOf(':', colon + 1) >= 0) return null;
                    String high = source.substring(0, colon);
                    String low = source.substring(colon + 1);
                    if (!high.startsWith("0x") || !low.startsWith("0x")
                            || high.length() <= 2 || low.length() <= 2) return null;
                    BigInteger highValue = new BigInteger(high.substring(2), 16);
                    cid = new BigInteger(low.substring(2), 16);
                    if (highValue.signum() < 0 || highValue.bitLength() > 64
                            || cid.signum() < 0 || cid.bitLength() > 64) return null;
                } else {
                    if (!source.matches("-?[0-9]+")) return null;
                    cid = new BigInteger(source, 10);
                    if (cid.signum() < 0) {
                        if (cid.compareTo(BigInteger.ONE.shiftLeft(63).negate()) < 0) return null;
                    } else if (cid.bitLength() > 64) {
                        return null;
                    }
                }
                long bits = cid.longValue();
                return bits == 0 ? null : Long.valueOf(bits);
            } catch (NumberFormatException unsupportedId) {
                return null;
            }
        }

        private static final class NativeSelection {
            final Set<Long> accepted = new HashSet<Long>();
            final Set<Long> rejected = new HashSet<Long>();
            int candidateCount;
            int acceptedCount;
            boolean missingAcceptedId;

            void add(Long cid, boolean keep) {
                candidateCount++;
                if (keep) {
                    acceptedCount++;
                    if (cid == null) missingAcceptedId = true;
                    else accepted.add(cid);
                } else if (cid != null) {
                    rejected.add(cid);
                }
            }

            boolean confirmedEmpty() { return candidateCount > 0 && acceptedCount == 0; }
            boolean complete() { return candidateCount > 0 && !missingAcceptedId; }

            boolean keep(Long cid) {
                if (confirmedEmpty()) return false;
                if (cid == null) return true;
                if (accepted.contains(cid)) return true;
                if (rejected.contains(cid)) return false;
                return !complete();
            }
        }

        private Object filterSupplements(Object original, StrictNameMatcher.Query query)
                throws Exception {
            requireType(original, supplements.getType());
            Edit edit = new Edit(original);
            Object groups = supplementalGroups.get(original);
            if (groups != null) {
                edit.replace(supplementalGroups,
                        filterContainer(groups, groupContainerItems, GROUPS, query));
            }
            Object pinned = supplementalPinned.get(original);
            if (pinned != null) {
                edit.replace(supplementalPinned,
                        filterContainer(pinned, pinnedContainerItems, PINNED, query));
            }
            Object promoted = supplementalPromoted.get(original);
            if (promoted != null) {
                edit.replace(supplementalPromoted,
                        filterContainer(promoted, promotedContainerItems, PROMOTED, query));
            }
            return edit.finish();
        }

        private Object filterContainer(Object original, Field items, int kind,
                StrictNameMatcher.Query query) throws Exception {
            requireType(original, items.getDeclaringClass());
            Edit edit = new Edit(original);
            edit.replace(items, filterList(items.get(original), kind, query));
            return edit.finish();
        }

        private Object filterList(Object original, int kind, StrictNameMatcher.Query query)
                throws Exception {
            requireType(original, listClass);
            List<?> source = (List<?>) original;
            ArrayList<Object> kept = new ArrayList<Object>(source.size());
            boolean changed = false;
            for (Object item : source) {
                Object next = filterItem(item, kind, query);
                if (next != null) kept.add(next);
                if (next != item) changed = true;
            }
            if (!changed) return original;
            // Never clear/remove from a list borrowed from the source message.
            if (Boolean.TRUE.equals(listIsMutable.invoke(original))) throw unsupported();
            Object copied = copyList.invoke(original, source.size());
            requireType(copied, listClass);
            if (copied == original || !Boolean.TRUE.equals(listIsMutable.invoke(copied))) {
                throw unsupported();
            }
            @SuppressWarnings("unchecked")
            List<Object> writable = (List<Object>) copied;
            writable.clear();
            writable.addAll(kept);
            return copied;
        }

        private Object filterItem(Object item, int kind, StrictNameMatcher.Query query)
                throws Exception {
            switch (kind) {
                case PLACES:
                    requireType(item, placeClass);
                    return keepName((String) name.get(item), query) ? item : null;
                case WRAPPERS:
                    return filterWrapper(item, query);
                case GROUPS:
                    return filterGroup(item, query);
                case PINNED:
                    requireType(item, pinnedClass);
                    return keepOptionalPlace(pinnedPlace.get(item), query) ? item : null;
                case PROMOTED:
                    requireType(item, promotedClass);
                    return keepOptionalPlace(promotedPlace.get(item), query) ? item : null;
                case GROUP_ITEMS:
                    requireType(item, groupItemClass);
                    return keepOptionalPlace(groupItemPlace.get(item), query) ? item : null;
                default:
                    throw unsupported();
            }
        }

        private Object filterWrapper(Object original, StrictNameMatcher.Query query)
                throws Exception {
            requireType(original, wrapperClass);
            int tag = wrapperTag.getInt(original);
            Object payload = wrapperPayload.get(original);
            Object next;
            switch (tag) {
                case 2: // Ordinary placemark.
                    next = filterItem(payload, PLACES, query);
                    break;
                case 5: // Supplemental/advertising placemark group.
                    next = filterGroup(payload, query);
                    break;
                case 6: // cfmi promoted placemark.
                    next = filterItem(payload, PROMOTED, query);
                    break;
                case 7: // cfgz supplementary placemark.
                    next = filterItem(payload, PINNED, query);
                    break;
                case 8: // ccfl title is copied by Lokx.ab/W/a into cqdn.l.
                    requireType(payload, alternativeClass);
                    next = keepName((String) alternativeName.get(payload), query) ? payload : null;
                    break;
                default:
                    // Sections, continuation metadata, and unrecognized payloads are
                    // not presumed to be named places. Preserve them verbatim.
                    return original;
            }
            if (next == null) return null;
            if (next == payload) return original;
            Edit edit = new Edit(original);
            edit.replace(wrapperPayload, next);
            return edit.finish();
        }

        private Object filterGroup(Object original, StrictNameMatcher.Query query)
                throws Exception {
            requireType(original, groupClass);
            Object before = groupItems.get(original);
            Object after = filterList(before, GROUP_ITEMS, query);
            if (after == before) return original;
            // A group whose identified places were all excluded must not remain
            // as a standalone unrelated card. Empty/unknown original groups stay.
            if (((List<?>) after).isEmpty()) return null;
            Edit edit = new Edit(original);
            edit.replace(groupItems, after);
            return edit.finish();
        }

        private boolean keepOptionalPlace(Object place, StrictNameMatcher.Query query)
                throws Exception {
            if (place == null) return true;
            requireType(place, placeClass);
            return keepName((String) name.get(place), query);
        }

        private boolean keepName(String title, StrictNameMatcher.Query query) {
            // Empty/missing display names are unknown, not proved mismatches.
            if (title == null || title.trim().length() == 0) return true;
            return StrictNameMatcher.matches(query, title);
        }

        private void requireImmutable(Object original) throws Exception {
            if (!messageClass.isInstance(original)
                    || Boolean.TRUE.equals(isMutable.invoke(original))) throw unsupported();
        }

        private final class Edit {
            final Object original;
            Object builder;
            Object writable;

            Edit(Object original) {
                this.original = original;
            }

            private void begin() throws Exception {
                if (builder != null) return;
                requireImmutable(original);
                builder = toBuilder.invoke(original);
                requireType(builder, builderClass);
                copyOnWrite.invoke(builder);
                writable = instance.get(builder);
                if (writable == original || writable == null
                        || writable.getClass() != original.getClass()
                        || !Boolean.TRUE.equals(isMutable.invoke(writable))) throw unsupported();
            }

            void replace(Field target, Object value) throws Exception {
                if (target.get(original) == value) return;
                begin();
                target.set(writable, value);
            }

            void replaceInt(Field target, int value) throws Exception {
                if (target.getInt(original) == value) return;
                begin();
                target.setInt(writable, value);
            }

            Object finish() throws Exception {
                if (builder == null) return original;
                Object result = build.invoke(builder);
                if (result == original || result == null
                        || result.getClass() != original.getClass()) throw unsupported();
                requireImmutable(result);
                return result;
            }
        }

        private Class<?> proto(ClassLoader loader, String name) throws Exception {
            Class<?> type = load(loader, name);
            if (!messageClass.isAssignableFrom(type)) throw unsupported();
            return type;
        }

        private static Class<?> load(ClassLoader loader, String name) throws Exception {
            return Class.forName(name, false, loader);
        }

        private static void requireType(Object value, Class<?> expected) {
            if (value == null || !expected.isInstance(value)) throw unsupported();
        }

        private static Field field(Class<?> owner, String name, Class<?> type) throws Exception {
            Field value = owner.getDeclaredField(name);
            if (value.getType() != type || !Modifier.isPublic(value.getModifiers())
                    || Modifier.isStatic(value.getModifiers())) throw unsupported();
            value.setAccessible(true);
            return value;
        }

        private static Field privateDataField(Class<?> owner, String name, Class<?> type) throws Exception {
            Field value = owner.getDeclaredField(name);
            if (value.getType() != type || !Modifier.isPrivate(value.getModifiers())
                    || Modifier.isStatic(value.getModifiers())) throw unsupported();
            value.setAccessible(true);
            return value;
        }

        private static Method method(Class<?> owner, String name, Class<?> returns,
                Class<?>... arguments) throws Exception {
            Method found = null;
            for (Method value : owner.getDeclaredMethods()) {
                if (value.getName().equals(name) && value.getReturnType() == returns
                        && Arrays.equals(value.getParameterTypes(), arguments)) {
                    if (found != null || Modifier.isStatic(value.getModifiers())) throw unsupported();
                    found = value;
                }
            }
            if (found == null) throw unsupported();
            found.setAccessible(true);
            return found;
        }
    }
}
