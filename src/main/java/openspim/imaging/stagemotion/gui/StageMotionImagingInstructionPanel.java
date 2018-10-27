package openspim.imaging.stagemotion.gui;

import clearcontrol.gui.jfx.custom.gridpane.CustomGridPane;
import openspim.imaging.stagemotion.StageMotionImagingInstruction;

/**
 * StageMotionImagingInstructionPanel
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 10 2018
 */
public class StageMotionImagingInstructionPanel extends CustomGridPane {
    public StageMotionImagingInstructionPanel(StageMotionImagingInstruction instruction) {

        int row = 0;
        addIntegerField(instruction.getImageWidth(), row++);
        addIntegerField(instruction.getImageHeight(), row++);

        addIntegerField(instruction.getNumberOfSlices(), row++ );
        addDoubleField(instruction.getSliceDistance(), row++ );

        addIntegerField(instruction.getSleepAtStartingPosition(), row++ );
        addIntegerField(instruction.getSleepBeforeImaging(), row++ );

        addDoubleField(instruction.getExposureTimeInSeconds(), row++);
        addIntegerField(instruction.getBinning(), row++);

    }
}
