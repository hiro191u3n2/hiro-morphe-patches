/* NR5–NR8 single-image range backend. StrongNoise1957.java is the oracle.
 * All array reads are bounded row regions. Requested output is transactional:
 * errors or cancellation leave the caller's array untouched.
 */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>
#include <math.h>

typedef struct {
    jint *pixels;
    int width, height, first, finish;
} Region;

typedef struct {
    jobject thread;
    jmethodID interrupted;
} Cancellation;

static int mini(int a, int b) { return a < b ? a : b; }
static int maxi(int a, int b) { return a > b ? a : b; }
static float minf(float a, float b) { return a < b ? a : b; }
static float maxf(float a, float b) { return a > b ? a : b; }
static float clampf(float a, float lo, float hi) { return maxf(lo, minf(hi, a)); }
static int ceil_div2(int a) { return a / 2 + (a & 1); }
static int round_float(float v) { return (int)floor((double)v + .5); }
static int byte_value(float v) { return maxi(0, mini(255, round_float(v))); }
static float square(float v) { return v * v; }
static float luma(uint32_t p) {
    return .299f * ((p >> 16) & 255) + .587f * ((p >> 8) & 255) + .114f * (p & 255);
}

static int cancellation_start(JNIEnv *env, Cancellation *c) {
    jclass cls = (*env)->FindClass(env, "java/lang/Thread");
    if (!cls) return 0;
    jmethodID current = (*env)->GetStaticMethodID(env, cls, "currentThread", "()Ljava/lang/Thread;");
    c->interrupted = (*env)->GetMethodID(env, cls, "isInterrupted", "()Z");
    if (!current || !c->interrupted) { (*env)->DeleteLocalRef(env, cls); return 0; }
    c->thread = (*env)->CallStaticObjectMethod(env, cls, current);
    (*env)->DeleteLocalRef(env, cls);
    return c->thread && !(*env)->ExceptionCheck(env);
}
static int cancelled(JNIEnv *env, const Cancellation *c) {
    return (*env)->CallBooleanMethod(env, c->thread, c->interrupted) || (*env)->ExceptionCheck(env);
}

static int copy_region(JNIEnv *env, jintArray array, int width, int height,
        int first, int finish, Region *r) {
    int64_t source_count = (int64_t)width * height;
    int64_t count = (int64_t)width * (finish - first);
    if (!array || width < 1 || height < 1 || first < 0 || finish < first || finish > height ||
        source_count > INT_MAX || count > INT_MAX || (uint64_t)count > SIZE_MAX / sizeof(jint) ||
        source_count > (*env)->GetArrayLength(env, array)) return 0;
    r->width = width; r->height = height; r->first = first; r->finish = finish;
    if (count == 0) return 1;
    r->pixels = malloc((size_t)count * sizeof(jint));
    if (!r->pixels) return 0;
    (*env)->GetIntArrayRegion(env, array, first * width, (jsize)count, r->pixels);
    return !(*env)->ExceptionCheck(env);
}
static uint32_t pixel(const Region *r, int x, int y) {
    x = maxi(0, mini(r->width - 1, x));
    y = maxi(0, mini(r->height - 1, y));
    return (uint32_t)r->pixels[(size_t)(y - r->first) * r->width + x];
}

