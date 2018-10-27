package net.clearcontrol.devices.cameras.andorsdk;

import andorsdkj.AndorCamera;
import andorsdkj.AndorSdkJ;
import andorsdkj.AndorSdkJException;
import andorsdkj.ImageBuffer;
import andorsdkj.enums.CycleMode;
import andorsdkj.enums.ReadOutRate;
import andorsdkj.enums.TriggerMode;
import andorsdkj.util.Buffer16ToArray;
import clearcl.ClearCLImage;
import clearcl.enums.ImageChannelDataType;
import clearcl.imagej.ClearCLIJ;
import clearcl.imagej.kernels.Kernels;
import clearcontrol.core.device.VirtualDevice;
import clearcontrol.devices.lasers.LaserDeviceInterface;
import clearcontrol.stack.OffHeapPlanarStack;
import clearcontrol.stack.StackInterface;

import java.nio.ByteBuffer;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AndorImager extends VirtualDevice {

    AndorSdkJ lAsdkj;
    AndorCamera lCamera;
    int cameraIndex;

    ClearCLImage lastAcquiredImage = null;


    LaserDeviceInterface laserDevice;

    int imageWidth = 2560;
    int imageHeight = 2160;
    double exposureInSeconds = 0.1;

    double pixelSizeInMicrons = 0.26;

    public AndorImager(int cameraIndex) {
        super("Andor imager");
        this.cameraIndex = cameraIndex;
    }
    public AndorImager(int cameraIndex, int imageWidth, int imageHeight) {
        super("Andor imager");
        this.cameraIndex = cameraIndex;

        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }
    public AndorImager(int cameraIndex, int imageWidth, int imageHeight, double exposureInSeconds) {
        this(cameraIndex, imageWidth, imageHeight);
        this.exposureInSeconds = exposureInSeconds;
    }

    @Override
    public boolean open() {
        connect();
        return super.open();
    }

    @Override
    public boolean close() {
        disconnect();
        return super.close();
    }

    public ClearCLImage acquire() {
        lastAcquiredImage = null;
        //open();
        image();
        //close();
        return lastAcquiredImage;
    }

    private boolean connect() {

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
            // lCamera.setBinning(2);

            System.out.println("is overlap? - " + lCamera.getOverlapReadoutMode());
        } catch (Exception e) {
            e.printStackTrace();
            lAsdkj = null;
            lCamera = null;
            return false;
        }
        return true;
    }

    private boolean image() {
        if (lCamera == null) {
            return false;
        }

        if (laserDevice != null) {
            System.out.println("No laser device detected!");
        }
        //laserDevice.start();
        laserDevice.getTargetPowerInMilliWattVariable().set(1);

        laserDevice.getLaserOnVariable().set(true);
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

            laserDevice.getLaserOnVariable().set(false);

            ClearCLIJ clij = ClearCLIJ.getInstance();
            ClearCLImage clByteImage = clij.createCLImage(new long[]{imageWidth, imageHeight}, ImageChannelDataType.UnsignedInt16);

            byte[] bytes = lImageBuffer.getPointer().getBytes(lImageBuffer.getImageSizeInBytes());

            int numberOfPytesPerPixel = 2;
            byte[] filteredBytes = new byte[imageWidth * imageHeight * numberOfPytesPerPixel];

            int targetAdress = 0;
            for (int j = 0; j < bytes.length; j++) {
                if ((j+1)%(imageWidth*2+1) != 0) {
                    filteredBytes[targetAdress] = bytes[j];
                    targetAdress++;
                }
            }

            System.out.println("Read bytes: " + lImageBuffer.getImageSizeInBytes());
            System.out.println("Avail mem:  " + clByteImage.getSizeInBytes());
            System.out.println("bytes    :  " + bytes.length);
            System.out.println("filtere  :  " + filteredBytes.length);

            clByteImage.readFrom(filteredBytes, true);

            lastAcquiredImage = clByteImage;


            /*
            // Convert from ImageBuffer to int[][]
            int[][] array = Buffer16ToArray.toArray(lImageBuffer, imageWidth, imageHeight);

            // CLIJ cannot convert from int[][] to ClearCLImage, we need to convert it to char[][][]
            char[][][] charArray = new char[1][][];
            int z = 0;
            charArray[z] = new char[array.length][];
            for (int y = 0; y < charArray[z].length; y++) {
                charArray[z][y] = new char[array[0].length];
                for (int x = 0; x < charArray[z][y].length; x++) {
                    charArray[z][y][x] = (char) array[y][x];
                }
            }

            // Convert from char[][][] to ClearCLImage
            ClearCLIJ clij = ClearCLIJ.getInstance();
            lastAcquiredImage = clij.converter(charArray).getClearCLImage();
            clij.show(lastAcquiredImage, "acq");
            ClearCLImage sliceImage = clij.createCLImage(new long[] {lastAcquiredImage.getWidth(), lastAcquiredImage.getHeight()}, lastAcquiredImage.getChannelDataType());
            Kernels.copySlice(clij, lastAcquiredImage, sliceImage, 0);
            lastAcquiredImage.close();
            lastAcquiredImage = sliceImage;
            */
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

    private boolean disconnect() {
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

    public void setLaserDevice(LaserDeviceInterface laserDevice) {
        this.laserDevice = laserDevice;
    }

    public double getPixelSizeInMicrons() {
        return pixelSizeInMicrons;
    }

    public void setPixelSizeInMicrons(double pixelSizeInMicrons) {
        this.pixelSizeInMicrons = pixelSizeInMicrons;
    }
}
