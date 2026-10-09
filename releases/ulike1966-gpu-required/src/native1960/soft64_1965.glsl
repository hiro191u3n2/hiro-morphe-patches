/*
 * The implementations contained in this file are heavily based on the
 * implementations found in the Berkeley SoftFloat library. As such, they are
 * licensed under the same 3-clause BSD license:
 *
 * License for Berkeley SoftFloat Release 3e
 *
 * John R. Hauser
 * 2018 January 20
 *
 * The following applies to the whole of SoftFloat Release 3e as well as to
 * each source file individually.
 *
 * Copyright 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 The Regents of the
 * University of California.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions, and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions, and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *  3. Neither the name of the University nor the names of its contributors
 *     may be used to endorse or promote products derived from this software
 *     without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS "AS IS", AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/


/* Portable adaptation for ULike v1.9.65. Original source SHA256: 2fa2575aca36750f40faf91d043babf65f5f3b6c34db596822b05a16f725e149.
 * RNE, separate add/multiply, subnormal results retained; no native uint64/fp64.
 * Source: https://android.googlesource.com/platform/external/mesa3d/+/refs/heads/main/src/compiler/glsl/float64.glsl
 * GLSL callers use uvec2(lo,hi) and explicit s64_* arithmetic, never vector operators.
 */
#ifndef ULIKE_SOFT64_1965
#define ULIKE_SOFT64_1965
#define FLOAT_ROUND_NEAREST_EVEN 0
#define FLOAT_ROUND_TO_ZERO 1
#define FLOAT_ROUND_DOWN 2
#define FLOAT_ROUND_UP 3
#define FLOAT_ROUNDING_MODE FLOAT_ROUND_NEAREST_EVEN
#define RELAXED_NAN_PROPAGATION
#define EXCHANGE(a,b) do { a^=b; b^=a; a^=b; } while(false)
uvec2 packUint2x32(uvec2 v){return v;}
uvec2 unpackUint2x32(uvec2 v){return v;}
uvec2 s64m_mix(uvec2 a,uvec2 b,bool c){return c?b:a;}
uvec2 s64m_mix(uvec2 a,uvec2 b,bvec2 c){return uvec2(c.x?b.x:a.x,c.y?b.y:a.y);}
uint s64m_mix(uint a,uint b,bool c){return c?b:a;}
int s64m_mix(int a,int b,bool c){return c?b:a;}
float s64m_mix(float a,float b,bool c){return c?b:a;}
uvec2
s64m_fabs64(uvec2 s64m_a)
{
   uvec2 a = unpackUint2x32(s64m_a);
   a.y &= 0x7FFFFFFFu;
   return packUint2x32(a);
}

bool
s64m_is_nan(uvec2 s64m_a)
{
   uvec2 a = unpackUint2x32(s64m_a);
   return (0xFFE00000u <= (a.y<<1)) &&
      ((a.x != 0u) || ((a.y & 0x000FFFFFu) != 0u));
}

uvec2
s64m_fneg64(uvec2 s64m_a)
{
   uvec2 a = unpackUint2x32(s64m_a);
   a.y ^= (1u << 31);
   return packUint2x32(a);
}

uint
s64m_extractFloat64FracLo(uvec2 a)
{
   return unpackUint2x32(a).x;
}

uint
s64m_extractFloat64FracHi(uvec2 a)
{
   return unpackUint2x32(a).y & 0x000FFFFFu;
}

int
s64m_extractFloat64Exp(uvec2 s64m_a)
{
   uvec2 a = unpackUint2x32(s64m_a);
   return int((a.y>>20) & 0x7FFu);
}

bool
s64m_feq64_nonnan(uvec2 s64m_a, uvec2 s64m_b)
{
   uvec2 a = unpackUint2x32(s64m_a);
   uvec2 b = unpackUint2x32(s64m_b);
   return (a.x == b.x) &&
          ((a.y == b.y) || ((a.x == 0u) && (((a.y | b.y)<<1) == 0u)));
}

bool
s64m_feq64(uvec2 a, uvec2 b)
{
   if (s64m_is_nan(a) || s64m_is_nan(b))
      return false;

   return s64m_feq64_nonnan(a, b);
}

uint
s64m_extractFloat64Sign(uvec2 a)
{
   return unpackUint2x32(a).y & 0x80000000u;
}

bool
ilt64(uint a0, uint a1, uint b0, uint b1)
{
   return (int(a0) < int(b0)) || ((a0 == b0) && (a1 < b1));
}

bool
s64m_flt64_nonnan(uvec2 s64m_a, uvec2 s64m_b)
{
   uvec2 a = unpackUint2x32(s64m_a);
   uvec2 b = unpackUint2x32(s64m_b);

   /* IEEE 754 floating point numbers are specifically designed so that, with
    * two exceptions, values can be compared by bit-casting to signed integers
    * with the same number of bits.
    *
    * From https://en.wikipedia.org/wiki/IEEE_754-1985#Comparing_floating-point_numbers:
    *
    *    When comparing as 2's-complement integers: If the sign bits differ,
    *    the negative number precedes the positive number, so 2's complement
    *    gives the correct result (except that negative zero and positive zero
    *    should be considered equal). If both values are positive, the 2's
    *    complement comparison again gives the correct result. Otherwise (two
    *    negative numbers), the correct FP ordering is the opposite of the 2's
    *    complement ordering.
    *
    * The logic implied by the above quotation is:
    *
    *    !both_are_zero(a, b) && (both_negative(a, b) ? a > b : a < b)
    *
    * This is equivalent to
    *
    *    fneu(a, b) && (both_negative(a, b) ? a >= b : a < b)
    *
    *    fneu(a, b) && (both_negative(a, b) ? !(a < b) : a < b)
    *
    *    fneu(a, b) && ((both_negative(a, b) && !(a < b)) ||
    *                  (!both_negative(a, b) && (a < b)))
    *
    * (A!|B)&(A|!B) is (A xor B) which is implemented here using !=.
    *
    *    fneu(a, b) && (both_negative(a, b) != (a < b))
    */
   bool lt = ilt64(a.y, a.x, b.y, b.x);
   bool both_negative = (a.y & b.y & 0x80000000u) != 0u;

   return !s64m_feq64_nonnan(s64m_a, s64m_b) && (lt != both_negative);
}