static float sigma(const float *values, int offset, float mean) {
    float x = clampf((mean - 16) / 32, 0, 7);
    int a = (int)x, b = mini(7, a + 1);
    return values[offset + a] * (1 - (x - a)) + values[offset + b] * (x - a);
}
static float structure(const Region *r, int x, int y, int valid_begin, int valid_end) {
    float left = 0, right = 0, up = 0, down = 0;
    int n = 0;
    for (int k = -1; k <= 1; k++) {
        uint32_t a = pixel(r, x - 2, maxi(valid_begin, mini(valid_end - 1, y + k)));
        uint32_t b = pixel(r, x + 2, maxi(valid_begin, mini(valid_end - 1, y + k)));
        uint32_t c = pixel(r, x + k, maxi(valid_begin, mini(valid_end - 1, y - 2)));
        uint32_t d = pixel(r, x + k, maxi(valid_begin, mini(valid_end - 1, y + 2)));
        if ((a >> 24) != 255 || (b >> 24) != 255 || (c >> 24) != 255 || (d >> 24) != 255) return 255;
        left += luma(a); right += luma(b); up += luma(c); down += luma(d); n++;
    }
    return (fabsf(right - left) + fabsf(down - up)) / n;
}
static uint32_t pack(int guide, float r, float g, float b) {
    return ((uint32_t)maxi(0, mini(255, guide)) << 24) |
        ((uint32_t)(maxi(-127, mini(127, round_float(r))) & 255) << 16) |
        ((uint32_t)(maxi(-127, mini(127, round_float(g))) & 255) << 8) |
        (uint32_t)(maxi(-127, mini(127, round_float(b))) & 255);
}
static void nonlocal(const Region *src, int x, int y, float sy, float sc, float *result) {
    uint32_t p = pixel(src, x, y);
    float yy = luma(p), rr = ((p >> 16) & 255) - yy, bb = (p & 255) - yy;
    float total = 1, sum_y = yy, sum_r = rr, sum_b = bb;
    float variance = sy * sy + .22f * sc * sc, h2 = maxf(1.5f, variance * 2.6f);
    for (int dy = -6; dy <= 6; dy += 3) for (int dx = -6; dx <= 6; dx += 3) {
        if (dx == 0 && dy == 0) continue;
        int cx = x + dx, cy = y + dy;
        if (cx < 0 || cy < 0 || cx >= src->width || cy >= src->height) continue;
        uint32_t q = pixel(src, cx, cy);
        if ((q >> 24) != 255) continue;
        float distance = 0;
        int valid = 1;
        for (int py = -1; py <= 1; py++) for (int px = -1; px <= 1; px++) {
            uint32_t a = pixel(src, x + px, y + py), b = pixel(src, cx + px, cy + py);
            if ((a >> 24) != 255 || (b >> 24) != 255) { valid = 0; continue; }
            float ya = luma(a), yb = luma(b), ra = ((a >> 16) & 255) - ya;
            float rb = ((b >> 16) & 255) - yb, ba = (a & 255) - ya, bc = (b & 255) - yb;
            distance += square(ya - yb) + .11f * (square(ra - rb) + square(ba - bc));
        }
        if (!valid) continue;
        distance = maxf(0, distance / 9 - 2 * variance);
        float z = distance / h2;
        if (z > 5) continue;
        float weight = 1 / (1 + z + z * z * .5f + z * z * z / 6);
        float qy = luma(q);
        sum_y += weight * qy; sum_r += weight * (((q >> 16) & 255) - qy);
        sum_b += weight * ((q & 255) - qy); total += weight;
    }
    result[0] = sum_y / total; result[1] = sum_r / total; result[2] = sum_b / total;
}
static void prepare_pixel(const Region *src, int x, int y, int noise, int shadows,
        const float *ev, int mode, jint *out) {
    uint32_t p = pixel(src, x, y);
    float y0 = luma(p), cr = ((p >> 16) & 255) - y0, cb = (p & 255) - y0;
    if ((p >> 24) != 255) { *out = 0; return; }
    float sy = sigma(ev, 0, y0), sc = sigma(ev, 8, y0);
    if (sy < .40f && sc < .40f) { *out = (jint)((uint32_t)round_float(y0) << 24); return; }
    float threshold_y = maxf(3, sy * 4.5f), threshold_c = maxf(5, sc * 5.5f);
    float sum_y = 0, sum_r = 0, sum_b = 0, weight_y = 0, weight_c = 0, var_y = 0;
    for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) {
        uint32_t q = pixel(src, x + dx, y + dy);
        if ((q >> 24) != 255) continue;
        float yy = luma(q), rr = ((q >> 16) & 255) - yy, bb = (q & 255) - yy;
        float base = dx == 0 && dy == 0 ? 4.f : abs(dx) <= 1 && abs(dy) <= 1 ? 2.f : 1.f;
        float wy = base / (1 + square((yy - y0) / threshold_y));
        float wc = base / (1 + square((yy - y0) / threshold_y) + square((rr - cr) / threshold_c) + square((bb - cb) / threshold_c));
        sum_y += wy * yy; weight_y += wy; var_y += wy * square(yy - y0);
        sum_r += wc * rr; sum_b += wc * bb; weight_c += wc;
    }
    float dark = clampf((144 - y0) / 112, 0, 1);
    float strength = .60f + .08f * noise + (shadows ? .10f * dark : 0);
    float local_var = var_y / maxf(.001f, weight_y);
    float edge = structure(src, x, y, 0, src->height);
    float flat = 1 / (1 + square(edge / (sy * 3 + 3)));
    float ly = sy >= .40f ? strength * flat * clampf(sy * sy / maxf(sy * sy, local_var * .55f), .12f, 1) : 0;
    float lc = sc >= .40f ? minf(.98f, strength + .10f) * flat : 0;
    float target_y = sum_y / maxf(.001f, weight_y), target_r = sum_r / maxf(.001f, weight_c), target_b = sum_b / maxf(.001f, weight_c);
    if (mode == 1 && dark > .12f && (sy >= .40f || sc >= .40f) && flat > .25f) {
        float nl[3]; nonlocal(src, x, y, sy, sc, nl);
        float blend = .65f * flat;
        target_y = target_y * (1 - blend) + nl[0] * blend;
        target_r = target_r * (1 - blend) + nl[1] * blend;
        target_b = target_b * (1 - blend) + nl[2] * blend;
        ly = minf(.95f, ly + .15f * flat); lc = minf(.99f, lc + .10f * flat);
    }
    float dy = (target_y - y0) * ly, dr = (target_r - cr) * lc, db = (target_b - cb) * lc;
    *out = (jint)pack(round_float(y0), dy + dr, dy - (.299f * dr + .114f * db) / .587f, dy + db);
}
static void sample(const Region *map, int x, int y, int scale, float *out) {
    float fx = clampf((x + .5f) / scale - .5f, 0, map->width - 1);
    float fy = clampf((y + .5f) / scale - .5f, 0, map->height - 1);
    int ix = (int)fx, iy = (int)fy;
    int nx = mini(map->width - 1, ix + 1), ny = mini(map->height - 1, iy + 1);
    fx -= ix; fy -= iy;
    uint32_t a = pixel(map, ix, iy), b = pixel(map, nx, iy), c = pixel(map, ix, ny), d = pixel(map, nx, ny);
    for (int plane = 0; plane < 4; plane++) {
        int shift = plane == 3 ? 24 : 16 - plane * 8;
        float av = plane == 3 ? (float)(a >> 24) : (float)(int8_t)(a >> shift);
        float bv = plane == 3 ? (float)(b >> 24) : (float)(int8_t)(b >> shift);
        float cv = plane == 3 ? (float)(c >> 24) : (float)(int8_t)(c >> shift);
        float dv = plane == 3 ? (float)(d >> 24) : (float)(int8_t)(d >> shift);
        out[plane] = (av * (1 - fx) + bv * fx) * (1 - fy) + (cv * (1 - fx) + dv * fx) * fy;
    }
}
static void process_pixel(const Region *input, const Region *maps, int x, int row,
        int valid_begin, int valid_end, int origin_y, int noise, int shadows,
        const float *ev, const jint *policy, jint *out) {
    uint32_t p = pixel(input, x, row);
    if ((p >> 24) != 255) { *out = (jint)p; return; }
    int budget = policy ? maxi(0, mini(256, policy[0])) : 256;
    int detail = policy ? maxi(0, mini(256, policy[1])) : 0;
    if (budget == 0) { *out = (jint)p; return; }
    float y0 = luma(p), r0 = ((p >> 16) & 255) - y0, b0 = (p & 255) - y0;
    float sy = sigma(ev, 0, y0), sc = sigma(ev, 8, y0);
    float edge = structure(input, x, row, valid_begin, valid_end);
    float flat = 1 / (1 + square(edge / (sy * 3 + 3))), dark = clampf((144 - y0) / 112, 0, 1);
    float threshold_y = maxf(2.5f, sy * 4), threshold_c = maxf(5, sc * 5);
    float sum_y = 0, sum_r = 0, sum_b = 0, wy_total = 0, wc_total = 0, var = 0;
    for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) {
        uint32_t q = pixel(input, x + dx, maxi(valid_begin, mini(valid_end - 1, row + dy)));
        if ((q >> 24) != 255) continue;
        float yy = luma(q), rr = ((q >> 16) & 255) - yy, bb = (q & 255) - yy;
        float base = dx == 0 && dy == 0 ? 4.f : abs(dx) <= 1 && abs(dy) <= 1 ? 2.f : 1.f;
        float wy = base / (1 + square((yy - y0) / threshold_y));
        float wc = base / (1 + square((yy - y0) / threshold_y) + square((rr - r0) / threshold_c) + square((bb - b0) / threshold_c));
        sum_y += wy * yy; wy_total += wy; var += wy * square(yy - y0);
        sum_r += wc * rr; sum_b += wc * bb; wc_total += wc;
    }
    float strength = (.56f + .09f * noise + (shadows ? .12f * dark : 0)) * ((160 + budget * .375f) / 256.f);
    float texture = 1 - .55f * detail / 256.f;
    float ly = sy >= .40f ? .70f * minf(.95f, strength) * flat * texture * clampf(sy * sy / maxf(sy * sy, var / maxf(.001f, wy_total) * .55f), .16f, 1) : 0;
    float lc = sc >= .40f ? minf(.98f, strength + .10f) * flat * texture : 0;
    float dy = (sum_y / maxf(.001f, wy_total) - y0) * ly;
    float dr = (sum_r / maxf(.001f, wc_total) - r0) * lc, db = (sum_b / maxf(.001f, wc_total) - b0) * lc;
    float d_r = dy + dr, d_g = dy - (.299f * dr + .114f * db) / .587f, d_b = dy + db;
    for (int k = 0; k < 3; k++) {
        float coarse[4]; sample(&maps[k], x, row + origin_y, 1 << (k + 1), coarse);
        float gate = 1 / (1 + square((coarse[3] - y0) / (sy * 5 + 16)));
        float blend = (k == 0 ? .48f : k == 1 ? .32f : .22f) * minf(1, strength) * texture * gate * (.30f + .70f * flat);
        d_r += coarse[0] * blend; d_g += coarse[1] * blend; d_b += coarse[2] * blend;
    }
    *out = (jint)((p & UINT32_C(0xff000000)) | ((uint32_t)byte_value(((p >> 16) & 255) + d_r) << 16) |
        ((uint32_t)byte_value(((p >> 8) & 255) + d_g) << 8) | (uint32_t)byte_value((p & 255) + d_b));
}

