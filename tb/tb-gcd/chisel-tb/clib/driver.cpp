#include <common.h>
#include <driver.h>
uint16_t gcd(uint16_t x, uint16_t y);
void write_to_pointer(uint8_t* dst, const uint8_t* data, size_t len);
#define DATA_WIDTH 16 //TODO: read from config

Driver::Driver(json &config)
{
  std::cout << "config file: " << CONFIG_FILE << std::endl;
  std::cout << config.dump(4) << std::endl;
  testnum = config["testSize"];
  timeout = config["timeout"];
  srand(std::time(NULL));
}

uint8_t Driver::watchdog()
{
  const int WATCHDOG_CONTINUE = 0;
  const int WATCHDOG_TIMEOUT = 1;
  const int WATCHDOG_FINISH = 2;
  s_vpi_time vpi_time;
  vpi_time.type = vpiSimTime; //type

  vpi_get_time(NULL, &vpi_time);

  uint64_t high = vpi_time.high;
  uint64_t time_now = (high << 32) + vpi_time.low;
  if(time_now > timeout){
    return WATCHDOG_TIMEOUT;
  }
  else if(testnumCounter >= testnum){
    return WATCHDOG_FINISH;
  }else{
    printf("Current simulation time: %u:%u, %llu\n", vpi_time.high, vpi_time.low, time_now);
    return WATCHDOG_CONTINUE;
  }
  return 0;
}

void Driver::generate(uint8_t *dst)
{

  // generate x, y randomly
  uint16_t x = rand() % ((uint16_t)1 << DATA_WIDTH);
  uint16_t y = rand() % ((uint16_t)1 << DATA_WIDTH);
  uint16_t result = gcd(x, y);

  // print
  std::cout << "[Generate] " << testnumCounter << ": x = " << x << ", y = " << y << ", result = " << result << std::endl;

  // [ result | y | x | valid ]
  uint8_t data[3 * sizeof(uint16_t) + 1] = {0}; // valid (1 byte) + 3 * 64-bit numbers

  // write in
  memcpy(&data[0], &result, sizeof(uint16_t));
  memcpy(&data[sizeof(uint16_t)], &y, sizeof(uint16_t));
  memcpy(&data[2 * sizeof(uint16_t)], &x, sizeof(uint16_t));

  // generate valid randomly(95%)
  data[3 * sizeof(uint16_t)] = (rand() % 100 < 95) ? 1 : 0;

  testnumCounter++;

  // wirte back
  write_to_pointer((uint8_t*)dst, data, sizeof(data));
}
// helper function
uint16_t gcd(uint16_t x, uint16_t y) 
{
  while (y != 0) {
      uint16_t temp = y;
      y = x % y;
      x = temp;
  }
  return x;
}
void write_to_pointer(uint8_t* dst, const uint8_t* data, size_t len) 
{
    memcpy(dst, data, len);
}

//#include <type_traits> // std::conditional
//
//// 定义一个类型选择器
//template <std::size_t Width>
//struct SelectUIntType {
//    using type = typename std::conditional<
//        (Width <= 8), uint8_t,
//        typename std::conditional<
//            (Width <= 16), uint16_t,
//            typename std::conditional<
//                (Width <= 32), uint32_t,
//                uint64_t>::type>::type>::type;
//};
//
//// 宏定义
//#define DATA_WIDTH 16
//
//// 根据 DATA_WIDTH 选择类型
//using UIntType = typename SelectUIntType<DATA_WIDTH>::type;