bool
s64m_flt64_nonnan_minmax(uvec2 s64m_a, uvec2 s64m_b)
{
   uvec2 a = unpackUint2x32(s64m_a);
   uvec2 b = unpackUint2x32(s64m_b);

   /* See s64m_flt64_nonnan. For implementing fmin/fmax, we compare -0 < 0, so the
    * implied logic is a bit simpler:
    *
    *    both_negative(a, b) ? a > b : a < b
    *
    * If a == b, it doesn't matter what we return, so that's equivalent to:
    *
    *    both_negative(a, b) ? a >= b : a < b
    *    both_negative(a, b) ? !(a < b) : a < b
    *    both_negative(a, b) ^ (a < b)
    *
    * XOR is again implemented using !=.
    */
   bool lt = ilt64(a.y, a.x, b.y, b.x);
   bool both_negative = (a.y & b.y & 0x80000000u) != 0u;

   return (lt != both_negative);
}

bool
s64m_flt64(uvec2 a, uvec2 b)
{
   /* This weird layout matters.  Doing the "obvious" thing results in extra
    * flow control being inserted to implement the short-circuit evaluation
    * rules.  Flow control is bad!
    */
   bool x = !s64m_is_nan(a);
   bool y = !s64m_is_nan(b);
   bool z = s64m_flt64_nonnan(a, b);

   return (x && y && z);
}

bool
s64m_fge64(uvec2 a, uvec2 b)
{
   /* This weird layout matters.  Doing the "obvious" thing results in extra
    * flow control being inserted to implement the short-circuit evaluation
    * rules.  Flow control is bad!
    */
   bool x = !s64m_is_nan(a);
   bool y = !s64m_is_nan(b);
   bool z = !s64m_flt64_nonnan(a, b);

   return (x && y && z);
}

void
s64m_add64(uint a0, uint a1, uint b0, uint b1,
        out uint z0Ptr,
        out uint z1Ptr)
{
   uint z1 = a1 + b1;
   z1Ptr = z1;
   z0Ptr = a0 + b0 + uint(z1 < a1);
}

void
s64m_sub64(uint a0, uint a1, uint b0, uint b1,
        out uint z0Ptr,
        out uint z1Ptr)
{
   z1Ptr = a1 - b1;
   z0Ptr = a0 - b0 - uint(a1 < b1);
}

void
s64m_shift64RightJamming(uint a0,
                      uint a1,
                      int count,
                      out uint z0Ptr,
                      out uint z1Ptr)
{
   uint z0;
   uint z1;
   int negCount = (-count) & 31;

   z0 = s64m_mix(0u, a0, count == 0);
   z0 = s64m_mix(z0, (a0 >> (count & 31)), count < 32);

   z1 = uint((a0 | a1) != 0u); /* count >= 64 */
   uint z1_lt64 = (a0>>(count & 31)) | uint(((a0<< (negCount & 31)) | a1) != 0u);
   z1 = s64m_mix(z1, z1_lt64, count < 64);
   z1 = s64m_mix(z1, (a0 | uint(a1 != 0u)), count == 32);
   uint z1_lt32 = (a0<< (negCount & 31)) | (a1>> (count & 31)) | uint ((a1<< (negCount & 31)) != 0u);
   z1 = s64m_mix(z1, z1_lt32, count < 32);
   z1 = s64m_mix(z1, a1, count == 0);
   z1Ptr = z1;
   z0Ptr = z0;
}

void
s64m_shift64ExtraRightJamming(uint a0, uint a1, uint a2,
                           int count,
                           out uint z0Ptr,
                           out uint z1Ptr,
                           out uint z2Ptr)
{
   uint z0 = 0u;
   uint z1;
   uint z2;
   int negCount = (-count) & 31;

   z2 = s64m_mix(uint(a0 != 0u), a0, count == 64);
   z2 = s64m_mix(z2, a0 << (negCount & 31), count < 64);
   z2 = s64m_mix(z2, a1 << (negCount & 31), count < 32);

   z1 = s64m_mix(0u, (a0 >> (count & 31)), count < 64);
   z1 = s64m_mix(z1, (a0<< (negCount & 31)) | (a1>> (count & 31)), count < 32);

   a2 = s64m_mix(a2 | a1, a2, count < 32);
   z0 = s64m_mix(z0, a0 >> (count & 31), count < 32);
   z2 |= uint(a2 != 0u);

   z0 = s64m_mix(z0, 0u, (count == 32));
   z1 = s64m_mix(z1, a0, (count == 32));
   z2 = s64m_mix(z2, a1, (count == 32));
   z0 = s64m_mix(z0, a0, (count == 0));
   z1 = s64m_mix(z1, a1, (count == 0));
   z2 = s64m_mix(z2, a2, (count == 0));
   z2Ptr = z2;
   z1Ptr = z1;
   z0Ptr = z0;
}

void
s64m_shortShift64Left(uint a0, uint a1,
                   int count,
                   out uint z0Ptr,
                   out uint z1Ptr)
{
   z1Ptr = a1<< (count & 31);
   z0Ptr = s64m_mix((a0 << (count & 31) | (a1 >> ((-count) & 31))), a0, count == 0);
}

uvec2
s64m_packFloat64(uint zSign, int zExp, uint zFrac0, uint zFrac1)
{
   uvec2 z;

   z.y = zSign + (uint(zExp) << 20) + zFrac0;
   z.x = zFrac1;
   return packUint2x32(z);
}

uvec2
s64m_roundAndPackFloat64(uint zSign,
                      int zExp,
                      uint zFrac0,
                      uint zFrac1,
                      uint zFrac2)
{
   bool roundNearestEven;
   bool increment;

   roundNearestEven = FLOAT_ROUNDING_MODE == FLOAT_ROUND_NEAREST_EVEN;
   increment = int(zFrac2) < 0;
   if (!roundNearestEven) {
      if (FLOAT_ROUNDING_MODE == FLOAT_ROUND_TO_ZERO) {
         increment = false;
      } else {
         if (zSign != 0u) {
            increment = (FLOAT_ROUNDING_MODE == FLOAT_ROUND_DOWN) &&
               (zFrac2 != 0u);
         } else {
            increment = (FLOAT_ROUNDING_MODE == FLOAT_ROUND_UP) &&
               (zFrac2 != 0u);
         }
      }
   }
   if (0x7FD <= zExp) {
      if ((0x7FD < zExp) ||
         ((zExp == 0x7FD) &&
            (0x001FFFFFu == zFrac0 && 0xFFFFFFFFu == zFrac1) &&
               increment)) {
         if ((FLOAT_ROUNDING_MODE == FLOAT_ROUND_TO_ZERO) ||
            ((zSign != 0u) && (FLOAT_ROUNDING_MODE == FLOAT_ROUND_UP)) ||
               ((zSign == 0u) && (FLOAT_ROUNDING_MODE == FLOAT_ROUND_DOWN))) {
            return s64m_packFloat64(zSign, 0x7FE, 0x000FFFFFu, 0xFFFFFFFFu);
         }
         return s64m_packFloat64(zSign, 0x7FF, 0u, 0u);
      }
   }

   if (zExp < 0) {
      s64m_shift64ExtraRightJamming(
         zFrac0, zFrac1, zFrac2, -zExp, zFrac0, zFrac1, zFrac2);
      zExp = 0;
      if (roundNearestEven) {
         increment = int(zFrac2) < 0;
      } else {
         if (zSign != 0u) {
            increment = (FLOAT_ROUNDING_MODE == FLOAT_ROUND_DOWN) &&
               (zFrac2 != 0u);
         } else {
            increment = (FLOAT_ROUNDING_MODE == FLOAT_ROUND_UP) &&
               (zFrac2 != 0u);
         }
      }
   }

   if (increment) {
      s64m_add64(zFrac0, zFrac1, 0u, 1u, zFrac0, zFrac1);
      zFrac1 &= ~uint((zFrac2 == 0x80000000u) && roundNearestEven);
   } else {
      zExp = s64m_mix(zExp, 0, (zFrac0 | zFrac1) == 0u);
   }
   return s64m_packFloat64(zSign, zExp, zFrac0, zFrac1);
}

