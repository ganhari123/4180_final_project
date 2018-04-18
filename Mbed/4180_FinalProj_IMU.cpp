#include "mbed.h"
#include "rtos.h"
#include <math.h>

RawSerial  pc(USBTX, USBRX);
RawSerial  bluetooth(p13,p14);

Thread t_imu;
Thread t_heartrate;

volatile char heartrate;
volatile char has_fallen = 0; 

/*****
TODO:
in my thread, read the requesite number of times from the IMU and run function for has_fallen set.

in the main loop, fire message to phone using "G" as the header then the hearrate, then the has_fallen.
main will wait .25 before firing another (letting each other thread run as quickly as they want to)

*****/

LSM9DS1 imu(p9, p10, 0xD6, 0x3C);
 
void imu_fall_check() {
    while(1) {
	//Calculate the magnitude of the accel vector. If above 4.0 (from research graphs) we will mark it as a fall.
	imu.readMag();
	float accel_mag = sqrt(imu.ax*imu.ax + imu.ay*imu.ay + imu.az*imu.az);
	if(accel_mag > 4.0) {
	    has_fallen = 1;  
	    //Wait for half a second to make sure that the fall gets sent properly by the main thread.
	    Thread::wait(500);
	} else {
	    has_fallen = 0;
	}
    }
}
 
void read_heartrate() {
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

    //IMU init stuff
    imu.begin();
    if( !imu.begin() ) {
	pc.printf("Failed to start IMU.");
    }
    imu.calibrate();

    //Start threads
    t_imu.start(imu_fall_check);
    t_heartrate.start(read_heartrate);

    //Main thread will fire a packet every .25 seconds
    while(1) {
	bluetooth.putc('G');
	bluetooth.putc(heartrate);
	bluetooth.putc(has_fallen);
        Thread::wait(250);
    }
}
