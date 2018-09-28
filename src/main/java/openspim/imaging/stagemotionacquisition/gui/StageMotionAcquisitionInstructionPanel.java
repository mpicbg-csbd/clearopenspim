package openspim.imaging.stagemotionacquisition.gui;

import clearcontrol.gui.jfx.custom.gridpane.CustomGridPane;
import clearcontrol.microscope.lightsheet.imaging.stagemotionacquisition.StageMotionAcquisitionInstruction;

/**
 * StageMotionAcquisitionInstructionPanel
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 09 2018
 */
public class StageMotionAcquisitionInstructionPanel extends CustomGridPane {
    public StageMotionAcquisitionInstructionPanel(StageMotionAcquisitionInstruction instruction) {

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