int
s64m_countLeadingZeros32(uint a)
{
   return 31 - findMSB(a);
}

uvec2
s64m_normalizeRoundAndPackFloat64(uint zSign,
                               int zExp,
                               uint zFrac0,
                               uint zFrac1)
{
   int shiftCount;
   uint zFrac2;

   if (zFrac0 == 0u) {
      zExp -= 32;
      zFrac0 = zFrac1;
      zFrac1 = 0u;
   }

   shiftCount = s64m_countLeadingZeros32(zFrac0) - 11;
   if (0 <= shiftCount) {
      zFrac2 = 0u;
      s64m_shortShift64Left(zFrac0, zFrac1, shiftCount, zFrac0, zFrac1);
   } else {
      s64m_shift64ExtraRightJamming(
         zFrac0, zFrac1, 0u, -shiftCount, zFrac0, zFrac1, zFrac2);
   }
   zExp -= shiftCount;
   return s64m_roundAndPackFloat64(zSign, zExp, zFrac0, zFrac1, zFrac2);
}

uvec2
s64m_propagateFloat64NaN(uvec2 s64m_a, uvec2 s64m_b)
{
#if defined RELAXED_NAN_PROPAGATION
   uvec2 a = unpackUint2x32(s64m_a);
   uvec2 b = unpackUint2x32(s64m_b);

   return packUint2x32(uvec2(a.x | b.x, a.y | b.y));
#else
   bool aIsNaN = s64m_is_nan(s64m_a);
   bool bIsNaN = s64m_is_nan(s64m_b);
   uvec2 a = unpackUint2x32(s64m_a);
   uvec2 b = unpackUint2x32(s64m_b);
   a.y |= 0x00080000u;
   b.y |= 0x00080000u;

   return packUint2x32(s64m_mix(b, s64m_mix(a, b, bvec2(bIsNaN, bIsNaN)), bvec2(aIsNaN, aIsNaN)));
#endif
}

uvec2
s64m_fadd64(uvec2 a, uvec2 b)
{
   uint aSign = s64m_extractFloat64Sign(a);
   uint bSign = s64m_extractFloat64Sign(b);
   uint aFracLo = s64m_extractFloat64FracLo(a);
   uint aFracHi = s64m_extractFloat64FracHi(a);
   uint bFracLo = s64m_extractFloat64FracLo(b);
   uint bFracHi = s64m_extractFloat64FracHi(b);
   int aExp = s64m_extractFloat64Exp(a);
   int bExp = s64m_extractFloat64Exp(b);
   int expDiff = aExp - bExp;
   if (aSign == bSign) {
      uint zFrac0;
      uint zFrac1;
      uint zFrac2;
      int zExp;

      if (expDiff == 0) {
         if (aExp == 0x7FF) {
            bool propagate = ((aFracHi | bFracHi) | (aFracLo| bFracLo)) != 0u;
            return s64m_mix(a, s64m_propagateFloat64NaN(a, b), propagate);
         }
         s64m_add64(aFracHi, aFracLo, bFracHi, bFracLo, zFrac0, zFrac1);
         if (aExp == 0)
            return s64m_packFloat64(aSign, 0, zFrac0, zFrac1);
         zFrac2 = 0u;
         zFrac0 |= 0x00200000u;
         zExp = aExp;
         s64m_shift64ExtraRightJamming(
            zFrac0, zFrac1, zFrac2, 1, zFrac0, zFrac1, zFrac2);
      } else {
         if (expDiff < 0) {
            EXCHANGE(aFracHi, bFracHi);
            EXCHANGE(aFracLo, bFracLo);
            EXCHANGE(aExp, bExp);
         }

         if (aExp == 0x7FF) {
            bool propagate = (aFracHi | aFracLo) != 0u;
            return s64m_mix(s64m_packFloat64(aSign, 0x7ff, 0u, 0u), s64m_propagateFloat64NaN(a, b), propagate);
         }

         expDiff = s64m_mix(abs(expDiff), abs(expDiff) - 1, bExp == 0);
         bFracHi = s64m_mix(bFracHi | 0x00100000u, bFracHi, bExp == 0);
         s64m_shift64ExtraRightJamming(
            bFracHi, bFracLo, 0u, expDiff, bFracHi, bFracLo, zFrac2);
         zExp = aExp;

         aFracHi |= 0x00100000u;
         s64m_add64(aFracHi, aFracLo, bFracHi, bFracLo, zFrac0, zFrac1);
         --zExp;
         if (!(zFrac0 < 0x00200000u)) {
            s64m_shift64ExtraRightJamming(zFrac0, zFrac1, zFrac2, 1, zFrac0, zFrac1, zFrac2);
            ++zExp;
         }
      }
      return s64m_roundAndPackFloat64(aSign, zExp, zFrac0, zFrac1, zFrac2);

   } else {
      int zExp;

      s64m_shortShift64Left(aFracHi, aFracLo, 10, aFracHi, aFracLo);
      s64m_shortShift64Left(bFracHi, bFracLo, 10, bFracHi, bFracLo);
      if (expDiff != 0) {
         uint zFrac0;
         uint zFrac1;

         if (expDiff < 0) {
            EXCHANGE(aFracHi, bFracHi);
            EXCHANGE(aFracLo, bFracLo);
            EXCHANGE(aExp, bExp);
            aSign ^= 0x80000000u;
         }

         if (aExp == 0x7FF) {
            bool propagate = (aFracHi | aFracLo) != 0u;
            return s64m_mix(s64m_packFloat64(aSign, 0x7ff, 0u, 0u), s64m_propagateFloat64NaN(a, b), propagate);
         }

         expDiff = s64m_mix(abs(expDiff), abs(expDiff) - 1, bExp == 0);
         bFracHi = s64m_mix(bFracHi | 0x40000000u, bFracHi, bExp == 0);
         s64m_shift64RightJamming(bFracHi, bFracLo, expDiff, bFracHi, bFracLo);
         aFracHi |= 0x40000000u;
         s64m_sub64(aFracHi, aFracLo, bFracHi, bFracLo, zFrac0, zFrac1);
         zExp = aExp;
         --zExp;
         return s64m_normalizeRoundAndPackFloat64(aSign, zExp - 10, zFrac0, zFrac1);
      }
      if (aExp == 0x7FF) {
         bool propagate = ((aFracHi | bFracHi) | (aFracLo | bFracLo)) != 0u;
         return s64m_mix(uvec2(0xffffffffu,0xffffffffu), s64m_propagateFloat64NaN(a, b), propagate);
      }
      bExp = s64m_mix(bExp, 1, aExp == 0);
      aExp = s64m_mix(aExp, 1, aExp == 0);

      uint zFrac0;
      uint zFrac1;
      uint sign_of_difference = 0u;
      if (bFracHi < aFracHi) {
         s64m_sub64(aFracHi, aFracLo, bFracHi, bFracLo, zFrac0, zFrac1);
      }
      else if (aFracHi < bFracHi) {
         s64m_sub64(bFracHi, bFracLo, aFracHi, aFracLo, zFrac0, zFrac1);
         sign_of_difference = 0x80000000u;
      }
      else if (bFracLo <= aFracLo) {
         /* It is possible that zFrac0 and zFrac1 may be zero after this. */
         s64m_sub64(aFracHi, aFracLo, bFracHi, bFracLo, zFrac0, zFrac1);
      }
      else {
         s64m_sub64(bFracHi, bFracLo, aFracHi, aFracLo, zFrac0, zFrac1);
         sign_of_difference = 0x80000000u;
      }
      zExp = s64m_mix(bExp, aExp, sign_of_difference == 0u);
      aSign ^= sign_of_difference;
      uvec2 retval_0 = s64m_packFloat64(uint(FLOAT_ROUNDING_MODE == FLOAT_ROUND_DOWN) << 31, 0, 0u, 0u);
      uvec2 retval_1 = s64m_normalizeRoundAndPackFloat64(aSign, zExp - 11, zFrac0, zFrac1);
      return s64m_mix(retval_0, retval_1, zFrac0 != 0u || zFrac1 != 0u);
   }
}

