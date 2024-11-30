#include <iostream>
#include <numeric>

extern "C" {
    int gcd(unsigned int a,unsigned int b) {
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }
    void ref_generate(unsigned int a,unsigned int b,unsigned int* c){
        *c = gcd(a, b);
    }
}