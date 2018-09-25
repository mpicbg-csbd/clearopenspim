package net.clearcontrol.devices.stages.picard;

import clearcontrol.core.device.VirtualDevice;
import clearcontrol.core.log.LoggingFeature;
import clearcontrol.core.variable.Variable;
import clearcontrol.devices.stages.BasicStageInterface;
import clearcontrol.gui.jfx.custom.visualconsole.VisualConsoleInterface;
import pi4dusb.PiUsbLinearStage;

/**
 * LinearPicardStage
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 09 2018
 */
public class LinearPicardStage  extends VirtualDevice implements
        VisualConsoleInterface,
        LoggingFeature,
        BasicStageInterface
{
    Variable<Double> position = new Variable<Double>("Position", 0.0);

    int serial = 0;

    PiUsbLinearStage piStage;

    /**
     * INstanciates a virtual device with a given name
     *
     * @param pDeviceName device name
     */
    public LinearPicardStage(String pDeviceName, int serial) {
        super(pDeviceName);
        this.serial = serial;
    }

    @Override
    public boolean open() {
        super.open();

        piStage = new PiUsbLinearStage(serial);
        return true;
    }

    @Override
    public boolean close() {
        super.close();

        piStage.dispose();
        return true;
    }

    @Override
    public boolean moveBy(double pDistance, boolean pWaitToFinish) {
        piStage.setPosition((int) (piStage.getPosition() + pDistance));
        // todo: wait until motion stopped
        return true;
    }

    @Override
    public Variable<Double> getPositionVariable() {
        position.set((double) piStage.getPosition());
        return position;
    }
}
