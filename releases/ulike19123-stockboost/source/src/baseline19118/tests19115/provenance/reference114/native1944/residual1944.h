#ifndef ULIKE_RESIDUAL1944_H
#define ULIKE_RESIDUAL1944_H
#include <stdint.h>
#include <stddef.h>

/* Java int arithmetic stays within signed-32 bounds: largest wide sum is
   (1+3+5+6+5+3+1)^2 * 256 * 255 = 37,601,280. */
int residual1944_valid(const int32_t *meta,int meta_count,int width,int rows,
    int begin,int end,int lo,int hi,int radius,int input_count,int output_count,
    int range_count,int ring_count,int x_count);
/* H30: metadata bit30 is an explicit final-pixel-caller opt-in. For fully
   protected texture (periodic==256), it returns stats==0 and leaves the first
   two seed words untouched: the original finalizer also performs no write.
   Callers needing complete aggregate summaries must leave bit30 unset. */
void residual1944_aggregate(const int32_t *src,const int32_t *meta,int32_t *out,
    int width,int begin,int end,int lo,int hi,int radius,const int32_t *range,
    int32_t *ring,const int32_t *x);
#endif
