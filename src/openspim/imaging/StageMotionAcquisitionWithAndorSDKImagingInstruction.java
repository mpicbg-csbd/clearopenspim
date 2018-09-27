package openspim.imaging;

import clearcl.ClearCLImage;
import clearcontrol.microscope.lightsheet.LightSheetMicroscope;
import clearcontrol.microscope.lightsheet.imaging.stagemotionacquisition.StageMotionAcquisitionInstruction;
import net.clearcontrol.devices.cameras.andorsdk.AndorImager;

public class StageMotionAcquisitionWithAndorSDKImagingInstruction extends StageMotionAcquisitionInstruction {
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
        return imager.acquire();
    }

    @Override
    public StageMotionAcquisitionWithAndorSDKImagingInstruction copy() {
        return new StageMotionAcquisitionWithAndorSDKImagingInstruction(getLightSheetMicroscope());
    }
}
