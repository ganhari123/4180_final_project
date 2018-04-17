#include "mbed.h"
#include "rtos.h"

RawSerial  pc(USBTX, USBRX);
RawSerial  bluetooth(p13,p14);

Thread t_imu;
Thread t_heartrate;

volatile char heartrate;
volatile char has_fallen; 

/*****
TODO:
in my thread, read the requesite number of times from the IMU and run function for has_fallen set.

in the main loop, fire message to phone using "G" as the header then the hearrate, then the has_fallen.
main will wait .25 before firing another (letting each other thread run as quickly as they want to)

*****/
 
void imu() {
    while(1) {
        Thread::wait(1000);
    }
}
 
void heartrate() {
    while(1) {
        Thread::wait(1000);
    }
}

void bluetooth_recv()
{
    while(bluetooth.readable()) {
        pc.putc(bluetooth.getc());
    }
}

void pc_recv()
{
    while(pc.readable()) {
        bluetooth.putc(pc.getc());
    }
}

int main()
{
    pc.baud(9600);
    bluetooth.baud(9600);

    pc.attach(&pc_recv, Serial::RxIrq);
    bluetooth.attach(&bluetooth_recv, Serial::RxIrq);

    t_imu.start(imu);
    t_heartrate.start(heartrate);

    //Main thread will fire a packet every .25 seconds
    while(1) {
        Thread::wait(1000);
    }
}