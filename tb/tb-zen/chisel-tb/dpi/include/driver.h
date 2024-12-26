#ifndef DRIVER_H
#define DRIVER_H
#include <nlohmann/json.hpp>
using json = nlohmann::json;

class Driver {
private:
  int timeout = 0;
public:
  Driver(json &j);
  Driver(){}
  ~Driver(){}
  uint8_t watchdog();
};

#endif 