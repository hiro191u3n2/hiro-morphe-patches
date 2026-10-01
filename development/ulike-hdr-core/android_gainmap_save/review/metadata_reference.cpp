#include "ultrahdr/gainmapmetadata.h"
#include <fstream>
#include <iomanip>
#include <iostream>
#include <iterator>

// Calls the unchanged official libultrahdr v2.0.2 parser. No copied decoder implementation.
int main(int argc, char** argv) {
  if (argc != 2) return 2;
  std::ifstream input(argv[1], std::ios::binary);
  if (!input) return 3;
  std::vector<uint8_t> bytes((std::istreambuf_iterator<char>(input)), {});
  ultrahdr::uhdr_gainmap_metadata_frac m{};
  auto error = ultrahdr::uhdr_gainmap_metadata_frac::decodeGainmapMetadata(bytes, &m);
  if (error.error_code != UHDR_CODEC_OK) { std::cerr << error.detail; return 4; }
  std::cout << std::setprecision(17) << "{\"backward\":" << (m.backwardDirection?"true":"false")
            << ",\"use_base\":" << (m.useBaseColorSpace?"true":"false")
            << ",\"base_headroom_log2\":" << double(m.baseHdrHeadroomN)/m.baseHdrHeadroomD
            << ",\"alternate_headroom_log2\":" << double(m.alternateHdrHeadroomN)/m.alternateHdrHeadroomD
            << ",\"channels\":[";
  for(int c=0;c<3;c++) {
    if(c)std::cout << ',';
    std::cout << '[' << double(m.gainMapMinN[c])/m.gainMapMinD[c] << ','
              << double(m.gainMapMaxN[c])/m.gainMapMaxD[c] << ','
              << double(m.gainMapGammaN[c])/m.gainMapGammaD[c] << ','
              << double(m.baseOffsetN[c])/m.baseOffsetD[c] << ','
              << double(m.alternateOffsetN[c])/m.alternateOffsetD[c] << ']';
  }
  std::cout << "]}" << std::endl;
}
