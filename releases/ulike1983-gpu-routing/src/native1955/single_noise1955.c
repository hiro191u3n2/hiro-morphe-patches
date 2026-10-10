/* Single-frame NR1/NR2/NR3 backend. The Java implementation is the oracle.
 * Build with -ffp-contract=off and without fast-math: float accumulation order
 * is deliberate. Source is immutable and no Java output is touched until the
 * complete requested range has succeeded. No other frame is read here.
 */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>
#include <math.h>
#ifndef COLOUR_CACHE_CPU_INCLUDED1976
#include <pthread.h>
#endif
#if defined(__aarch64__)
#include <arm_neon.h>
#endif

#define BATCH_ROWS 64
#define BINS 8
#define BANDS 3
#define HALO 7
#define MODEL_EXTRA (BINS * 2 + BINS * 3 * BANDS)

static const float window8[8] = {
    .03806023f, .30865828f, .6913417f, .96193975f,
    .96193975f, .6913417f, .30865828f, .03806023f
};

/* The transform only uses the first n/2 symmetric basis values. */
static const double d4[4][4] = {
    {.5, .5, 0, 0},
    {.65328148243818829, .27059805007309856, 0, 0},
    {.50000000000000011, -.5, 0, 0},
    {.27059805007309856, -.6532814824381884, 0, 0}
};
static const double d8[8][4] = {
    {.35355339059327379, .35355339059327379, .35355339059327379, .35355339059327379},
    {.49039264020161522, .41573480615127262, .27778511650980114, .097545161008064166},
    {.46193976625564337, .19134171618254492, -.19134171618254486, -.46193976625564337},
    {.41573480615127262, -.097545161008064096, -.49039264020161522, -.27778511650980109},
    {.35355339059327379, -.35355339059327373, -.35355339059327384, .35355339059327368},
    {.27778511650980114, -.49039264020161522, .097545161008064152, .41573480615127273},
    {.19134171618254492, -.46193976625564342, .46193976625564326, -.19134171618254495},
    {.097545161008064166, -.27778511650980109, .41573480615127273, -.49039264020161533}
};

typedef struct {
    int width, height, columns, rows;
    const float *luma, *chroma, *mean, *brightness_y, *brightness_c, *ratios;
    float step_x, step_y, left, top, global_y, global_c;
} Model;

/* Each invocation owns a fixed, bounded eight-row colour window. A sixteen
 * column ring retains the overlap between neighbouring 8x8 and 4x4 blocks;
 * no image-sized allocation, retained pointer, or cross-worker state is used. */
#define COLOR_ROWS1976 8
#define COLOR_COLUMNS1976 16
typedef struct {
    double y, red, blue;
    uint32_t pixel;
} ColorSample1976;
typedef struct {
    ColorSample1976 samples[COLOR_COLUMNS1976];
    int row, first, finish, valid;
} ColorRow1976;
static _Thread_local int colour_enabled1976;
typedef struct {
    float *delta, *weight;
    int colour_enabled1976;
    double source[3][64], coefficient[3][64], temp[64], residual[64];
    float sigma[3], variance[9];
    ColorRow1976 colours[COLOR_ROWS1976];
} Workspace;
_Static_assert(sizeof(ColorRow1976)*COLOR_ROWS1976 <= 4352,
    "The exact colour reuse window must stay bounded on the worker stack");

#ifndef COLOUR_CACHE_CPU_INCLUDED1976
/* A Java worker owns one generation-checked handle until its shot drains. No
 * input, JNI array pointer, model or colour-row validity survives a call. A
 * foreign release only retires an active lease; its owner ends it before free. */
