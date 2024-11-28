#include <iostream>
#include <numeric>

extern "C" {
    int gcd(int a, int b) {
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }
    void ref_generate(int a, int b, int* c){
        *c = gcd(a, b);
    }
}