void
s64m_mul64To128(uint a0, uint a1, uint b0, uint b1,
             out uint z0Ptr,
             out uint z1Ptr,
             out uint z2Ptr,
             out uint z3Ptr)
{
   uint z0 = 0u;
   uint z1 = 0u;
   uint z2 = 0u;
   uint z3 = 0u;
   uint more1 = 0u;
   uint more2 = 0u;

   umulExtended(a1, b1, z2, z3);
   umulExtended(a1, b0, z1, more2);
   s64m_add64(z1, more2, 0u, z2, z1, z2);
   umulExtended(a0, b0, z0, more1);
   s64m_add64(z0, more1, 0u, z1, z0, z1);
   umulExtended(a0, b1, more1, more2);
   s64m_add64(more1, more2, 0u, z2, more1, z2);
   s64m_add64(z0, z1, 0u, more1, z0, z1);
   z3Ptr = z3;
   z2Ptr = z2;
   z1Ptr = z1;
   z0Ptr = z0;
}

void
s64m_normalizeFloat64Subnormal(uint aFrac0, uint aFrac1,
                            out int zExpPtr,
                            out uint zFrac0Ptr,
                            out uint zFrac1Ptr)
{
   int shiftCount;
   uint temp_zfrac0, temp_zfrac1;
   shiftCount = s64m_countLeadingZeros32(s64m_mix(aFrac0, aFrac1, aFrac0 == 0u)) - 11;
   zExpPtr = s64m_mix(1 - shiftCount, -shiftCount - 31, aFrac0 == 0u);

   temp_zfrac0 = s64m_mix(aFrac1<< (shiftCount & 31), aFrac1>> ((-shiftCount) & 31), shiftCount < 0);
   temp_zfrac1 = s64m_mix(0u, aFrac1<<(shiftCount & 31), shiftCount < 0);

   s64m_shortShift64Left(aFrac0, aFrac1, shiftCount, zFrac0Ptr, zFrac1Ptr);

   zFrac0Ptr = s64m_mix(zFrac0Ptr, temp_zfrac0, aFrac0 == 0u);
   zFrac1Ptr = s64m_mix(zFrac1Ptr, temp_zfrac1, aFrac0 == 0u);
}

uvec2
s64m_fmul64(uvec2 a, uvec2 b)
{
   uint zFrac0 = 0u;
   uint zFrac1 = 0u;
   uint zFrac2 = 0u;
   uint zFrac3 = 0u;
   int zExp;

   uint aFracLo = s64m_extractFloat64FracLo(a);
   uint aFracHi = s64m_extractFloat64FracHi(a);
   uint bFracLo = s64m_extractFloat64FracLo(b);
   uint bFracHi = s64m_extractFloat64FracHi(b);
   int aExp = s64m_extractFloat64Exp(a);
   uint aSign = s64m_extractFloat64Sign(a);
   int bExp = s64m_extractFloat64Exp(b);
   uint bSign = s64m_extractFloat64Sign(b);
   uint zSign = aSign ^ bSign;
   if (aExp == 0x7FF) {
      if (((aFracHi | aFracLo) != 0u) ||
         ((bExp == 0x7FF) && ((bFracHi | bFracLo) != 0u))) {
         return s64m_propagateFloat64NaN(a, b);
      }
      if ((uint(bExp) | bFracHi | bFracLo) == 0u)
            return uvec2(0xffffffffu,0xffffffffu);
      return s64m_packFloat64(zSign, 0x7FF, 0u, 0u);
   }
   if (bExp == 0x7FF) {
      /* a cannot be NaN, but is b NaN? */
      if ((bFracHi | bFracLo) != 0u)
#if defined RELAXED_NAN_PROPAGATION
         return b;
#else
         return s64m_propagateFloat64NaN(a, b);
#endif
      if ((uint(aExp) | aFracHi | aFracLo) == 0u)
         return uvec2(0xffffffffu,0xffffffffu);
      return s64m_packFloat64(zSign, 0x7FF, 0u, 0u);
   }
   if (aExp == 0) {
      if ((aFracHi | aFracLo) == 0u)
         return s64m_packFloat64(zSign, 0, 0u, 0u);
      s64m_normalizeFloat64Subnormal(aFracHi, aFracLo, aExp, aFracHi, aFracLo);
   }
   if (bExp == 0) {
      if ((bFracHi | bFracLo) == 0u)
         return s64m_packFloat64(zSign, 0, 0u, 0u);
      s64m_normalizeFloat64Subnormal(bFracHi, bFracLo, bExp, bFracHi, bFracLo);
   }
   zExp = aExp + bExp - 0x400;
   aFracHi |= 0x00100000u;
   s64m_shortShift64Left(bFracHi, bFracLo, 12, bFracHi, bFracLo);
   s64m_mul64To128(
      aFracHi, aFracLo, bFracHi, bFracLo, zFrac0, zFrac1, zFrac2, zFrac3);
   s64m_add64(zFrac0, zFrac1, aFracHi, aFracLo, zFrac0, zFrac1);
   zFrac2 |= uint(zFrac3 != 0u);
   if (0x00200000u <= zFrac0) {
      s64m_shift64ExtraRightJamming(
         zFrac0, zFrac1, zFrac2, 1, zFrac0, zFrac1, zFrac2);
      ++zExp;
   }
   return s64m_roundAndPackFloat64(zSign, zExp, zFrac0, zFrac1, zFrac2);
}