typedef struct {
    uint64_t id;size_t bytes;int width,rows,busy,retired;
    jint *result;float *delta,*weight;
} OwnedWorkspace1978;
static pthread_mutex_t owned_mutex1978=PTHREAD_MUTEX_INITIALIZER;
static OwnedWorkspace1978 owned1978[4];
static uint64_t next_owned1978=1;
static size_t owned_bytes1978;
static _Thread_local OwnedWorkspace1978 *active_owned1978;
static void owned_free1978(OwnedWorkspace1978 *s) {
    free(s->result);free(s->delta);free(s->weight);
    owned_bytes1978-=s->bytes;memset(s,0,sizeof(*s));
}
JNIEXPORT jlong JNICALL Java_com_hiro_ulike_CpuSingle1978_createWorkspaceNative(
        JNIEnv *env,jclass cls,jint width,jint rows) {
    (void)env;(void)cls;
    if(width<1||rows<1||rows>256)return 0;
    uint64_t count=(uint64_t)width*(uint32_t)rows;
    uint64_t work=(uint64_t)width*(uint32_t)(rows<64?rows:64);
    uint64_t bytes=count*4+work*16+sizeof(OwnedWorkspace1978);
    if(count>INT_MAX||bytes>32U*1024U*1024U||bytes>SIZE_MAX)return 0;
    pthread_mutex_lock(&owned_mutex1978);
    OwnedWorkspace1978 *s=NULL;
    for(int i=0;i<4;i++)if(!owned1978[i].id){s=&owned1978[i];break;}
    if(!s||bytes>64U*1024U*1024U-owned_bytes1978||next_owned1978>INT64_MAX) {
        pthread_mutex_unlock(&owned_mutex1978);return 0;
    }
    s->bytes=(size_t)bytes;owned_bytes1978+=s->bytes;
    s->result=malloc((size_t)count*sizeof(jint));
    s->delta=malloc((size_t)work*3*sizeof(float));
    s->weight=malloc((size_t)work*sizeof(float));
    if(!s->result||!s->delta||!s->weight){owned_free1978(s);pthread_mutex_unlock(&owned_mutex1978);return 0;}
    s->width=width;s->rows=rows;s->id=next_owned1978++;
    jlong id=(jlong)s->id;pthread_mutex_unlock(&owned_mutex1978);return id;
}
JNIEXPORT void JNICALL Java_com_hiro_ulike_CpuSingle1978_releaseWorkspaceNative(
        JNIEnv *env,jclass cls,jlong handle) {
    (void)env;(void)cls;if(handle<=0)return;
    pthread_mutex_lock(&owned_mutex1978);
    for(int i=0;i<4;i++)if(owned1978[i].id==(uint64_t)handle) {
        if(owned1978[i].busy)owned1978[i].retired=1;else owned_free1978(&owned1978[i]);break;
    }
    pthread_mutex_unlock(&owned_mutex1978);
}
JNIEXPORT jboolean JNICALL Java_com_hiro_ulike_CpuSingle1978_beginWorkspaceNative(
        JNIEnv *env,jclass cls,jlong handle) {
    (void)env;(void)cls;if(handle<=0||active_owned1978)return JNI_FALSE;
    pthread_mutex_lock(&owned_mutex1978);jboolean result=JNI_FALSE;
    for(int i=0;i<4;i++)if(owned1978[i].id==(uint64_t)handle&&!owned1978[i].busy&&!owned1978[i].retired) {
        owned1978[i].busy=1;active_owned1978=&owned1978[i];result=JNI_TRUE;break;
    }
    pthread_mutex_unlock(&owned_mutex1978);return result;
}
JNIEXPORT void JNICALL Java_com_hiro_ulike_CpuSingle1978_endWorkspaceNative(
        JNIEnv *env,jclass cls,jlong handle) {
    (void)env;(void)cls;
    if(!active_owned1978||handle<=0||active_owned1978->id!=(uint64_t)handle)return;
    pthread_mutex_lock(&owned_mutex1978);
    OwnedWorkspace1978 *s=active_owned1978;active_owned1978=NULL;s->busy=0;
    if(s->retired)owned_free1978(s);
    pthread_mutex_unlock(&owned_mutex1978);
}
#endif

static int maxi(int a, int b) { return a > b ? a : b; }
static int mini(int a, int b) { return a < b ? a : b; }
static double maxd(double a, double b) { return a > b ? a : b; }
static float maxf(float a, float b) { return a > b ? a : b; }
static float minf(float a, float b) { return a < b ? a : b; }
static float clampf(float a, float lo, float hi) { return maxf(lo, minf(hi, a)); }
static int floor_div4(int a) { int q = a / 4; return a < 0 && a % 4 != 0 ? q - 1 : q; }
static double luma(uint32_t p) {
    return .299 * ((p >> 16) & 255) + .587 * ((p >> 8) & 255) + .114 * (p & 255);
}
/* Preserve the binary64 colour conversion exactly. Mean and transform
 * reductions remain in the original block visit order below. Clamp before
 * caching, so border replication has the same sample values as the oracle. */
