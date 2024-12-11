#ifndef DRIVER_H
#define DRIVER_H
#include <nlohmann/json.hpp>
using json = nlohmann::json;

class Driver {
private:
  int testnumCounter = 0;
  int time = 0;
  int testnum = 0;
  int timeout = 0;
public:
  Driver(json &j);
  Driver(){}
  ~Driver(){}
  void generate(uint8_t *dst);
  uint8_t watchdog();
};

#endif
