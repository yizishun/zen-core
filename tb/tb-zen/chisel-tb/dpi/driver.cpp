#include <common.h>
#include <driver.h>

Driver::Driver(json &config)
{
  std::cout << "config file: " << CONFIG_FILE << std::endl;
  std::cout << config.dump(4) << std::endl;
  timeout = config["timeout"];
}

uint8_t Driver::watchdog()
{
  const int WATCHDOG_CONTINUE = 0;
  const int WATCHDOG_TIMEOUT = 1;
  const int WATCHDOG_FINISH = 2;
  s_vpi_time vpi_time;
  vpi_time.type = vpiSimTime;

  vpi_get_time(NULL, &vpi_time);

  uint64_t high = vpi_time.high;
  uint64_t time_now = (high << 32) + vpi_time.low;
  if(time_now > timeout){
    return WATCHDOG_TIMEOUT;
  }
  else{
    printf("Current simulation time: %u:%u, %llu\n", vpi_time.high, vpi_time.low, time_now);
    return WATCHDOG_CONTINUE;
  }
} 