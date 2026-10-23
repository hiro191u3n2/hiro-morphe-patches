/* NR9–NR13 single-image range backend. StrongNoise1958.java is the oracle.
 * All array reads are bounded row regions. Requested output is transactional:
 * errors or cancellation leave the caller's array untouched.
 */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>
#include <math.h>
#include <pthread.h>
#if defined(__aarch64__)
#include <arm_neon.h>
#endif
#include "scratch1959.h"

typedef struct { float y, r, b; } Color;
typedef struct {
    jint *pixels;
    Color *colors;
    int color_rows[16];
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
        int first, int finish, Region *r, Scratch1959 *scratch, int block) {
    int64_t source_count = (int64_t)width * height;
    int64_t count = (int64_t)width * (finish - first);
    if (!array || width < 1 || height < 1 || first < 0 || finish < first || finish > height ||
        source_count > INT_MAX || count > INT_MAX || (uint64_t)count > SIZE_MAX / sizeof(jint) ||
        source_count > (*env)->GetArrayLength(env, array)) return 0;
    r->width = width; r->height = height; r->first = first; r->finish = finish;
    if (count == 0) return 1;
    r->pixels = scratch_reserve1959(scratch, block, (size_t)count * sizeof(jint));
    if (!r->pixels) return 0;
    (*env)->GetIntArrayRegion(env, array, first * width, (jsize)count, r->pixels);
    return !(*env)->ExceptionCheck(env);
}
static uint32_t pixel(const Region *r, int x, int y) {
    x = maxi(0, mini(r->width - 1, x));
    y = maxi(0, mini(r->height - 1, y));
    return (uint32_t)r->pixels[(size_t)(y - r->first) * r->width + x];
}

/* Sixteen immutable source rows cover the largest 15-row NLM support.
 * A row is transformed once as the window advances; each channel retains
 * the original scalar float expression and exact operation order. */
static Color color(const Region *r, int x, int y) {
    x = maxi(0, mini(r->width - 1, x));
    y = maxi(0, mini(r->height - 1, y));
    int slot = y & 15;
    if (r->colors && r->color_rows[slot] == y) return r->colors[(size_t)slot*r->width+x];
    uint32_t p = pixel(r,x,y);
    float yy=luma(p);
    Color value={yy, ((p>>16)&255)-yy, (p&255)-yy};
    return value;
}
static void cache_rows(Region *r,int y,int radius,int valid_begin,int valid_end) {
    if(!r->colors)return;
    int begin=maxi(valid_begin,y-radius), end=mini(valid_end,y+radius+1);
    for(int row=begin;row<end;row++) {
        int slot=row&15;if(r->color_rows[slot]==row)continue;
        Color *out=r->colors+(size_t)slot*r->width;
        const jint *in=r->pixels+(size_t)(row-r->first)*r->width;
        for(int x=0;x<r->width;x++){uint32_t p=(uint32_t)in[x];float yy=luma(p);out[x].y=yy;out[x].r=((p>>16)&255)-yy;out[x].b=(p&255)-yy;}
        r->color_rows[slot]=row;
    }
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
        left += color(r,x-2,maxi(valid_begin,mini(valid_end-1,y+k))).y; right += color(r,x+2,maxi(valid_begin,mini(valid_end-1,y+k))).y; up += color(r,x+k,maxi(valid_begin,mini(valid_end-1,y-2))).y; down += color(r,x+k,maxi(valid_begin,mini(valid_end-1,y+2))).y; n++;
    }
    return (fabsf(right - left) + fabsf(down - up)) / n;
}
/* Opposite-side strip means distinguish coherent edges from random grain.
 * The chroma guide is independent of the luma boundary gate. */