static ColorRow1976 *colour_row1976(Workspace *w, const jint *input, int width,
        int row, int first, int finish) {
    ColorRow1976 *cached = &w->colours[row & (COLOR_ROWS1976-1)];
    if (!cached->valid || cached->row != row || first < cached->first ||
            first > cached->finish) {
        cached->row = row;
        cached->first = cached->finish = first;
        cached->valid = 1;
    }
    for (int x = cached->finish; x < finish; x++) {
        uint32_t p = (uint32_t)input[(size_t)row*width+x];
        ColorSample1976 *sample = &cached->samples[x & (COLOR_COLUMNS1976-1)];
        double y = luma(p);
        sample->y = y;
        sample->red = ((p >> 16)&255)-y;
        sample->blue = (p&255)-y;
        sample->pixel = p;
    }
    if (finish > cached->finish) cached->finish = finish;
    if (cached->finish-cached->first > COLOR_COLUMNS1976)
        cached->first = cached->finish-COLOR_COLUMNS1976;
    return cached;
}
static int source_block1976(const jint *input, int width, int valid_begin,
        int valid_end, int origin_y, int height, int bx, int by, int n,
        Workspace *w, double *mean, int *opaque) {
    if (!w->colour_enabled1976) {
    for (int yy = 0; yy < n; yy++) for (int xx = 0; xx < n; xx++) {
        int gx = maxi(0, mini(width-1, bx+xx));
        int gy = maxi(0, mini(height-1, by+yy))-origin_y;
        if (gy < valid_begin || gy >= valid_end) return 0;
        uint32_t p = (uint32_t)input[(size_t)gy*width+gx];
        int at = yy*n+xx; double y = luma(p);
        if ((p >> 24) != 255) *opaque = 0;
        w->source[0][at] = w->coefficient[0][at] = y;
        w->source[1][at] = w->coefficient[1][at] = ((p >> 16)&255)-y;
        w->source[2][at] = w->coefficient[2][at] = (p&255)-y;
        *mean += y;
    }
        return 1;
    }
    int first = maxi(0, mini(width-1, bx));
    int finish = maxi(0, mini(width-1, bx+n-1))+1;
    for (int yy = 0; yy < n; yy++) {
        int gy = maxi(0, mini(height-1, by+yy))-origin_y;
        if (gy < valid_begin || gy >= valid_end) return 0;
        ColorRow1976 *row = colour_row1976(w,input,width,gy,first,finish);
        for (int xx = 0; xx < n; xx++) {
            int gx = maxi(0, mini(width-1, bx+xx));
            const ColorSample1976 *sample = &row->samples[gx & (COLOR_COLUMNS1976-1)];
            int at = yy*n+xx;
            if ((sample->pixel >> 24) != 255) *opaque = 0;
            w->source[0][at] = w->coefficient[0][at] = sample->y;
            w->source[1][at] = w->coefficient[1][at] = sample->red;
            w->source[2][at] = w->coefficient[2][at] = sample->blue;
            *mean += sample->y;
        }
    }
    return 1;
}
static int byte_value(double value) {
    return maxi(0, mini(255, (int)floor(value + .5)));
}

/* H28 processes two independent transform outputs in double lanes. Each lane
 * visits the original x/y/k terms in the original order. Explicit multiply then
 * add plus -ffp-contract=off forbid FMA and reduction reassociation. */
