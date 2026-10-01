#include "../gainmap/gainmap.hpp"
#include <algorithm>
#include <cstdio>
#include <exception>
#include <limits>
#include <stdexcept>

// Host bridge only. Python validates provenance and owns immutable buffers.
extern "C" int gm_encode(unsigned width, unsigned height, const double* sdr,
                         const double* hdr, double headroom, double* bounds,
                         unsigned short* output, char* error, unsigned error_size) {
  try {
    using namespace hiro::gainmap;
    if (!width || !height || !sdr || !hdr || !bounds || !output)
      throw std::invalid_argument("invalid bridge buffer");
    if (std::size_t(width) > std::numeric_limits<std::size_t>::max() / height / 3)
      throw std::invalid_argument("image size overflow");
    Frame f{width,height,Primaries::Bt2020,203.0,1,1,1};
    const auto size=std::size_t(width)*height*3;
    Pair p{{f,Samples::Float64,sdr,width*std::size_t(3),size},
           {f,Samples::Float64,hdr,width*std::size_t(3),size}};
    Options o; o.max_display_ratio=headroom;
    const auto m=analyze(p,o);
    for (int c=0;c<3;++c) { bounds[c]=m.min_log2[c];bounds[c+3]=m.max_log2[c]; }
    encode_rows(p,m,[&](std::size_t y,const double* row,std::size_t n){
      quantize(row,output+y*width*3,n,MapBits::Ten);
    });
    return 0;
  } catch (const std::exception& e) {
    if (error && error_size) std::snprintf(error,error_size,"%s",e.what());
    return 1;
  }
}
