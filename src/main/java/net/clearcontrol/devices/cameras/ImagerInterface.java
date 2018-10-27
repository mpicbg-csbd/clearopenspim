package net.clearcontrol.devices.cameras;

import andorsdkj.AndorSdkJException;
import clearcl.ClearCLImage;
import coremem.ContiguousMemoryInterface;

/**
 * ImagerInterface
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 10 2018
 */
public interface ImagerInterface {

    public void setExposureTimeInSeconds(double exposureTimeInSeconds);

    public void setImageWidth(long imageWidth);

    public void setImageHeight(long imageHeight);

    public void setBinning(int binning);

    public boolean connect();

    public boolean disconnect();

    void setMemoryInterface(ContiguousMemoryInterface memoryInterface);

    public double getPixelSizeInMicrons();

    boolean acquire();
}