#if defined(__aarch64__)
static void transform_neon1956(double *values, double *temp, int n, int inverse) {
    const double (*basis)[4] = n == 8 ? d8 : d4;
    if (!inverse) {
        for (int row=0;row<n;row++) for (int k=0;k<n;k+=2) {
            float64x2_t sum=vdupq_n_f64(0.0);
            for (int x=0;x<n/2;x++) {
                double left=values[row*n+x],right=values[row*n+n-1-x];
                float64x2_t paired={left+right,left+(-right)};
                float64x2_t coefficient={basis[k][x],basis[k+1][x]};
                sum=vaddq_f64(sum,vmulq_f64(paired,coefficient));
            }
            vst1q_f64(temp+row*n+k,sum);
        }
        for (int col=0;col<n;col+=2) for (int k=0;k<n;k++) {
            float64x2_t sum=vdupq_n_f64(0.0);
            for (int y=0;y<n/2;y++) {
                float64x2_t left=vld1q_f64(temp+y*n+col);
                float64x2_t right=vld1q_f64(temp+(n-1-y)*n+col);
                if(k&1)right=vnegq_f64(right);
                float64x2_t paired=vaddq_f64(left,right);
                sum=vaddq_f64(sum,vmulq_n_f64(paired,basis[k][y]));
            }
            vst1q_f64(values+k*n+col,sum);
        }
    } else {
        for (int col=0;col<n;col+=2) for (int y=0;y<n/2;y++) {
            float64x2_t even=vdupq_n_f64(0.0),odd=vdupq_n_f64(0.0);
            for(int k=0;k<n;k++) {
                float64x2_t v=vmulq_n_f64(vld1q_f64(values+k*n+col),basis[k][y]);
                if(k&1)odd=vaddq_f64(odd,v);else even=vaddq_f64(even,v);
            }
            vst1q_f64(temp+y*n+col,vaddq_f64(even,odd));
            vst1q_f64(temp+(n-1-y)*n+col,vsubq_f64(even,odd));
        }
        for (int row=0;row<n;row++) for(int x=0;x<n/2;x+=2) {
            float64x2_t even=vdupq_n_f64(0.0),odd=vdupq_n_f64(0.0);
            for(int k=0;k<n;k++) {
                float64x2_t coefficient={basis[k][x],basis[k][x+1]};
                float64x2_t v=vmulq_n_f64(coefficient,temp[row*n+k]);
                if(k&1)odd=vaddq_f64(odd,v);else even=vaddq_f64(even,v);
            }
            double a[2],b[2];vst1q_f64(a,vaddq_f64(even,odd));vst1q_f64(b,vsubq_f64(even,odd));
            values[row*n+x]=a[0];values[row*n+x+1]=a[1];
            values[row*n+n-1-x]=b[0];values[row*n+n-2-x]=b[1];
        }
    }
}
#endif

static void transform(double *values, double *temp, int n, int inverse) {
#if defined(__aarch64__)
    transform_neon1956(values,temp,n,inverse);return;
#endif
    const double (*basis)[4] = n == 8 ? d8 : d4;
    if (!inverse) {
        for (int row = 0; row < n; row++) for (int k = 0; k < n; k++) {
            double sum = 0;
            for (int x = 0; x < n / 2; x++)
                sum += (values[row*n+x] + ((k & 1) == 0 ? values[row*n+n-1-x] : -values[row*n+n-1-x])) * basis[k][x];
            temp[row*n+k] = sum;
        }
        for (int col = 0; col < n; col++) for (int k = 0; k < n; k++) {
            double sum = 0;
            for (int y = 0; y < n / 2; y++)
                sum += (temp[y*n+col] + ((k & 1) == 0 ? temp[(n-1-y)*n+col] : -temp[(n-1-y)*n+col])) * basis[k][y];
            values[k*n+col] = sum;
        }
    } else {
        for (int col = 0; col < n; col++) for (int y = 0; y < n / 2; y++) {
            double even = 0, odd = 0;
            for (int k = 0; k < n; k++) {
                double v = values[k*n+col] * basis[k][y];
                if ((k & 1) == 0) even += v; else odd += v;
            }
            temp[y*n+col] = even + odd;
            temp[(n-1-y)*n+col] = even - odd;
        }
        for (int row = 0; row < n; row++) for (int x = 0; x < n / 2; x++) {
            double even = 0, odd = 0;
            for (int k = 0; k < n; k++) {
                double v = temp[row*n+k] * basis[k][x];
                if ((k & 1) == 0) even += v; else odd += v;
            }
            values[row*n+x] = even + odd;
            values[row*n+n-1-x] = even - odd;
        }
    }
}