static void guide(const Region *src, int x, int y, int valid_begin, int valid_end,
        float sy, float sc, float *out) {
    float ly=0, ry=0, uy=0, dy=0, lr=0, rr=0, ur=0, dr=0, lb=0, rb=0, ub=0, db=0;
    for (int k=-1;k<=1;k++) for (int d=2;d<=3;d++) {
        uint32_t a=pixel(src,x-d,maxi(valid_begin,mini(valid_end-1,y+k)));
        uint32_t b=pixel(src,x+d,maxi(valid_begin,mini(valid_end-1,y+k)));
        uint32_t c=pixel(src,x+k,maxi(valid_begin,mini(valid_end-1,y-d)));
        uint32_t e=pixel(src,x+k,maxi(valid_begin,mini(valid_end-1,y+d)));
        if ((a>>24)!=255 || (b>>24)!=255 || (c>>24)!=255 || (e>>24)!=255) {out[0]=out[1]=0;return;}
        Color ca=color(src,x-d,maxi(valid_begin,mini(valid_end-1,y+k))),cb=color(src,x+d,maxi(valid_begin,mini(valid_end-1,y+k))),cc=color(src,x+k,maxi(valid_begin,mini(valid_end-1,y-d))),ce=color(src,x+k,maxi(valid_begin,mini(valid_end-1,y+d)));
        float ya=ca.y,yb=cb.y,yc=cc.y,ye=ce.y;
        ly+=ya;ry+=yb;uy+=yc;dy+=ye;
        lr+=ca.r;rr+=cb.r;ur+=cc.r;dr+=ce.r;
        lb+=ca.b;rb+=cb.b;ub+=cc.b;db+=ce.b;
    }
    float center_y=0,center_r=0,center_b=0;
    for (int py=-1;py<=1;py++) for (int px=-1;px<=1;px++) {
        uint32_t p=pixel(src,x+px,maxi(valid_begin,mini(valid_end-1,y+py)));
        if ((p>>24)!=255) {out[0]=out[1]=0;return;}
        Color cp=color(src,x+px,maxi(valid_begin,mini(valid_end-1,y+py)));center_y+=cp.y;center_r+=cp.r;center_b+=cp.b;
    }
    float curve_y=maxf(fabsf(ly+ry-center_y*4/3),fabsf(uy+dy-center_y*4/3))/6;
    float curve_c=maxf(fabsf(lr+rr-center_r*4/3)+fabsf(lb+rb-center_b*4/3),fabsf(ur+dr-center_r*4/3)+fabsf(ub+db-center_b*4/3))/6;
    float edge_y=maxf(fabsf(ry-ly),fabsf(dy-uy))/6;
    float edge_c=maxf(fabsf(rr-lr)+fabsf(rb-lb),fabsf(dr-ur)+fabsf(db-ub))/6;
    edge_y=maxf(0,edge_y-sy*.85f);edge_c=maxf(0,edge_c-sc*2.0f);
    out[0]=1/(1+square(edge_y/(sy*1.7f+2))+square(maxf(0,curve_y-sy*1.4f)/(sy*.85f+1)));
    out[1]=1/(1+square(edge_c/(sc*1.0f+1.5f))+square(maxf(0,curve_c-sc*2.5f)/(sc*1.5f+2)));
}
static float regional_sigma(const float *ev,int width,int height,int first_grid_y,int x,int y,int plane) {
    int rw=(width+63)/64,rh=(height+63)/64;
    float fx=clampf((x+.5f)/64-.5f,0,rw-1),fy=clampf((y+.5f)/64-.5f,0,rh-1);
    int ix=(int)fx,iy=(int)fy,nx=mini(rw-1,ix+1),ny=mini(rh-1,iy+1);fx-=ix;fy-=iy;
    iy-=first_grid_y;ny-=first_grid_y;
    float a=ev[16+3*(iy*rw+ix)+plane],b=ev[16+3*(iy*rw+nx)+plane];
    float c=ev[16+3*(ny*rw+ix)+plane],d=ev[16+3*(ny*rw+nx)+plane];
    return (a*(1-fx)+b*fx)*(1-fy)+(c*(1-fx)+d*fx)*fy;
}
/* Eight candidates use five observed neighbouring samples per candidate. */
static void nonlocal_fine(const Region *src,int x,int y,int valid_begin,int valid_end,
        float sy,float sc,float *result) {
    Color cp=color(src,x,y);float yy=cp.y,rr=cp.r,bb=cp.b,total=1,sum_y=yy,sum_r=rr,sum_b=bb;
    float variance=sy*sy+.22f*sc*sc,h2=maxf(1.5f,variance*3.2f);
    for (int dy=-4;dy<=4;dy+=4) for (int dx=-4;dx<=4;dx+=4) {
        if (dx==0 && dy==0) continue;
        int cx=x+dx,cy=y+dy;
        if (cx<0 || cy<valid_begin || cx>=src->width || cy>=valid_end) continue;
        float distance=0,patch_y=0,patch_r=0,patch_b=0;int valid=1;
        for (int k=0;k<5;k++) {
            int px=k==1?-1:k==2?1:0,py=k==3?-1:k==4?1:0;
            uint32_t a=pixel(src,x+px,maxi(valid_begin,mini(valid_end-1,y+py)));
            uint32_t b=pixel(src,cx+px,maxi(valid_begin,mini(valid_end-1,cy+py)));
            if ((a>>24)!=255 || (b>>24)!=255) {valid=0;continue;}
            Color ca=color(src,x+px,maxi(valid_begin,mini(valid_end-1,y+py))),cb=color(src,cx+px,maxi(valid_begin,mini(valid_end-1,cy+py)));float ya=ca.y,yb=cb.y,ra=ca.r,rb=cb.r,ba=ca.b,bc=cb.b;
            distance+=square(ya-yb)+.11f*(square(ra-rb)+square(ba-bc));patch_y+=yb;patch_r+=rb;patch_b+=bc;
        }
        if (!valid) continue;
        distance=maxf(0,distance/5-2*variance);float z=distance/h2;
        if (z>5) continue;
        float weight=1/(1+z+z*z*.5f+z*z*z/6);
        sum_y+=weight*(patch_y/5);sum_r+=weight*(patch_r/5);sum_b+=weight*(patch_b/5);total+=weight;
    }
    result[0]=sum_y/total;result[1]=sum_r/total;result[2]=sum_b/total;
}
static uint32_t pack(int guide, float r, float g, float b) {
    return ((uint32_t)maxi(0, mini(255, guide)) << 24) |
        ((uint32_t)(maxi(-127, mini(127, round_float(r))) & 255) << 16) |
        ((uint32_t)(maxi(-127, mini(127, round_float(g))) & 255) << 8) |
        (uint32_t)(maxi(-127, mini(127, round_float(b))) & 255);
}
static void nonlocal(const Region *src, int x, int y, float sy, float sc, float *result) {
    Color cp=color(src,x,y);float yy=cp.y,rr=cp.r,bb=cp.b;
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
            Color ca=color(src,x+px,y+py),cb=color(src,cx+px,cy+py);float ya=ca.y,yb=cb.y,ra=ca.r;
            float rb=cb.r,ba=ca.b,bc=cb.b;
            distance += square(ya - yb) + .11f * (square(ra - rb) + square(ba - bc));
        }
        if (!valid) continue;
        distance = maxf(0, distance / 9 - 2 * variance);
        float z = distance / h2;
        if (z > 5) continue;
        float weight = 1 / (1 + z + z * z * .5f + z * z * z / 6);
        Color cq=color(src,cx,cy);float qy=cq.y;
        sum_y += weight * qy; sum_r += weight * cq.r;
        sum_b += weight * cq.b; total += weight;
    }
    result[0] = sum_y / total; result[1] = sum_r / total; result[2] = sum_b / total;
}
typedef struct {
    float sy,sc,flat[2],legacy_flat;
    float sum_y,sum_r,sum_b,wy,wc,var;
} PixelWork1959;
#include "neon1959.h"
static void prepare_pixel(const Region *src, int x, int y, int noise, int shadows,
        const float *ev, int mode, jint *out,const PixelWork1959 *fast) {
    uint32_t p = pixel(src, x, y);
    Color cp=color(src,x,y);float y0=cp.y,cr=cp.r,cb=cp.b;
    if ((p >> 24) != 255) { *out = 0; return; }
    float sy = sigma(ev, 0, y0), sc = sigma(ev, 8, y0);
    if (sy < .40f && sc < .40f) { *out = (jint)((uint32_t)round_float(y0) << 24); return; }
    float flat[2];if(fast){flat[0]=fast->flat[0];flat[1]=fast->flat[1];}else guide(src,x,y,0,src->height,sy,sc,flat);
    float legacy_flat=fast?fast->legacy_flat:1/(1+square(structure(src,x,y,0,src->height)/(sy*3+3)));
    float threshold_y = maxf(3, sy * 4.5f), threshold_c = maxf(5, sc * 5.5f);
    float sum_y = fast?fast->sum_y:0, sum_r=fast?fast->sum_r:0,sum_b=fast?fast->sum_b:0,weight_y=fast?fast->wy:0,weight_c=fast?fast->wc:0,var_y=fast?fast->var:0;
    if(!fast)for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) {
        uint32_t q = pixel(src, x + dx, y + dy);
        if ((q >> 24) != 255) continue;
        Color cq=color(src,x+dx,y+dy);float yy=cq.y,rr=cq.r,bb=cq.b;
        float base = dx == 0 && dy == 0 ? 4.f : abs(dx) <= 1 && abs(dy) <= 1 ? 2.f : 1.f;
        float wy = base / (1 + square((yy - y0) / threshold_y));
        float wc = base / (1 + .12f * square((yy - y0) / threshold_y) + square((rr - cr) / threshold_c) + square((bb - cb) / threshold_c));
        sum_y += wy * yy; weight_y += wy; var_y += wy * square(yy - y0);
        sum_r += wc * rr; sum_b += wc * bb; weight_c += wc;
    }
    float dark = clampf((144 - y0) / 112, 0, 1);
    float strength = .60f + .08f * noise + (shadows ? .10f * dark : 0);
    float local_var = var_y / maxf(.001f, weight_y);
    float ly = sy >= .40f ? minf(.98f,strength) * legacy_flat * clampf(sy * sy / maxf(sy * sy, local_var * .55f), .12f, 1) : 0;
    float lc = sc >= .40f ? minf(.99f, strength + .10f) * (mode==0?flat[1]:maxf(flat[1],legacy_flat)) : 0;
    float target_y = sum_y / maxf(.001f, weight_y), target_r = sum_r / maxf(.001f, weight_c), target_b = sum_b / maxf(.001f, weight_c);
    if (mode <= 1 && (sy >= .60f || sc >= .60f) && flat[0] > .72f) {
        float nl[3];
        if (mode==1) nonlocal(src,x,y,sy,sc,nl);
        else nonlocal_fine(src,x,y,0,src->height,sy,sc,nl);
        float blend = (mode==1?.65f:.78f) * flat[0];
        target_y = target_y * (1 - blend) + nl[0] * blend;
        target_r = target_r * (1 - blend) + nl[1] * blend;
        target_b = target_b * (1 - blend) + nl[2] * blend;
        ly = minf(.98f, ly + .15f * flat[0]); lc = minf(.99f, lc + .10f * flat[1]);
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
        int valid_begin, int valid_end, int origin_y, int full_height, int first_grid_y, int noise, int shadows,
        const float *ev, const jint *policy, jint *out,const PixelWork1959 *fast,jint *confidence_out) {
    if(confidence_out)*confidence_out=0;
    uint32_t p = pixel(input, x, row);
    if ((p >> 24) != 255) { *out = (jint)p; return; }
    int budget = policy ? maxi(0, mini(256, policy[0])) : 256;
    int detail = policy ? maxi(0, mini(256, policy[1])) : 0;
    if (budget == 0) { *out = (jint)p; return; }
    Color cp=color(input,x,row);float y0=cp.y,r0=cp.r,b0=cp.b;
    float sy=regional_sigma(ev,input->width,full_height,first_grid_y,x,row+origin_y,0);
    float sc=regional_sigma(ev,input->width,full_height,first_grid_y,x,row+origin_y,1);
    float flat[2];if(fast){flat[0]=fast->flat[0];flat[1]=fast->flat[1];}else guide(input,x,row,valid_begin,valid_end,sy,sc,flat);
    if(confidence_out)*confidence_out=round_float(256*clampf((flat[0]-.65f)/.30f,0,1)*clampf((maxf(sy,sc)-.60f)/1.8f,0,1)*(1-regional_sigma(ev,input->width,full_height,first_grid_y,x,row+origin_y,2)));
    float dark=clampf((144-y0)/112,0,1);
    float threshold_y = maxf(2.5f, sy * 4), threshold_c = maxf(5, sc * 5);
    float sum_y=fast?fast->sum_y:0,sum_r=fast?fast->sum_r:0,sum_b=fast?fast->sum_b:0,wy_total=fast?fast->wy:0,wc_total=fast?fast->wc:0,var=fast?fast->var:0;
    if(!fast)for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) {
        uint32_t q = pixel(input, x + dx, maxi(valid_begin, mini(valid_end - 1, row + dy)));
        if ((q >> 24) != 255) continue;
        Color cq=color(input,x+dx,maxi(valid_begin,mini(valid_end-1,row+dy)));float yy=cq.y,rr=cq.r,bb=cq.b;
        float base = dx == 0 && dy == 0 ? 4.f : abs(dx) <= 1 && abs(dy) <= 1 ? 2.f : 1.f;
        float wy = base / (1 + square((yy - y0) / threshold_y));
        float wc = base / (1 + .12f * square((yy - y0) / threshold_y) + square((rr - r0) / threshold_c) + square((bb - b0) / threshold_c));
        sum_y += wy * yy; wy_total += wy; var += wy * square(yy - y0);
        sum_r += wc * rr; sum_b += wc * bb; wc_total += wc;
    }
    float strength = (.56f + .09f * noise + (shadows ? .12f * dark : 0)) * ((160 + budget * .375f) / 256.f);
    float texture=(1-.55f*detail/256.f)*(1-.85f*regional_sigma(ev,input->width,full_height,first_grid_y,x,row+origin_y,2));
    float confidence=clampf((flat[0]-.62f)/.34f,0,1),residual=clampf((maxf(sy,sc)-.60f)/3.f,0,1);
    float ly=sy>=.40f?minf(.98f,strength*(.70f+.30f*confidence*residual))*flat[0]*texture*clampf(sy*sy/maxf(sy*sy,var/maxf(.001f,wy_total)*.55f),.16f,1):0;
    float lc=sc>=.40f?minf(.995f,strength+.15f*residual)*flat[1]*texture:0;
    float target_y=sum_y/maxf(.001f,wy_total),target_r=sum_r/maxf(.001f,wc_total),target_b=sum_b/maxf(.001f,wc_total);
    if(confidence>.45f && residual>.15f && detail<192 && texture>.6f) {
        float nl[3];nonlocal_fine(input,x,row,valid_begin,valid_end,sy,sc,nl);
        float by=.84f*confidence*residual,bc=.88f*confidence*residual*flat[1];
        target_y=target_y*(1-by)+nl[0]*by;target_r=target_r*(1-bc)+nl[1]*bc;target_b=target_b*(1-bc)+nl[2]*bc;
        ly=minf(.985f,ly+.18f*confidence*residual*texture);lc=minf(.995f,lc+.10f*confidence*residual*texture);
    }
    float dy=(target_y-y0)*ly,dr=(target_r-r0)*lc,db=(target_b-b0)*lc;
    float d_r = dy + dr, d_g = dy - (.299f * dr + .114f * db) / .587f, d_b = dy + db;
    for (int k = 0; k < 3; k++) {
        float coarse[4]; sample(&maps[k], x, row + origin_y, 1 << (k + 1), coarse);
        float gate = 1 / (1 + square((coarse[3] - y0) / (sy * 5 + 16)));
        float blend = (k == 0 ? .48f : k == 1 ? .32f : .22f) * minf(1, strength) * texture * gate;
        float cy=.299f*coarse[0]+.587f*coarse[1]+.114f*coarse[2],cr=coarse[0]-cy,cb=coarse[2]-cy;
        float by=blend*(.30f+.70f*flat[0]),bc=blend*flat[1];
        d_r+=cy*by+cr*bc;d_g+=cy*by-(.299f*cr+.114f*cb)/.587f*bc;d_b+=cy*by+cb*bc;
    }
    *out = (jint)((p & UINT32_C(0xff000000)) | ((uint32_t)byte_value(((p >> 16) & 255) + d_r) << 16) |
        ((uint32_t)byte_value(((p >> 8) & 255) + d_g) << 8) | (uint32_t)byte_value((p & 255) + d_b));
}

