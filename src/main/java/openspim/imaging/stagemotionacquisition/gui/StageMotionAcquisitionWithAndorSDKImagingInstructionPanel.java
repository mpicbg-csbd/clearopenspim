package openspim.imaging.stagemotionacquisition.gui;

import clearcontrol.gui.jfx.custom.gridpane.CustomGridPane;
import openspim.imaging.stagemotionacquisition.StageMotionAcquisitionWithAndorSDKImagingInstruction;

/**
 * StageMotionAcquisitionWithAndorSDKImagingInstructionPanel
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 09 2018
 */
public class StageMotionAcquisitionWithAndorSDKImagingInstructionPanel extends CustomGridPane {
    public StageMotionAcquisitionWithAndorSDKImagingInstructionPanel(StageMotionAcquisitionWithAndorSDKImagingInstruction instruction) {

        int row = 0;
        addIntegerField(instruction.getImageWidth(), row++);
        addIntegerField(instruction.getImageHeight(), row++);

        addIntegerField(instruction.getNumberOfSlices(), row++ );
        addDoubleField(instruction.getSliceDistance(), row++ );

        addIntegerField(instruction.getSleepAtStartingPosition(), row++ );
        addIntegerField(instruction.getSleepBeforeImaging(), row++ );

        addDoubleField(instruction.getExposureTimeInSeconds(), row++);

    }
}