static float interpolate(const Model *m, const float *values, float x, float y) {
    float xx = m->columns == 1 ? 0 : clampf((x-m->left)/m->step_x, 0, m->columns-1);
    float yy = m->rows == 1 ? 0 : clampf((y-m->top)/m->step_y, 0, m->rows-1);
    int ix = (int)xx, iy = (int)yy;
    int nx = mini(m->columns-1, ix+1), ny = mini(m->rows-1, iy+1);
    float fx = xx-ix, fy = yy-iy;
    float a = values[iy*m->columns+ix]*(1-fx) + values[iy*m->columns+nx]*fx;
    float b = values[ny*m->columns+ix]*(1-fx) + values[ny*m->columns+nx]*fx;
    return a*(1-fy) + b*fy;
}
static float brightness(const float *values, float mean) {
    float x = clampf((mean-16)/32, 0, BINS-1);
    int a = (int)x, b = mini(BINS-1, a+1);
    return values[a]*(1-(x-a)) + values[b]*(x-a);
}
static float model_sigma(const Model *m, int x, int y, float block_mean, int color) {
    float base = interpolate(m, color ? m->chroma : m->luma, x, y);
    if (base < .30f) return 0;
    float sampled = interpolate(m, m->mean, x, y);
    const float *bins = color ? m->brightness_c : m->brightness_y;
    float reference = brightness(bins, sampled), current = brightness(bins, block_mean);
    return base * clampf(reference > .3f ? current/reference : 1.f, .65f, 1.55f);
}
static float spectral(const Model *m, int plane, int band, float block_mean) {
    int bin = maxi(0, mini(BINS-1, (int)block_mean/32));
    return m->ratios[(bin*3+plane)*BANDS+band];
}

static double lag(const double *values, int n, int distance) {
    double numerator = 0, a = 0, b = 0;
    for (int y = 0; y < n; y++) for (int x = 0; x < n-distance; x++) {
        double p = values[y*n+x], q = values[y*n+x+distance];
        numerator += p*q; a += p*p; b += q*q;
    }
    for (int y = 0; y < n-distance; y++) for (int x = 0; x < n; x++) {
        double p = values[y*n+x], q = values[(y+distance)*n+x];
        numerator += p*q; a += p*p; b += q*q;
    }
    return a*b < 1e-9 ? 0 : numerator/sqrt(a*b);
}
static float residual_safeguard(const double *source, const double *residual, int n, float sigma) {
    double energy = 0;
    for (int i = 0; i < n*n; i++) energy += residual[i]*residual[i];
    if (energy < .0001) return 1;
    double r1 = lag(residual, n, 1), r2 = lag(residual, n, 2), r4 = n == 8 ? lag(residual, n, 4) : 0;
    double periodic = maxd(r2 > .25 && r1 < -.15 ? r2 : 0, r4 > .25 && r2 < -.15 ? r4 : 0);
    float keep = 1.f - clampf((float)(periodic-.25)/.5f, 0, 1)*.88f;
    for (int vertical = 0; vertical < 2; vertical++) {
        double coherent = 0, removed = 0; int count = 0;
        for (int k = 0; k < n; k++) {
            double a = 0, b = 0, ra = 0, rb = 0;
            for (int j = 0; j < n/2; j++) {
                int ia = vertical == 0 ? k*n+j : j*n+k;
                int ib = vertical == 0 ? k*n+j+n/2 : (j+n/2)*n+k;
                a += source[ia]; b += source[ib]; ra += residual[ia]; rb += residual[ib];
            }
            double edge = (b-a)/(n/2), lost = (rb-ra)/(n/2);
            if (fabs(edge) > maxd(3, sigma*1.4)) {
                coherent += edge; removed += lost; count++;
            }
        }
        if (count >= n/2 && fabs(coherent) > maxf(8, sigma*n) &&
            removed*coherent > 0 && fabs(removed) > fabs(coherent)*.015)
            keep = minf(keep, .18f);
    }
    return keep;
}