JNIEXPORT jint JNICALL Java_com_hiro_ulike_StrongNoise1958_nativeAbi(JNIEnv *env, jclass cls) {
    (void)env; (void)cls; return 1958;
}

static jboolean process_native1959(
        JNIEnv *env, jclass cls, jintArray input_array, jintArray output_array,
        jint width, jint rows, jint begin, jint end, jint valid_begin, jint valid_end,
        jint origin_y, jint noise, jboolean shadows, jfloatArray evidence_array,
        jintArray half_array, jintArray quarter_array, jintArray eighth_array,
        jint full_width, jint full_height, jintArray policy_array, jint mode,jintArray confidence_array) {
    (void)cls;
    Region input = {0}, maps[3] = {{0}, {0}, {0}};
    Scratch1959 temporary={0}, *scratch=NULL;
    Cancellation cancellation = {0};
    jint *policy = NULL, *result = NULL,*confidence_result=NULL;
    int64_t confidence_count=0;int confidence_width=0;
    float *evidence=NULL;
    int64_t evidence_count=16,grid_offset=16;
    int first_grid_y=0;
    jboolean success = JNI_FALSE;
    int64_t source_count = (int64_t)width * rows;
    int64_t count = (int64_t)width * ((int64_t)end - begin);
    int64_t absolute_valid_begin = (int64_t)origin_y + valid_begin;
    int64_t absolute_valid_end = (int64_t)origin_y + valid_end;
    int64_t absolute_begin = (int64_t)origin_y + begin;
    int64_t absolute_end = (int64_t)origin_y + end;
    if (!input_array || !output_array || !evidence_array ||
        (*env)->IsSameObject(env, input_array, output_array) ||
        width < 1 || width > INT_MAX - 64 || rows < 1 || rows > INT_MAX - 18 ||
        source_count > INT_MAX || count < 0 || count > INT_MAX ||
        (uint64_t)count > SIZE_MAX / sizeof(jint) ||
        begin < valid_begin || end > valid_end || begin > end || valid_begin < 0 ||
        valid_end > rows || valid_begin >= valid_end ||
        noise < 0 || noise > 4 || mode < 0 || mode > 3 ||
        source_count > (*env)->GetArrayLength(env, input_array) ||
        source_count > (*env)->GetArrayLength(env, output_array) ||
        (*env)->GetArrayLength(env, evidence_array) < 16) return JNI_FALSE;
    if (mode == 3) {
        if (full_width != width || full_height < 1 || full_height > INT_MAX - 64 ||
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
    if(confidence_array) {
        confidence_width=(width+3)/4;confidence_count=(int64_t)confidence_width*((end-begin+3)/4);
        if(mode!=3 || (absolute_begin&3)!=0 || (((end-begin)&3)!=0 && absolute_end!=full_height) ||
           confidence_count>INT_MAX || confidence_count>(*env)->GetArrayLength(env,confidence_array) ||
           (*env)->IsSameObject(env,confidence_array,input_array) || (*env)->IsSameObject(env,confidence_array,output_array) ||
           (*env)->IsSameObject(env,confidence_array,half_array) || (*env)->IsSameObject(env,confidence_array,quarter_array) ||
           (*env)->IsSameObject(env,confidence_array,eighth_array) || (policy_array&&(*env)->IsSameObject(env,confidence_array,policy_array)))return JNI_FALSE;
    }
    /* Copy only regional evidence rows read by this core; a tall/narrow image
     * must not replicate its entire global grid for every worker. */
    if(mode==3) {
        int rw=(full_width+63)/64,rh=(full_height+63)/64;
        int64_t required=16+3*(int64_t)rw*rh;
        if(required>INT_MAX || required>(*env)->GetArrayLength(env,evidence_array)) goto cleanup;
        if(begin<end) {
            float first=clampf(((int)absolute_begin+.5f)/64-.5f,0,rh-1);
            float last=clampf(((int)absolute_end-1+.5f)/64-.5f,0,rh-1);
            first_grid_y=(int)first;
            int last_grid_y=mini(rh,(int)last+2);
            evidence_count=16+3*(int64_t)rw*(last_grid_y-first_grid_y);
            grid_offset=16+3*(int64_t)rw*first_grid_y;
        }
    }
    if(evidence_count>INT_MAX || (uint64_t)evidence_count>SIZE_MAX/sizeof(float)) goto cleanup;
    scratch=scratch_acquire1959(&temporary);
    evidence=scratch_reserve1959(scratch,6,(size_t)evidence_count*sizeof(float));
    if(!evidence) goto cleanup;
    (*env)->GetFloatArrayRegion(env,evidence_array,0,16,evidence);
    if((*env)->ExceptionCheck(env)) goto cleanup;
    if(evidence_count>16) (*env)->GetFloatArrayRegion(env,evidence_array,(jsize)grid_offset,(jsize)(evidence_count-16),evidence+16);
    if ((*env)->ExceptionCheck(env)) goto cleanup;
    for (int64_t i=0;i<evidence_count;i++) if(!isfinite(evidence[i]) || evidence[i]<0 || evidence[i]>((i>=18 && (i-18)%3==0)?1.f:1000000.f)) goto cleanup;
    if (!cancellation_start(env, &cancellation) || cancelled(env, &cancellation)) goto cleanup;
    if (begin == end) { success = JNI_TRUE; goto cleanup; }
    int radius = mode == 1 ? 7 : 5;
    if (!copy_region(env, input_array, width, rows, maxi(valid_begin, begin - radius),
            mini(valid_end, end + radius), &input,scratch,0)) goto cleanup;
    if (mode == 3) {
        jintArray arrays[3] = {half_array, quarter_array, eighth_array};
        int map_width = full_width, map_height = full_height;
        for (int k = 0; k < 3; k++) {
            map_width = ceil_div2(map_width); map_height = ceil_div2(map_height);
            int scale = 1 << (k + 1);
            float first_y = clampf(((int)absolute_begin + .5f) / scale - .5f, 0, map_height - 1);
            float last_y = clampf(((int)absolute_end - 1 + .5f) / scale - .5f, 0, map_height - 1);
            if (!copy_region(env, arrays[k], map_width, map_height, (int)first_y,
                    mini(map_height, (int)last_y + 2), &maps[k],scratch,k+1)) goto cleanup;
        }
        if (policy_array) {
            policy = scratch_reserve1959(scratch,4,(size_t)count*2*sizeof(jint));
            if (!policy) goto cleanup;
            (*env)->GetIntArrayRegion(env, policy_array, 0, (jsize)(count * 2), policy);
            if ((*env)->ExceptionCheck(env)) goto cleanup;
        }
    }
    result = scratch_reserve1959(scratch,5,(size_t)count*sizeof(jint));
    if (!result) goto cleanup;
    if(confidence_count){confidence_result=scratch_reserve1959(scratch,8,(size_t)confidence_count*sizeof(jint));if(!confidence_result)goto cleanup;memset(confidence_result,0,(size_t)confidence_count*sizeof(jint));}
    if((uint64_t)width <= SIZE_MAX/(16*sizeof(Color))) {
        input.colors=scratch_reserve1959(scratch,7,(size_t)width*16*sizeof(Color));
        if(!input.colors)goto cleanup;
        for(int i=0;i<16;i++)input.color_rows[i]=-1;
    }else goto cleanup;
    for (int y = begin; y < end; y++) {
        if ((y - begin) % 16 == 0 && cancelled(env, &cancellation)) goto cleanup;
        cache_rows(&input,y,radius,valid_begin,valid_end);
        for (int x = 0; x < width;) {
            PixelWork1959 fast[4];int lanes=1;
#if defined(__aarch64__) && !defined(ULIKE_DISABLE_NEON1959)
            if(x+4<=width && noise>0){
                lanes=4;neon_work1959(&input,x,y,valid_begin,valid_end,origin_y,full_height,first_grid_y,evidence,mode,fast);
            }
#endif
            for(int lane=0;lane<lanes;lane++) {
                int xx=x+lane;size_t at=(size_t)(y-begin)*width+xx;
                jint *confidence=NULL;
                if(confidence_result && xx==mini(width-1,(xx/4)*4+1) && y+origin_y==mini(full_height-1,((y+origin_y)/4)*4+1))
                    confidence=confidence_result+(size_t)((y-begin)/4)*confidence_width+xx/4;
                if(mode==3) {
                    if(noise==0)result[at]=(jint)pixel(&input,xx,y);
                    else process_pixel(&input,maps,xx,y,valid_begin,valid_end,origin_y,full_height,first_grid_y,noise,
                        shadows,evidence,policy?policy+at*2:NULL,result+at,lanes==4?fast+lane:NULL,confidence);
                }else prepare_pixel(&input,xx,y,noise,shadows,evidence,mode,result+at,lanes==4?fast+lane:NULL);
            }
            x+=lanes;
        }
    }
    if (cancelled(env, &cancellation)) goto cleanup;
    if(confidence_result){(*env)->SetIntArrayRegion(env,confidence_array,0,(jsize)confidence_count,confidence_result);if((*env)->ExceptionCheck(env))goto cleanup;}
    (*env)->SetIntArrayRegion(env, output_array, begin * width, (jsize)count, result);
    success = (*env)->ExceptionCheck(env) ? JNI_FALSE : JNI_TRUE;
cleanup:
    if(scratch)scratch_release1959(scratch);
    if (cancellation.thread) (*env)->DeleteLocalRef(env, cancellation.thread);
    return success;
}


#define NATIVE1959_ARGUMENTS JNIEnv *env,jclass cls,jintArray input_array,jintArray output_array,jint width,jint rows,jint begin,jint end,jint valid_begin,jint valid_end,jint origin_y,jint noise,jboolean shadows,jfloatArray evidence_array,jintArray half_array,jintArray quarter_array,jintArray eighth_array,jint full_width,jint full_height,jintArray policy_array,jint mode
#define NATIVE1959_FORWARD env,cls,input_array,output_array,width,rows,begin,end,valid_begin,valid_end,origin_y,noise,shadows,evidence_array,half_array,quarter_array,eighth_array,full_width,full_height,policy_array,mode
JNIEXPORT jboolean JNICALL Java_com_hiro_ulike_StrongNoise1958_processNative(NATIVE1959_ARGUMENTS){return process_native1959(NATIVE1959_FORWARD,NULL);}
JNIEXPORT jboolean JNICALL Java_com_hiro_ulike_StrongNoise1958_processNativeShared(NATIVE1959_ARGUMENTS,jintArray confidence){return process_native1959(NATIVE1959_FORWARD,confidence);}
JNIEXPORT jlong JNICALL Java_com_hiro_ulike_StrongNoise1958_nativeScratchBytes(JNIEnv *env,jclass cls){(void)env;(void)cls;return (jlong)scratch_bytes1959();}
JNIEXPORT void JNICALL Java_com_hiro_ulike_StrongNoise1958_nativeReleaseScratch(JNIEnv *env,jclass cls){(void)env;(void)cls;scratch_trim1959();}
