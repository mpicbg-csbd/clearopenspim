package net.clearcontrol.devices.lasers.coherent.obis.demo;

import net.clearcontrol.devices.lasers.coherent.obis.SingleCoherentObisLaserDevice;

/**
 * SingleCoherentObisLaserDemo
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 09 2018
 */
public class SingleCoherentObisLaserDemo {


    public static void main(String... args) throws InterruptedException {
        SingleCoherentObisLaserDevice laser = new SingleCoherentObisLaserDevice("COM8", 115200, 488);
        laser.start();

        laser.getTargetPowerInMilliWattVariable().set(10);

        for (int i = 0; i < 10; i++) {
            laser.getLaserOnVariable().set(true);
            Thread.sleep(1000);
            laser.getLaserOnVariable().set(false);
            Thread.sleep(1000);
        }
    }
}