static int block(const jint *input, int width, int valid_begin, int valid_end,
        int origin_y, const Model *m, const jint *protection, int protection_begin,
        int noise, int shadows, int bx, int by, int n, int start, int finish, Workspace *w) {
    int absolute_start = start+origin_y, absolute_end = finish+origin_y;
    if (by+n <= absolute_start || by >= absolute_end || bx+n <= 0) return 1;
    double mean = 0; int opaque = 1;
    if (!source_block1976(input,width,valid_begin,valid_end,origin_y,m->height,
            bx,by,n,w,&mean,&opaque)) return 0;
    if (!opaque) return 1;
    mean /= n*n;
    int cx = maxi(0, mini(width-1, bx+n/2)), cy = maxi(0, mini(m->height-1, by+n/2));
    float sy = model_sigma(m, cx, cy, (float)mean, 0), sc = model_sigma(m, cx, cy, (float)mean, 1);
    if (sy < .30f && sc < .30f) return 1;
    float strength = .35f + noise*.21f;
    if (shadows) strength *= 1.f + .12f*clampf((128-(float)mean)/96, 0, 1);
    w->sigma[0] = sy; w->sigma[1] = w->sigma[2] = sc;
    for (int plane = 0; plane < 3; plane++) {
        for (int band = 0; band < BANDS; band++) {
            float shape = spectral(m, plane, band, (float)mean);
            w->variance[plane*BANDS+band] = w->sigma[plane]*w->sigma[plane]*shape*shape*strength;
        }
        transform(w->coefficient[plane], w->temp, n, 0);
        for (int v = 0; v < n; v++) for (int u = 0; u < n; u++) {
            if (u+v == 0) continue; /* Keep DC at both scales. */
            int at = v*n+u; double coefficient = w->coefficient[plane][at];
            int frequency = (u+v)*8/n, band = frequency < 4 ? 0 : frequency < 8 ? 1 : 2;
            float variance = w->variance[plane*BANDS+band];
            if (plane == 0 && u+v == 1) variance *= .32f;
            double power = coefficient*coefficient;
            if (variance <= .03f || power > variance*36) continue;
            double gain = power/(power+variance*1.25);
            w->coefficient[plane][at] = coefficient*gain;
        }
        transform(w->coefficient[plane], w->temp, n, 1);
    }
    for (int i = 0; i < n*n; i++) w->residual[i] = w->source[0][i]-w->coefficient[0][i];
    float safeguard = residual_safeguard(w->source[0], w->residual, n, sy);
    for (int yy = 0; yy < n; yy++) {
        int ay = by+yy;
        if (ay < absolute_start || ay >= absolute_end || ay < 0 || ay >= m->height) continue;
        for (int xx = 0; xx < n; xx++) {
            int ax = bx+xx;
            if (ax < 0 || ax >= width) continue;
            int at = yy*n+xx;
            size_t local = (size_t)(ay-origin_y-start)*width+ax;
            float blend = safeguard;
            if (protection) {
                size_t pi = ((size_t)(ay-origin_y-protection_begin)*width+ax)*2;
                int budget = maxi(0, mini(256, protection[pi]));
                int detail = maxi(0, mini(256, protection[pi+1]));
                blend *= budget/256.f*(1-detail/256.f);
            }
            float weight = n == 8 ? .82f*window8[xx]*window8[yy] : .18f;
            w->weight[local] += weight;
            for (int plane = 0; plane < 3; plane++)
                w->delta[local*3+plane] += (float)(w->coefficient[plane][at]-w->source[plane][at])*weight*blend;
        }
    }
    return 1;
}

JNIEXPORT jint JNICALL Java_com_hiro_ulike_SingleNoise1955_nativeAbi(JNIEnv *env, jclass cls) {
    (void)env; (void)cls; return 1955;
}

