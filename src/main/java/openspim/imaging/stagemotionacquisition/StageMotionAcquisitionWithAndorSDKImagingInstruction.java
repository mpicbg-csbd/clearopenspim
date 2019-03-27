package openspim.imaging.stagemotionacquisition;

import clearcontrol.microscope.lightsheet.LightSheetMicroscope;
import net.clearcontrol.devices.cameras.andorsdk.AndorImager;
import net.haesleinhuepf.clij.clearcl.ClearCLImage;

public class StageMotionAcquisitionWithAndorSDKImagingInstruction extends StageMotionAcquisitionInstruction2 {
    /**
     * INstanciates a virtual device with a given name
     *
     * @param pLightSheetMicroscope
     */
    public StageMotionAcquisitionWithAndorSDKImagingInstruction(LightSheetMicroscope pLightSheetMicroscope) {
        super("Acquisition: Stage motion with Andor acquisition", pLightSheetMicroscope);
    }

    @Override
    protected ClearCLImage acquireSinglePlane(double z) {
        AndorImager imager = getLightSheetMicroscope().getDevice(AndorImager.class, 0);
        imager.setExposureTimeInSeconds(getExposureTimeInSeconds().get());
        return imager.acquire();
    }

    @Override
    public StageMotionAcquisitionWithAndorSDKImagingInstruction copy() {
        return new StageMotionAcquisitionWithAndorSDKImagingInstruction(getLightSheetMicroscope());
    }
}
