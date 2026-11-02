#ifndef ULIKE_SPEED1935_H
#define ULIKE_SPEED1935_H
#include <stddef.h>
#include <stdint.h>
/* Inputs validated separately; output is never touched when validation fails. */
int speed1935_plane_valid(int start,int limit,int row_stride,int pixel_stride,int width,int height);
void speed1935_pack(const uint8_t *y,int ys,int yr,int yp,const uint8_t *u,int us,int ur,int up,const uint8_t *v,int vs,int vr,int vp,int width,int height,uint8_t *out);
int speed1935_horizontal_valid(const int32_t *raw,int raw_count,const int32_t *offsets,int offset_count,const int32_t *indices,int index_count,const float *weights,int weight_count,int out_count,int width);
void speed1935_horizontal(const int32_t *raw,const int32_t *offsets,const int32_t *indices,const float *weights,float *out,int width);
void speed1935_vertical(float *accum,float *minimum,float *maximum,const float *row,float weight,int count);
#endif
