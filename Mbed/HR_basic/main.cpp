
#include "rtos.h"
#include "mbed.h"
#include "LSM9DS1.h"

#include "algorithm.h"
#include "MAX30102.h"

#include <math.h>

#define MAX_BRIGHTNESS 255

RawSerial  bluetooth(p13,p14);

Thread t_imu;
Thread t_heartrate;

volatile char heartrate;
volatile char has_fallen = 0; 


uint32_t aun_ir_buffer[500]; //IR LED sensor data
int32_t n_ir_buffer_length;    //data length
uint32_t aun_red_buffer[500];    //Red LED sensor data
int32_t n_sp02; //SPO2 value
int8_t ch_spo2_valid;   //indicator to show if the SP02 calculation is valid
int32_t n_heart_rate;   //heart rate value
int8_t  ch_hr_valid;    //indicator to show if the heart rate calculation is valid
uint8_t uch_dummy;

Serial pc(USBTX, USBRX);    //initializes the serial port

PwmOut led(LED1);    //initializes the pwm output that connects to the on board LED
DigitalIn INT(p26);  //pin P20 connects to the interrupt output pin of the MAX30102

LSM9DS1 imu(p9, p10, 0xD6, 0x3C);

void getHR(void);

void imu_fall_check() {
    while(1) {
    //Calculate the magnitude of the accel vector. If above 4.0 (from research graphs) we will mark it as a fall.
    imu.readMag();
    float accel_mag = imu.ax*imu.ax + imu.ay*imu.ay + imu.az*imu.az;
    if(accel_mag > 16.0) {
        has_fallen = 1;  
        //Wait for half a second to make sure that the fall gets sent properly by the main thread.
        Thread::wait(500);
    } else {
        has_fallen = 0;
    }
    }
}

// the setup routine runs once when you press reset:
int main() { 

    //int hearRate = getHR();
    
    pc.baud(9600);
    bluetooth.baud(9600);

    //IMU init stuff
    imu.begin();
    if( !imu.begin() ) {
    pc.printf("Failed to start IMU.");
    }
    imu.calibrate();

    //Start threads
    t_imu.start(imu_fall_check);
    t_heartrate.start(getHR);

    //Main thread will fire a packet every .25 seconds
    while(1) {
		Thread::wait(250);
    bluetooth.putc('G');
    bluetooth.putc(heartrate);
    bluetooth.putc(has_fallen);
        
    }
}



void getHR(void)
{
     uint32_t un_min, un_max, un_prev_data;  //variables to calculate the on-board LED brightness that reflects the heartbeats
    int i;
    int32_t n_brightness;
    float f_temp;
    
    maxim_max30102_reset(); //resets the MAX30102
    // initialize serial communication at 115200 bits per second:
    pc.baud(9600);
    pc.format(8,SerialBase::None,1);
    
    //read and clear status register
    maxim_max30102_read_reg(0,&uch_dummy);
    uch_dummy=getchar();
    
    maxim_max30102_init();  //initializes the MAX30102
        
        
    n_brightness=0;
    un_min=0x3FFFF;
    un_max=0;
  
    n_ir_buffer_length=500; //buffer length of 100 stores 5 seconds of samples running at 100sps
    
    //read the first 500 samples, and determine the signal range
    for(i=0;i<n_ir_buffer_length;i++)
    {
        while(INT.read()==1)//wait until the interrupt pin asserts
        {
            Thread::wait(10);
        }   
        
        maxim_max30102_read_fifo((aun_red_buffer+i), (aun_ir_buffer+i));  //read from MAX30102 FIFO
            
        if(un_min>aun_red_buffer[i])
            un_min=aun_red_buffer[i];    //update signal min
        if(un_max<aun_red_buffer[i])
            un_max=aun_red_buffer[i];    //update signal max
        pc.printf("red=");
        pc.printf("%i", aun_red_buffer[i]);
        pc.printf(", ir=");
        pc.printf("%i\n\r", aun_ir_buffer[i]);
    }
    un_prev_data=aun_red_buffer[i];
    
    
    //calculate heart rate and SpO2 after first 500 samples (first 5 seconds of samples)
    maxim_heart_rate_and_oxygen_saturation(aun_ir_buffer, n_ir_buffer_length, aun_red_buffer, &n_sp02, &ch_spo2_valid, &n_heart_rate, &ch_hr_valid); 
    
    //Continuously taking samples from MAX30102.  Heart rate and SpO2 are calculated every 1 second
    while(1)
    {
        i=0;
        un_min=0x3FFFF;
        un_max=0;
        
        //dumping the first 100 sets of samples in the memory and shift the last 400 sets of samples to the top
        for(i=100;i<500;i++)
        {
            aun_red_buffer[i-100]=aun_red_buffer[i];
            aun_ir_buffer[i-100]=aun_ir_buffer[i];
            
            //update the signal min and max
            if(un_min>aun_red_buffer[i])
            un_min=aun_red_buffer[i];
            if(un_max<aun_red_buffer[i])
            un_max=aun_red_buffer[i];
        }
        
        //take 100 sets of samples before calculating the heart rate.
        for(i=400;i<500;i++)
        {
            un_prev_data=aun_red_buffer[i-1];
            while(INT.read()==1);
            maxim_max30102_read_fifo((aun_red_buffer+i), (aun_ir_buffer+i));
        
            if(aun_red_buffer[i]>un_prev_data)
            {
                f_temp=aun_red_buffer[i]-un_prev_data;
                f_temp/=(un_max-un_min);
                f_temp*=MAX_BRIGHTNESS;
                n_brightness-=(int)f_temp;
                if(n_brightness<0)
                    n_brightness=0;
            }
            else
            {
                f_temp=un_prev_data-aun_red_buffer[i];
                f_temp/=(un_max-un_min);
                f_temp*=MAX_BRIGHTNESS;
                n_brightness+=(int)f_temp;
                if(n_brightness>MAX_BRIGHTNESS)
                    n_brightness=MAX_BRIGHTNESS;
            }

            led.write(1-(float)n_brightness/256);
            
            if(n_heart_rate > 255)
                heartrate = 255;
            else if(n_heart_rate < 0)
                heartrate = 0;
            else 
                heartrate = n_heart_rate;

            //send samples and calculation result to terminal program through UART
            pc.printf("red=");
            pc.printf("%i", aun_red_buffer[i]);
            pc.printf(", ir=");
            pc.printf("%i", aun_ir_buffer[i]);
            pc.printf(", HR=%i, ", n_heart_rate); 
            pc.printf("HRvalid=%i, ", ch_hr_valid);
            pc.printf("SpO2=%i, ", n_sp02);
            pc.printf("SPO2Valid=%i\n\r", ch_spo2_valid);
        }
        maxim_heart_rate_and_oxygen_saturation(aun_ir_buffer, n_ir_buffer_length, aun_red_buffer, &n_sp02, &ch_spo2_valid, &n_heart_rate, &ch_hr_valid); 
    }
    
}
 