void
s64m_shift64Right(uint a0, uint a1,
               int count,
               out uint z0Ptr,
               out uint z1Ptr)
{
   uint z0;
   uint z1;
   int negCount = (-count) & 31;

   z0 = 0u;
   z0 = s64m_mix(z0, (a0 >> (count & 31)), count < 32);
   z0 = s64m_mix(z0, a0, count == 0);

   z1 = s64m_mix(0u, (a0 >> (count & 31)), count < 64);
   z1 = s64m_mix(z1, (a0<< (negCount & 31)) | (a1>> (count & 31)), count < 32);
   z1 = s64m_mix(z1, a0, count == 0);

   z1Ptr = z1;
   z0Ptr = z0;
}

uvec2
s64m_uint_to_fp64(uint a)
{
   if (a == 0u)
      return uvec2(0u,0u);

   int shiftDist = s64m_countLeadingZeros32(a) + 21;

   uint aHigh = 0u;
   uint aLow = 0u;
   int negCount = (- shiftDist) & 31;

   aHigh = s64m_mix(0u, a<< ((shiftDist - 32) & 31), shiftDist < 64);
   aLow = 0u;
   aHigh = s64m_mix(aHigh, 0u, shiftDist == 0);
   aLow = s64m_mix(aLow, a, shiftDist ==0);
   aHigh = s64m_mix(aHigh, a >> (negCount & 31), shiftDist < 32);
   aLow = s64m_mix(aLow, a << (shiftDist & 31), shiftDist < 32);

   return s64m_packFloat64(0u, 0x432 - shiftDist, aHigh, aLow);
}

int
s64m_fp64_to_int(uvec2 a)
{
   uint aFracLo = s64m_extractFloat64FracLo(a);
   uint aFracHi = s64m_extractFloat64FracHi(a);
   int aExp = s64m_extractFloat64Exp(a);
   uint aSign = s64m_extractFloat64Sign(a);

   uint absZ = 0u;
   uint aFracExtra = 0u;
   int shiftCount = aExp - 0x413;

   if (0 <= shiftCount) {
      if (0x41E < aExp) {
         if ((aExp == 0x7FF) && bool(aFracHi | aFracLo))
            aSign = 0u;
         return s64m_mix(0x7FFFFFFF, 0x80000000, aSign != 0u);
      }
      s64m_shortShift64Left(aFracHi | 0x00100000u, aFracLo, shiftCount, absZ, aFracExtra);
   } else {
      if (aExp < 0x3FF)
         return 0;

      aFracHi |= 0x00100000u;
      aFracExtra = ( aFracHi << (shiftCount & 31)) | aFracLo;
      absZ = aFracHi >> ((-shiftCount) & 31);
   }

   int z = s64m_mix(int(absZ), -int(absZ), aSign != 0u);
   int nan = s64m_mix(0x7FFFFFFF, 0x80000000, aSign != 0u);
   return s64m_mix(z, nan, ((aSign != 0u) != (z < 0)) && bool(z));
}

uvec2
s64m_int_to_fp64(int a)
{
   uint zFrac0 = 0u;
   uint zFrac1 = 0u;
   if (a==0)
      return s64m_packFloat64(0u, 0, 0u, 0u);
   uint zSign = uint(a) & 0x80000000u;
   uint absA = s64m_mix(uint(a), uint(-a), a < 0);
   int shiftCount = s64m_countLeadingZeros32(absA) - 11;
   if (0 <= shiftCount) {
      zFrac0 = absA << (shiftCount & 31);
      zFrac1 = 0u;
   } else {
      s64m_shift64Right(absA, 0u, -shiftCount, zFrac0, zFrac1);
   }
   return s64m_packFloat64(zSign, 0x412 - shiftCount, zFrac0, zFrac1);
}

float
s64m_packFloat32(uint zSign, int zExp, uint zFrac)
{
   return uintBitsToFloat(zSign + (uint(zExp)<<23) + zFrac);
}

float
s64m_roundAndPackFloat32(uint zSign, int zExp, uint zFrac)
{
   bool roundNearestEven;
   int roundIncrement;
   int roundBits;

   roundNearestEven = FLOAT_ROUNDING_MODE == FLOAT_ROUND_NEAREST_EVEN;
   roundIncrement = 0x40;
   if (!roundNearestEven) {
      if (FLOAT_ROUNDING_MODE == FLOAT_ROUND_TO_ZERO) {
         roundIncrement = 0;
      } else {
         roundIncrement = 0x7F;
         if (zSign != 0u) {
            if (FLOAT_ROUNDING_MODE == FLOAT_ROUND_UP)
               roundIncrement = 0;
         } else {
            if (FLOAT_ROUNDING_MODE == FLOAT_ROUND_DOWN)
               roundIncrement = 0;
         }
      }
   }
   roundBits = int(zFrac & 0x7Fu);
   if (0xFDu <= uint(zExp)) {
      if ((0xFD < zExp) || ((zExp == 0xFD) && (int(zFrac) + roundIncrement) < 0))
         return s64m_packFloat32(zSign, 0xFF, 0u) - float(roundIncrement == 0);
      int count = -zExp;
      bool zexp_lt0 = zExp < 0;
      uint zFrac_lt0 = s64m_mix(uint(zFrac != 0u), (zFrac>> (count & 31)) | uint((zFrac<<((-count) & 31)) != 0u), (-zExp) < 32);
      zFrac = s64m_mix(zFrac, zFrac_lt0, zexp_lt0);
      roundBits = s64m_mix(roundBits, int(zFrac) & 0x7f, zexp_lt0);
      zExp = s64m_mix(zExp, 0, zexp_lt0);
   }
   zFrac = (zFrac + uint(roundIncrement))>>7;
   zFrac &= ~uint(((roundBits ^ 0x40) == 0) && roundNearestEven);

   return s64m_packFloat32(zSign, s64m_mix(zExp, 0, zFrac == 0u), zFrac);
}

