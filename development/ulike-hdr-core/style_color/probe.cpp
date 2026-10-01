#include "style_color.hpp"
#include <fstream>
#include <iostream>
#include <iomanip>
#include <stdexcept>
#include <vector>
using namespace ulike::color;
static Texture read_texture(const char* path) {
    std::ifstream in(path,std::ios::binary);
    if(!in) throw std::runtime_error("missing texture fixture");
    std::vector<unsigned char> b((std::istreambuf_iterator<char>(in)),{});
    if(b.size()<8)throw std::runtime_error("short fixture");
    auto u32=[&](int i){return std::uint32_t(b[i])|(std::uint32_t(b[i+1])<<8)|
        (std::uint32_t(b[i+2])<<16)|(std::uint32_t(b[i+3])<<24);};
    std::size_t w=u32(0),h=u32(4);
    if(!w||!h||w>4096||h>4096||b.size()!=8+w*h*4)throw std::runtime_error("invalid fixture");
    std::vector<Rgba> pixels;pixels.reserve(w*h);
    for(std::size_t i=8;i<b.size();i+=4)pixels.push_back({b[i]/255.,b[i+1]/255.,b[i+2]/255.,b[i+3]/255.});
    return Texture(w,h,std::move(pixels));
}
int main(int argc,char** argv) {
    try {
        if(argc!=4)throw std::runtime_error("usage: probe final-lut background-lut skin-lut");
        auto lut=read_texture(argv[1]),bg=read_texture(argv[2]),skin=read_texture(argv[3]);
        const Encoding encoding{Primaries::Bt709,Transfer::SrgbEncoded,Range::Full};
        const Contract c{encoding,encoding,Sampling::LinearClampToEdge};
        Rgba p;double alpha,mask;
        std::cout<<std::setprecision(17);
        while(std::cin>>p.r>>p.g>>p.b>>p.a>>alpha>>mask) {
            const auto a=purity_final_lut(p,lut,alpha,c);
            const auto b=skin_background_lut(p,bg,skin,mask,alpha,c);
            std::cout<<a.r<<' '<<a.g<<' '<<a.b<<' '<<a.a<<' '
                     <<b.r<<' '<<b.g<<' '<<b.b<<' '<<b.a<<'\n';
        }
        return 0;
    }catch(const std::exception& e){std::cerr<<e.what()<<'\n';return 1;}
}