JNIEXPORT jint JNICALL Java_com_hiro_ulike_StrongNoise1957_nativeAbi(JNIEnv *env, jclass cls) {
    (void)env; (void)cls; return 1957;
}

JNIEXPORT jboolean JNICALL Java_com_hiro_ulike_StrongNoise1957_processNative(
        JNIEnv *env, jclass cls, jintArray input_array, jintArray output_array,
        jint width, jint rows, jint begin, jint end, jint valid_begin, jint valid_end,
        jint origin_y, jint noise, jboolean shadows, jfloatArray evidence_array,
        jintArray half_array, jintArray quarter_array, jintArray eighth_array,
        jint full_width, jint full_height, jintArray policy_array, jint mode) {
    (void)cls;
    Region input = {0}, maps[3] = {{0}, {0}, {0}};
    Cancellation cancellation = {0};
    jint *policy = NULL, *result = NULL;
    float evidence[16];
    jboolean success = JNI_FALSE;
    int64_t source_count = (int64_t)width * rows;
    int64_t count = (int64_t)width * ((int64_t)end - begin);
    int64_t absolute_valid_begin = (int64_t)origin_y + valid_begin;
    int64_t absolute_valid_end = (int64_t)origin_y + valid_end;
    int64_t absolute_begin = (int64_t)origin_y + begin;
    int64_t absolute_end = (int64_t)origin_y + end;
    if (!input_array || !output_array || !evidence_array ||
        (*env)->IsSameObject(env, input_array, output_array) ||
        width < 1 || width > INT_MAX - 18 || rows < 1 || rows > INT_MAX - 18 ||
        source_count > INT_MAX || count < 0 || count > INT_MAX ||
        (uint64_t)count > SIZE_MAX / sizeof(jint) ||
        begin < valid_begin || end > valid_end || begin > end || valid_begin < 0 ||
        valid_end > rows || valid_begin >= valid_end ||
        noise < 0 || noise > 4 || mode < 0 || mode > 3 ||
        source_count > (*env)->GetArrayLength(env, input_array) ||
        source_count > (*env)->GetArrayLength(env, output_array) ||
        (*env)->GetArrayLength(env, evidence_array) < 16) return JNI_FALSE;
    if (mode == 3) {
        if (full_width != width || full_height < 1 || full_height > INT_MAX - 18 ||
            (int64_t)full_width * full_height > INT_MAX || absolute_valid_begin < 0 ||
            absolute_valid_end > full_height || end - begin > 256 ||
            (absolute_begin > 0 && begin - valid_begin < mini(18, (int)absolute_begin)) ||
            (absolute_end < full_height && valid_end - end < mini(18, full_height - (int)absolute_end)) ||
            !half_array || !quarter_array || !eighth_array ||
            (*env)->IsSameObject(env, output_array, half_array) ||
            (*env)->IsSameObject(env, output_array, quarter_array) ||
            (*env)->IsSameObject(env, output_array, eighth_array)) return JNI_FALSE;
    } else if (origin_y != 0 || valid_begin != 0 || valid_end != rows ||
            full_width != width || full_height != rows || end - begin > 128) return JNI_FALSE;
    if (policy_array && (count > INT_MAX / 2 || count * 2 > (*env)->GetArrayLength(env, policy_array) ||
            (*env)->IsSameObject(env, output_array, policy_array))) return JNI_FALSE;
    (*env)->GetFloatArrayRegion(env, evidence_array, 0, 16, evidence);
    if ((*env)->ExceptionCheck(env)) goto cleanup;
    for (int i = 0; i < 16; i++) if (!isfinite(evidence[i]) || evidence[i] < 0 || evidence[i] > 1000000.f) goto cleanup;
    if (!cancellation_start(env, &cancellation) || cancelled(env, &cancellation)) goto cleanup;
    if (begin == end) { success = JNI_TRUE; goto cleanup; }
    int radius = mode == 1 ? 7 : 2;
    if (!copy_region(env, input_array, width, rows, maxi(valid_begin, begin - radius),
            mini(valid_end, end + radius), &input)) goto cleanup;
    if (mode == 3) {
        jintArray arrays[3] = {half_array, quarter_array, eighth_array};
        int map_width = full_width, map_height = full_height;
        for (int k = 0; k < 3; k++) {
            map_width = ceil_div2(map_width); map_height = ceil_div2(map_height);
            int scale = 1 << (k + 1);
            float first_y = clampf(((int)absolute_begin + .5f) / scale - .5f, 0, map_height - 1);
            float last_y = clampf(((int)absolute_end - 1 + .5f) / scale - .5f, 0, map_height - 1);
            if (!copy_region(env, arrays[k], map_width, map_height, (int)first_y,
                    mini(map_height, (int)last_y + 2), &maps[k])) goto cleanup;
        }
        if (policy_array) {
            policy = malloc((size_t)count * 2 * sizeof(jint));
            if (!policy) goto cleanup;
            (*env)->GetIntArrayRegion(env, policy_array, 0, (jsize)(count * 2), policy);
            if ((*env)->ExceptionCheck(env)) goto cleanup;
        }
    }
    result = malloc((size_t)count * sizeof(jint));
    if (!result) goto cleanup;
    for (int y = begin; y < end; y++) {
        if ((y - begin) % 16 == 0 && cancelled(env, &cancellation)) goto cleanup;
        for (int x = 0; x < width; x++) {
            size_t at = (size_t)(y - begin) * width + x;
            if (mode == 3) {
                if (noise == 0) result[at] = (jint)pixel(&input, x, y);
                else process_pixel(&input, maps, x, y, valid_begin, valid_end, origin_y, noise,
                    shadows, evidence, policy ? policy + at * 2 : NULL, result + at);
            } else prepare_pixel(&input, x, y, noise, shadows, evidence, mode, result + at);
        }
    }
    if (cancelled(env, &cancellation)) goto cleanup;
    (*env)->SetIntArrayRegion(env, output_array, begin * width, (jsize)count, result);
    success = (*env)->ExceptionCheck(env) ? JNI_FALSE : JNI_TRUE;
cleanup:
    free(input.pixels);
    for (int k = 0; k < 3; k++) free(maps[k].pixels);
    free(policy); free(result);
    if (cancellation.thread) (*env)->DeleteLocalRef(env, cancellation.thread);
    return success;
}