float
s64m_fp64_to_fp32(uvec2 s64m_a)
{
   uvec2 a = unpackUint2x32(s64m_a);
   uint zFrac = 0u;
   uint allZero = 0u;

   uint aFracLo = s64m_extractFloat64FracLo(s64m_a);
   uint aFracHi = s64m_extractFloat64FracHi(s64m_a);
   int aExp = s64m_extractFloat64Exp(s64m_a);
   uint aSign = s64m_extractFloat64Sign(s64m_a);
   if (aExp == 0x7FF) {
      s64m_shortShift64Left(a.y, a.x, 12, a.y, a.x);
      float rval = uintBitsToFloat(aSign | 0x7FC00000u | (a.y>>9));
      rval = s64m_mix(s64m_packFloat32(aSign, 0xFF, 0u), rval, (aFracHi | aFracLo) != 0u);
      return rval;
   }
   s64m_shift64RightJamming(aFracHi, aFracLo, 22, allZero, zFrac);
   zFrac = s64m_mix(zFrac, zFrac | 0x40000000u, aExp != 0);
   return s64m_roundAndPackFloat32(aSign, aExp - 0x381, zFrac);
}

uvec2
s64m_fp32_to_fp64(float f)
{
   uint a = floatBitsToUint(f);
   uint aFrac = a & 0x007FFFFFu;
   int aExp = int((a>>23) & 0xFFu);
   uint aSign = a & 0x80000000u;
   uint zFrac0 = 0u;
   uint zFrac1 = 0u;

   if (aExp == 0xFF) {
      if (aFrac != 0u) {
         uint nanLo = 0u;
         uint nanHi = a<<9;
         s64m_shift64Right(nanHi, nanLo, 12, nanHi, nanLo);
         nanHi |= aSign | 0x7FF80000u;
         return packUint2x32(uvec2(nanLo, nanHi));
      }
      return s64m_packFloat64(aSign, 0x7FF, 0u, 0u);
    }

   if (aExp == 0) {
      if (aFrac == 0u)
         return s64m_packFloat64(aSign, 0, 0u, 0u);
      /* Normalize subnormal */
      int shiftCount = s64m_countLeadingZeros32(aFrac) - 8;
      aFrac <<= (shiftCount & 31);
      aExp = 1 - shiftCount;
      --aExp;
   }

   s64m_shift64Right(aFrac, 0u, 3, zFrac0, zFrac1);
   return s64m_packFloat64(aSign, aExp + 0x380, zFrac0, zFrac1);
}

void
s64m_add96(uint a0, uint a1, uint a2,
        uint b0, uint b1, uint b2,
        out uint z0Ptr,
        out uint z1Ptr,
        out uint z2Ptr)
{
   uint z2 = a2 + b2;
   uint carry1 = uint(z2 < a2);
   uint z1 = a1 + b1;
   uint carry0 = uint(z1 < a1);
   uint z0 = a0 + b0;
   z1 += carry1;
   z0 += uint(z1 < carry1);
   z0 += carry0;
   z2Ptr = z2;
   z1Ptr = z1;
   z0Ptr = z0;
}

void
s64m_sub96(uint a0, uint a1, uint a2,
        uint b0, uint b1, uint b2,
        out uint z0Ptr,
        out uint z1Ptr,
        out uint z2Ptr)
{
   uint z2 = a2 - b2;
   uint borrow1 = uint(a2 < b2);
   uint z1 = a1 - b1;
   uint borrow0 = uint(a1 < b1);
   uint z0 = a0 - b0;
   z0 -= uint(z1 < borrow1);
   z1 -= borrow1;
   z0 -= borrow0;
   z2Ptr = z2;
   z1Ptr = z1;
   z0Ptr = z0;
}

uint
s64m_estimateDiv64To32(uint a0, uint a1, uint b)
{
   uint b0;
   uint b1;
   uint rem0 = 0u;
   uint rem1 = 0u;
   uint term0 = 0u;
   uint term1 = 0u;
   uint z;

   if (b <= a0)
      return 0xFFFFFFFFu;
   b0 = b>>16;
   z = (b0<<16 <= a0) ? 0xFFFF0000u : (a0 / b0)<<16;
   umulExtended(b, z, term0, term1);
   s64m_sub64(a0, a1, term0, term1, rem0, rem1);
   while (int(rem0) < 0) {
      z -= 0x10000u;
      b1 = b<<16;
      s64m_add64(rem0, rem1, b0, b1, rem0, rem1);
   }
   rem0 = (rem0<<16) | (rem1>>16);
   z |= (b0<<16 <= rem0) ? 0xFFFFu : rem0 / b0;
   return z;
}

uint
s64m_sqrtOddAdjustments(int index)
{
   uint res = 0u;
   if (index == 0)
      res = 0x0004u;
   if (index == 1)
      res = 0x0022u;
   if (index == 2)
      res = 0x005Du;
   if (index == 3)
      res = 0x00B1u;
   if (index == 4)
      res = 0x011Du;
   if (index == 5)
      res = 0x019Fu;
   if (index == 6)
      res = 0x0236u;
   if (index == 7)
      res = 0x02E0u;
   if (index == 8)
      res = 0x039Cu;
   if (index == 9)
      res = 0x0468u;
   if (index == 10)
      res = 0x0545u;
   if (index == 11)
      res = 0x631u;
   if (index == 12)
      res = 0x072Bu;
   if (index == 13)
      res = 0x0832u;
   if (index == 14)
      res = 0x0946u;
   if (index == 15)
      res = 0x0A67u;

   return res;
}

uint
s64m_sqrtEvenAdjustments(int index)
{
   uint res = 0u;
   if (index == 0)
      res = 0x0A2Du;
   if (index == 1)
      res = 0x08AFu;
   if (index == 2)
      res = 0x075Au;
   if (index == 3)
      res = 0x0629u;
   if (index == 4)
      res = 0x051Au;
   if (index == 5)
      res = 0x0429u;
   if (index == 6)
      res = 0x0356u;
   if (index == 7)
      res = 0x029Eu;
   if (index == 8)
      res = 0x0200u;
   if (index == 9)
      res = 0x0179u;
   if (index == 10)
      res = 0x0109u;
   if (index == 11)
      res = 0x00AFu;
   if (index == 12)
      res = 0x0068u;
   if (index == 13)
      res = 0x0034u;
   if (index == 14)
      res = 0x0012u;
   if (index == 15)
      res = 0x0002u;

   return res;
}

