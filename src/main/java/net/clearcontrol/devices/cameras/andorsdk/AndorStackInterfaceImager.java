package net.clearcontrol.devices.cameras.andorsdk;

import andorsdkj.AndorCamera;
import andorsdkj.AndorSdkJ;
import andorsdkj.AndorSdkJException;
import andorsdkj.ImageBuffer;
import andorsdkj.enums.CycleMode;
import andorsdkj.enums.ReadOutRate;
import andorsdkj.enums.TriggerMode;
import clearcl.ClearCLImage;
import clearcl.enums.ImageChannelDataType;
import clearcl.imagej.ClearCLIJ;
import clearcontrol.core.device.VirtualDevice;
import clearcontrol.core.variable.Variable;
import clearcontrol.devices.lasers.LaserDeviceInterface;
import clearcontrol.stack.OffHeapPlanarStack;
import clearcontrol.stack.StackInterface;
import coremem.ContiguousMemoryInterface;
import coremem.enums.NativeTypeEnum;
import net.clearcontrol.devices.cameras.ImagerInterface;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * AndorStackInterfaceImager
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 10 2018
 */
public class AndorStackInterfaceImager extends VirtualDevice implements ImagerInterface {

    AndorSdkJ lAsdkj;
    AndorCamera lCamera;
    int cameraIndex;


    Variable<Boolean> laserTrigger;

    long imageWidth = 2560;
    long imageHeight = 2160;
    double exposureInSeconds = 0.1;
    int binning = 1;
    private ContiguousMemoryInterface memoryInterface;
    private double pixelSizeInMicrons;

    public AndorStackInterfaceImager(int cameraIndex) {
        super("Andor stack imager");
        this.cameraIndex = cameraIndex;
    }

    public boolean connect() {

        try {
            lAsdkj = new AndorSdkJ();
            lAsdkj.open();
            lCamera = lAsdkj.openCamera(cameraIndex);

            lCamera.setOverlapReadoutMode(true);
            lCamera.set16PixelEncoding();
            lCamera.setReadoutRate(ReadOutRate._280_MHz);
            lCamera.allocateAndQueueAlignedBuffers(5);
            lCamera.setTriggeringMode(TriggerMode.SOFTWARE);
            lCamera.setExposureTimeInSeconds(exposureInSeconds);
            lCamera.setCycleMode(CycleMode.CONTINUOUS);
            lCamera.setBinning(binning);

            System.out.println("is overlap? - " + lCamera.getOverlapReadoutMode());
        } catch (Exception e) {
            e.printStackTrace();
            lAsdkj = null;
            lCamera = null;
            return false;
        }
        return true;
    }

    public boolean acquire() {
        if (lCamera == null) {
            return false;
        }

        if (laserTrigger != null) {
            System.out.println("No laser device detected!");
        }

        laserTrigger.set(true);
        try {
            lCamera.startAcquisition();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        ScheduledThreadPoolExecutor lExecutor = new ScheduledThreadPoolExecutor(20);

        int lNumTimePoints = 1;
        double t0 = System.nanoTime();

        double t11;


        Future<?> f = lExecutor.scheduleAtFixedRate(() -> {
            try {



                lCamera.SoftwareTrigger();


                double t1 = System.nanoTime();
                System.out.println(String.format("---> Trigger: %.3f", (t1 - t0) * 1e-6));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 100, TimeUnit.MILLISECONDS);

        System.out.println();


        for (int i = 0; i < lNumTimePoints; i++) {
            int ind = i;


            double t1 = System.nanoTime();


            // wait for / re
            ImageBuffer lImageBuffer = null;
            try {
                lImageBuffer = lCamera.waitForBuffer(10, TimeUnit.SECONDS);
            } catch (AndorSdkJException e) {
                e.printStackTrace();
                return false;
            }


            double t25 = System.nanoTime();


            System.out.println(String.format("Wait for buffer %d took %.3f ms.", i, (t25 - t1) * 1e-6));
            try{
                lCamera.enqueueBuffer(lImageBuffer);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            System.out.println("Buffer received with " + lImageBuffer.getImageSizeInBytes() + " bytes");

            laserTrigger.set(false);

            OffHeapPlanarStack stack = new OffHeapPlanarStack(true, 0, NativeTypeEnum.UnsignedShort, 1, imageWidth, imageHeight, 1);

            //ClearCLIJ clij = ClearCLIJ.getInstance();
            //ClearCLImage clByteImage = clij.createCLImage(new long[]{imageWidth, imageHeight}, ImageChannelDataType.UnsignedInt16);

            byte[] bytes = lImageBuffer.getPointer().getBytes(lImageBuffer.getImageSizeInBytes());

            int numberOfPytesPerPixel = 2;
            byte[] filteredBytes = new byte[(int)(imageWidth * imageHeight * numberOfPytesPerPixel)];

            int targetAdress = 0;
            for (int j = 0; j < bytes.length; j++) {
                if ((j+1)%(imageWidth*2+1) != 0) {
                    filteredBytes[targetAdress] = bytes[j];
                    targetAdress++;
                }
            }

            System.out.println("Read bytes: " + lImageBuffer.getImageSizeInBytes());
            //System.out.println("Avail mem:  " + clByteImage.getSizeInBytes());
            System.out.println("bytes    :  " + bytes.length);
            System.out.println("filtere  :  " + filteredBytes.length);

            stack.getContiguousMemory().copyFrom(filteredBytes);

            System.out.println();
        }


        double t2 = System.nanoTime();
        f.cancel(false);

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        lExecutor.shutdownNow();

        try {
            lCamera.stopAcquisition();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean disconnect() {
        if (lCamera == null) {
            return false;
        }
        try {
            lCamera.stopAcquisition();
            lCamera.close();

            lAsdkj.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void setMemoryInterface(ContiguousMemoryInterface memoryInterface) {
        this.memoryInterface = memoryInterface;
    }

    public void setExposureTimeInSeconds(double exposureTimeInSeconds) {
        this.exposureInSeconds = exposureTimeInSeconds;
        if (lCamera != null ) {
            try {
                lCamera.setExposureTimeInSeconds(exposureTimeInSeconds);
            } catch (AndorSdkJException e) {
                e.printStackTrace();
            }
        }
    }

    public void setImageWidth(long imageWidth) {
        this.imageWidth = imageWidth;
    }

    public void setImageHeight(long imageHeight) {
        this.imageHeight = imageHeight;
    }

    public void setBinning(int binning) {
        this.binning = binning;
    }

    public void setLaserTriggerOnVariable(Variable<Boolean> laserTrigger) {
        this.laserTrigger = laserTrigger;
    }


    public double getPixelSizeInMicrons() {
        return pixelSizeInMicrons;
    }

    public void setPixelSizeInMicrons(double pixelSizeInMicrons) {
        this.pixelSizeInMicrons = pixelSizeInMicrons;
    }
}