JNIEXPORT jboolean JNICALL Java_com_hiro_ulike_SingleNoise1955_processNative(
        JNIEnv *env, jclass cls, jintArray input_array, jintArray output_array,
        jint width, jint rows, jint begin, jint end, jint valid_begin, jint valid_end,
        jint origin_y, jint noise, jboolean shadows, jint model_width, jint model_height,
        jint columns, jint model_rows, jint patch_w, jint patch_h,
        jfloatArray model_array, jintArray protection_array) {
    (void)cls;
    jint *input = NULL, *protection = NULL, *result = NULL;
    jfloat *data = NULL;
    Workspace w = {0};
    int reused_workspace1978=0;
    w.colour_enabled1976 = colour_enabled1976;
    jboolean success = JNI_FALSE;
    int64_t source_count = (int64_t)width*rows;
    int64_t count = (int64_t)width*((int64_t)end-begin);
    int64_t cells = (int64_t)columns*model_rows;
    int64_t valid_y0 = (int64_t)origin_y+valid_begin, valid_y1 = (int64_t)origin_y+valid_end;
    int64_t start_y = (int64_t)origin_y+begin, finish_y = (int64_t)origin_y+end;
    if (!input_array || !output_array || !model_array ||
        (*env)->IsSameObject(env, input_array, output_array) ||
        width < 1 || rows < 1 || model_width != width || model_height < 1 ||
        model_height > INT_MAX-8 || width > INT_MAX-8 ||
        columns < 1 || model_rows < 1 || cells > (INT_MAX-MODEL_EXTRA)/3 ||
        patch_w < 1 || patch_h < 1 || patch_w > width || patch_h > model_height ||
        (columns > 1 && patch_w == width) || (model_rows > 1 && patch_h == model_height) ||
        noise < 0 || noise > 4 || valid_begin < 0 || valid_end > rows ||
        begin < valid_begin || end < begin || end > valid_end ||
        valid_y0 < 0 || valid_y1 > model_height || source_count > INT_MAX ||
        count < 0 || count > INT_MAX ||
        source_count > (*env)->GetArrayLength(env, input_array) ||
        source_count > (*env)->GetArrayLength(env, output_array) ||
        cells*3+MODEL_EXTRA > (*env)->GetArrayLength(env, model_array)) return JNI_FALSE;
    if (protection_array && (count > INT_MAX/2 || count*2 > (*env)->GetArrayLength(env, protection_array)))
        return JNI_FALSE;
    if (noise > 0 && begin < end &&
        (valid_y0 > (start_y-HALO > 0 ? start_y-HALO : 0) ||
         valid_y1 < (finish_y+HALO < model_height ? finish_y+HALO : model_height))) return JNI_FALSE;
    if (begin == end) return JNI_TRUE;
    if ((uint64_t)count > SIZE_MAX/sizeof(jint)) return JNI_FALSE;
    input = (*env)->GetIntArrayElements(env, input_array, NULL);
    if (!input) goto cleanup;
    data = (*env)->GetFloatArrayElements(env, model_array, NULL);
    if (!data) goto cleanup;
    /* Defend the backend from malformed foreign models, including NaNs. */
    for (int64_t i = 0; i < cells*3+MODEL_EXTRA; i++)
        if (!isfinite(data[i]) || data[i] < 0 || data[i] > 1000000.f) goto cleanup;
    Model m = {0};
    m.width = model_width; m.height = model_height; m.columns = columns; m.rows = model_rows;
    m.luma = data; m.chroma = data+cells; m.mean = data+cells*2;
    m.brightness_y = data+cells*3; m.brightness_c = m.brightness_y+BINS;
    m.ratios = m.brightness_c+BINS;
    m.left = (patch_w-1)*.5f; m.top = (patch_h-1)*.5f;
    m.step_x = columns == 1 ? 1 : (float)(model_width-patch_w)/(columns-1);
    m.step_y = model_rows == 1 ? 1 : (float)(model_height-patch_h)/(model_rows-1);
    float sy = 0, sc = 0;
    for (int64_t i = 0; i < cells; i++) { sy += m.luma[i]; sc += m.chroma[i]; }
    m.global_y = sy/(int)cells; m.global_c = sc/(int)cells;
    if (noise == 0 || (m.global_y < .30f && m.global_c < .30f)) {
        (*env)->SetIntArrayRegion(env, output_array, begin*width, (jsize)count, input+(size_t)begin*width);
        success = (*env)->ExceptionCheck(env) ? JNI_FALSE : JNI_TRUE;
        goto cleanup;
    }
    if (protection_array) {
        protection = (*env)->GetIntArrayElements(env, protection_array, NULL);
        if (!protection) goto cleanup;
    }
    size_t work_count = (size_t)width*mini(BATCH_ROWS, end-begin);
    if(work_count > SIZE_MAX/(3*sizeof(float)))goto cleanup;
#ifndef COLOUR_CACHE_CPU_INCLUDED1976
    if(active_owned1978) {
        if(active_owned1978->width!=width||active_owned1978->rows<end-begin)goto cleanup;
        result=active_owned1978->result;w.delta=active_owned1978->delta;w.weight=active_owned1978->weight;reused_workspace1978=1;
    } else
#endif
    {
        result = malloc((size_t)count*sizeof(jint));
        if(!result)goto cleanup;
        w.delta = calloc(work_count*3, sizeof(float));
        w.weight = calloc(work_count, sizeof(float));
    }
    if(!result)goto cleanup;
    if (!w.delta || !w.weight) goto cleanup;
    for (int start = begin; start < end;) {
        int finish = end-start > BATCH_ROWS ? start+BATCH_ROWS : end;
        size_t batch_count = (size_t)(finish-start)*width;
        memset(w.delta, 0, batch_count*3*sizeof(float));
        memset(w.weight, 0, batch_count*sizeof(float));
        int absolute_start = start+origin_y, absolute_end = finish+origin_y;
        int first = floor_div4(absolute_start-7)*4;
        for (int by = first; by < absolute_end; by += 4) {
            for (int bx = -4; bx < width; bx += 4) {
                if (!block(input, width, valid_begin, valid_end, origin_y, &m, protection,
                    begin, noise, shadows, bx, by, 8, start, finish, &w)) goto cleanup;
                if (by+4 > absolute_start && by < absolute_end && bx >= 0 &&
                    !block(input, width, valid_begin, valid_end, origin_y, &m, protection,
                    begin, noise, shadows, bx, by, 4, start, finish, &w)) goto cleanup;
            }
        }
        for (int row = start; row < finish; row++) for (int col = 0; col < width; col++) {
            size_t at = (size_t)row*width+col, local = (size_t)(row-start)*width+col;
            size_t destination = (size_t)(row-begin)*width+col;
            uint32_t p = (uint32_t)input[at];
            if ((p >> 24) != 255 || w.weight[local] == 0) { result[destination] = (jint)p; continue; }
            float denom = w.weight[local], dy = w.delta[local*3]/denom;
            float dr = w.delta[local*3+1]/denom, db = w.delta[local*3+2]/denom;
            int r = byte_value(((p >> 16)&255)+dy+dr), b = byte_value((p&255)+dy+db);
            int g = byte_value(((p >> 8)&255)+dy-(.299f*dr+.114f*db)/.587f);
            result[destination] = (jint)((p&UINT32_C(0xff000000)) | (uint32_t)(r<<16) | (uint32_t)(g<<8) | (uint32_t)b);
        }
        start = finish;
    }
    (*env)->SetIntArrayRegion(env, output_array, begin*width, (jsize)count, result);
    success = (*env)->ExceptionCheck(env) ? JNI_FALSE : JNI_TRUE;
cleanup:
    if(!reused_workspace1978){free(w.delta); free(w.weight); free(result);}
    if (protection) (*env)->ReleaseIntArrayElements(env, protection_array, protection, JNI_ABORT);
    if (data) (*env)->ReleaseFloatArrayElements(env, model_array, data, JNI_ABORT);
    if (input) (*env)->ReleaseIntArrayElements(env, input_array, input, JNI_ABORT);
    return success;
}

#ifndef COLOUR_CACHE_CPU_INCLUDED1976
/* Mode is scoped to this JNI call and restored even on a failed transaction. */
JNIEXPORT jboolean JNICALL Java_com_hiro_ulike_ColourCache1976_cpuNative(
        JNIEnv *env, jclass cls, jintArray input_array, jintArray output_array,
        jint width, jint rows, jint begin, jint end, jint valid_begin, jint valid_end,
        jint origin_y, jint noise, jboolean shadows, jint model_width, jint model_height,
        jint columns, jint model_rows, jint patch_w, jint patch_h,
        jfloatArray model_array, jintArray protection_array, jboolean cached) {
    int previous=colour_enabled1976;colour_enabled1976=cached?1:0;
    jboolean result=Java_com_hiro_ulike_SingleNoise1955_processNative(env,cls,
        input_array,output_array,width,rows,begin,end,valid_begin,valid_end,origin_y,
        noise,shadows,model_width,model_height,columns,model_rows,patch_w,patch_h,
        model_array,protection_array);
    colour_enabled1976=previous;return result;
}
#endif