uint
s64m_estimateSqrt32(int aExp, uint a)
{
   uint z;

   int index = int(a>>27 & 15u);
   if ((aExp & 1) != 0) {
      z = 0x4000u + (a>>17) - s64m_sqrtOddAdjustments(index);
      z = ((a / z)<<14) + (z<<15);
      a >>= 1;
   } else {
      z = 0x8000u + (a>>17) - s64m_sqrtEvenAdjustments(index);
      z = a / z + z;
      z = (0x20000u <= z) ? 0xFFFF8000u : (z<<15);
      if (z <= a)
         return uint(int(a)>>1);
   }
   return ((s64m_estimateDiv64To32(a, 0u, z))>>1) + (z>>1);
}

uvec2
s64m_fsqrt64(uvec2 a)
{
   uint zFrac0 = 0u;
   uint zFrac1 = 0u;
   uint zFrac2 = 0u;
   uint doubleZFrac0 = 0u;
   uint rem0 = 0u;
   uint rem1 = 0u;
   uint rem2 = 0u;
   uint rem3 = 0u;
   uint term0 = 0u;
   uint term1 = 0u;
   uint term2 = 0u;
   uint term3 = 0u;
   uvec2 default_nan = uvec2(0xffffffffu,0xffffffffu);

   uint aFracLo = s64m_extractFloat64FracLo(a);
   uint aFracHi = s64m_extractFloat64FracHi(a);
   int aExp = s64m_extractFloat64Exp(a);
   uint aSign = s64m_extractFloat64Sign(a);
   if (aExp == 0x7FF) {
      if ((aFracHi | aFracLo) != 0u)
         return s64m_propagateFloat64NaN(a, a);
      if (aSign == 0u)
         return a;
      return default_nan;
   }
   if (aSign != 0u) {
      if ((uint(aExp) | aFracHi | aFracLo) == 0u)
         return a;
      return default_nan;
   }
   if (aExp == 0) {
      if ((aFracHi | aFracLo) == 0u)
         return s64m_packFloat64(0u, 0, 0u, 0u);
      s64m_normalizeFloat64Subnormal(aFracHi, aFracLo, aExp, aFracHi, aFracLo);
   }
   int zExp = ((aExp - 0x3FF)>>1) + 0x3FE;
   aFracHi |= 0x00100000u;
   s64m_shortShift64Left(aFracHi, aFracLo, 11, term0, term1);
   zFrac0 = (s64m_estimateSqrt32(aExp, term0)>>1) + 1u;
   if (zFrac0 == 0u)
      zFrac0 = 0x7FFFFFFFu;
   doubleZFrac0 = zFrac0 + zFrac0;
   s64m_shortShift64Left(aFracHi, aFracLo, 9 - (aExp & 1), aFracHi, aFracLo);
   umulExtended(zFrac0, zFrac0, term0, term1);
   s64m_sub64(aFracHi, aFracLo, term0, term1, rem0, rem1);
   while (int(rem0) < 0) {
      --zFrac0;
      doubleZFrac0 -= 2u;
      s64m_add64(rem0, rem1, 0u, doubleZFrac0 | 1u, rem0, rem1);
   }
   zFrac1 = s64m_estimateDiv64To32(rem1, 0u, doubleZFrac0);
   if ((zFrac1 & 0x1FFu) <= 5u) {
      if (zFrac1 == 0u)
         zFrac1 = 1u;
      umulExtended(doubleZFrac0, zFrac1, term1, term2);
      s64m_sub64(rem1, 0u, term1, term2, rem1, rem2);
      umulExtended(zFrac1, zFrac1, term2, term3);
      s64m_sub96(rem1, rem2, 0u, 0u, term2, term3, rem1, rem2, rem3);
      while (int(rem1) < 0) {
         --zFrac1;
         s64m_shortShift64Left(0u, zFrac1, 1, term2, term3);
         term3 |= 1u;
         term2 |= doubleZFrac0;
         s64m_add96(rem1, rem2, rem3, 0u, term2, term3, rem1, rem2, rem3);
      }
      zFrac1 |= uint((rem1 | rem2 | rem3) != 0u);
   }
   s64m_shift64ExtraRightJamming(zFrac0, zFrac1, 0u, 10, zFrac0, zFrac1, zFrac2);
   return s64m_roundAndPackFloat64(0u, zExp, zFrac0, zFrac1, zFrac2);
}

uvec2
s64m_ftrunc64(uvec2 s64m_a)
{
   uvec2 a = unpackUint2x32(s64m_a);
   int aExp = s64m_extractFloat64Exp(s64m_a);
   uint zLo;
   uint zHi;

   int unbiasedExp = aExp - 1023;
   int fracBits = 52 - unbiasedExp;
   uint maskLo = s64m_mix(~0u << (fracBits & 31), 0u, fracBits >= 32);
   uint maskHi = s64m_mix(~0u << ((fracBits - 32) & 31), ~0u, fracBits < 33);
   zLo = maskLo & a.x;
   zHi = maskHi & a.y;

   zLo = s64m_mix(zLo, 0u, unbiasedExp < 0);
   zHi = s64m_mix(zHi, 0u, unbiasedExp < 0);
   zLo = s64m_mix(zLo, a.x, unbiasedExp > 52);
   zHi = s64m_mix(zHi, a.y, unbiasedExp > 52);
   return packUint2x32(uvec2(zLo, zHi));
}

uvec2
s64m_ffloor64(uvec2 a)
{
   /* The big assumption is that when 'a' is NaN, s64m_ftrunc(a) returns a.  Based
    * on that assumption, NaN values that don't have the sign bit will safely
    * return NaN (identity).  This is guarded by RELAXED_NAN_PROPAGATION
    * because otherwise the NaN should have the "signal" bit set.  The
    * s64m_fadd64 will ensure that occurs.
    */
   bool is_positive =
#if defined RELAXED_NAN_PROPAGATION
      int(unpackUint2x32(a).y) >= 0
#else
      s64m_fge64(a, uvec2(0u,0u))
#endif
      ;
   uvec2 tr = s64m_ftrunc64(a);

   if (is_positive || s64m_feq64(tr, a)) {
      return tr;
   } else {
      return s64m_fadd64(tr, uvec2(0x00000000u,0xbff00000u) /* -1.0 */);
   }
}

