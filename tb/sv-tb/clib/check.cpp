#include <iostream>
#include <numeric>

extern "C" {
    void ref_generate(int a, int b, int* c){
        *c = std::gcd(a, b);
    }
}