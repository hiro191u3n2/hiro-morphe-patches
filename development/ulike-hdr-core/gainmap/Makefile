CXX ?= g++
CXXFLAGS ?= -std=c++17 -O2 -Wall -Wextra -Wpedantic -Werror

.PHONY: check sanitize clean
check: build/test_gainmap
	./build/test_gainmap

build/test_gainmap: gainmap.cpp gainmap.hpp test_gainmap.cpp
	mkdir -p build
	$(CXX) $(CXXFLAGS) gainmap.cpp test_gainmap.cpp -o $@

sanitize:
	mkdir -p build
	$(CXX) -std=c++17 -O1 -g -Wall -Wextra -Wpedantic -Werror -fsanitize=address,undefined -fno-omit-frame-pointer gainmap.cpp test_gainmap.cpp -o build/test_gainmap_sanitize
	./build/test_gainmap_sanitize

clean:
	rm -rf build