uvec2
s64m_fmin64(uvec2 a, uvec2 b)
{
   /* This weird layout matters.  Doing the "obvious" thing results in extra
    * flow control being inserted to implement the short-circuit evaluation
    * rules.  Flow control is bad!
    */
   bool b_nan = s64m_is_nan(b);
   bool a_lt_b = s64m_flt64_nonnan_minmax(a, b);
   bool a_nan = s64m_is_nan(a);

   return (b_nan || a_lt_b) && !a_nan ? a : b;
}

uvec2
s64m_fmax64(uvec2 a, uvec2 b)
{
   /* This weird layout matters.  Doing the "obvious" thing results in extra
    * flow control being inserted to implement the short-circuit evaluation
    * rules.  Flow control is bad!
    */
   bool b_nan = s64m_is_nan(b);
   bool a_lt_b = s64m_flt64_nonnan_minmax(a, b);
   bool a_nan = s64m_is_nan(a);

   return (b_nan || a_lt_b) && !a_nan ? b : a;
}
/* Explicit public API. IEEE binary64 low word precedes high word. */
bool s64_eq(uvec2 a,uvec2 b){return s64m_feq64(a,b);}
bool s64_lt(uvec2 a,uvec2 b){return s64m_flt64(a,b);}
bool s64_le(uvec2 a,uvec2 b){return s64m_fge64(b,a);}
bool s64_gt(uvec2 a,uvec2 b){return s64m_flt64(b,a);}
bool s64_ge(uvec2 a,uvec2 b){return s64m_fge64(a,b);}
bool s64_finite(uvec2 a){return (a.y&0x7ff00000u)!=0x7ff00000u;}
bool s64_zero(uvec2 a){return (a.x|(a.y&0x7fffffffu))==0u;}
uvec2 s64_neg(uvec2 a){return s64m_fneg64(a);}
uvec2 s64_abs(uvec2 a){return s64m_fabs64(a);}
uvec2 s64_add(uvec2 a,uvec2 b){return s64m_fadd64(a,b);}
uvec2 s64_sub(uvec2 a,uvec2 b){return s64m_fadd64(a,s64m_fneg64(b));}
uvec2 s64_mul(uvec2 a,uvec2 b){return s64m_fmul64(a,b);}
uvec2 s64_sqrt(uvec2 a){return s64m_fsqrt64(a);}
uvec2 s64_min(uvec2 a,uvec2 b){
    if(s64m_is_nan(a)||s64m_is_nan(b))return s64m_propagateFloat64NaN(a,b);
    return s64m_flt64_nonnan_minmax(a,b)?a:b;
}
uvec2 s64_max(uvec2 a,uvec2 b){
    if(s64m_is_nan(a)||s64m_is_nan(b))return s64m_propagateFloat64NaN(a,b);
    return s64m_flt64_nonnan_minmax(a,b)?b:a;
}
uvec2 s64_trunc(uvec2 a){
    uvec2 z=s64m_ftrunc64(a);
    return s64_zero(z)?uvec2(0u,a.y&0x80000000u):z;
}
uvec2 s64_floor(uvec2 a){return s64_zero(a)?a:s64m_ffloor64(a);}
uvec2 s64_fromFloat(float a){return s64m_fp32_to_fp64(a);}
float s64_toFloat(uvec2 a){return s64m_fp64_to_fp32(a);}
uvec2 s64_fromInt(int a){return s64m_int_to_fp64(a);}
uvec2 s64_fromUint(uint a){return s64m_uint_to_fp64(a);}
int s64_toInt(uvec2 a){return s64m_fp64_to_int(a);}

/* Exact restoring division of finite significands, with three rounding bits
 * plus sticky. Every loop uses uint32 pair arithmetic; no reciprocal estimate.
 * The normalized quotient [1,2) has 53 significant bits followed by G/R/S.
 * s64m_roundAndPackFloat64 performs RNE and handles subnormal/overflow output. */
bool s64_wordsLess(uvec2 a,uvec2 b){return a.y<b.y||(a.y==b.y&&a.x<b.x);}
uvec2 s64_wordsShift(uvec2 a){return uvec2(a.x<<1,(a.y<<1)|(a.x>>31));}
uvec2 s64_wordsSubtract(uvec2 a,uvec2 b){return uvec2(a.x-b.x,a.y-b.y-uint(a.x<b.x));}
uvec2 s64_div(uvec2 a,uvec2 b){
    uint sign=(a.y^b.y)&0x80000000u;
    int ae=int((a.y>>20)&0x7ffu),be=int((b.y>>20)&0x7ffu);
    uint ahi=a.y&0xfffffu,bhi=b.y&0xfffffu,alo=a.x,blo=b.x;
    if(ae==0x7ff||be==0x7ff){
        if((ae==0x7ff&&(ahi|alo)!=0u)||(be==0x7ff&&(bhi|blo)!=0u))return s64m_propagateFloat64NaN(a,b);
        if(ae==0x7ff&&be==0x7ff)return uvec2(0u,0x7ff80000u);
        return ae==0x7ff?uvec2(0u,sign|0x7ff00000u):uvec2(0u,sign);
    }
    bool az=(uint(ae)|ahi|alo)==0u,bz=(uint(be)|bhi|blo)==0u;
    if(az&&bz)return uvec2(0u,0x7ff80000u);
    if(bz)return uvec2(0u,sign|0x7ff00000u);
    if(az)return uvec2(0u,sign);
    if(ae==0)s64m_normalizeFloat64Subnormal(ahi,alo,ae,ahi,alo);
    if(be==0)s64m_normalizeFloat64Subnormal(bhi,blo,be,bhi,blo);
    ahi|=0x100000u;bhi|=0x100000u;
    uvec2 rem=uvec2(alo,ahi),den=uvec2(blo,bhi),quot=uvec2(0u);
    int exponent=ae-be+0x3fe;
    if(s64_wordsLess(rem,den)){rem=s64_wordsShift(rem);exponent--;}
    // quotient bits 55..0: leading bit, 52 fraction bits, three round bits.
    for(int bit=55;bit>=0;bit--){
        quot=s64_wordsShift(quot);
        if(!s64_wordsLess(rem,den)){rem=s64_wordsSubtract(rem,den);quot.x|=1u;}
        rem=s64_wordsShift(rem);
    }
    uint sticky=uint((rem.x|rem.y)!=0u);
    uint hi=quot.y>>3,lo=(quot.x>>3)|(quot.y<<29);
    uint extra=(quot.x&7u)<<29;extra|=sticky;
    return s64m_roundAndPackFloat64(sign,exponent,hi,lo,extra);
}

#endif
