package openspim.imaging.gui;

import clearcontrol.gui.jfx.custom.gridpane.CustomGridPane;
import clearcontrol.microscope.lightsheet.imaging.stagemotionacquisition.StageMotionAcquisitionInstruction;
import openspim.imaging.StageMotionAcquisitionWithAndorSDKImagingInstruction;

/**
 * StageMotionAcquisitionInstructionPanel
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
