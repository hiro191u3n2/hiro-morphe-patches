CXX ?= g++
CXXFLAGS ?= -std=c++17 -O2 -Wall -Wextra -Werror -pedantic

all: build/libgainmap_bridge.so

build/libgainmap_bridge.so: gainmap_bridge.cpp ../gainmap/gainmap.cpp ../gainmap/gainmap.hpp
	mkdir -p build
	$(CXX) $(CXXFLAGS) -fPIC -shared gainmap_bridge.cpp ../gainmap/gainmap.cpp -o $@

test: all
	python3 test_heif_save.py

clean:
	rm -rf build

.PHONY: all test clean
