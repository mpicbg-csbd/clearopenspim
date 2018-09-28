package net.clearcontrol.devices.stages.picard;

import clearcontrol.core.device.VirtualDevice;
import clearcontrol.core.log.LoggingFeature;
import clearcontrol.core.variable.Variable;
import clearcontrol.devices.stages.BasicStageInterface;
import clearcontrol.gui.jfx.custom.visualconsole.VisualConsoleInterface;
import pi4dusb.PiUsbLinearStage;
import pi4dusb.PiUsbTwisterStage;

/**
 * TwisterPicardStage
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 09 2018
 */
public class TwisterPicardStage extends VirtualDevice implements
        VisualConsoleInterface,
        LoggingFeature,
        BasicStageInterface
{
    Variable<Double> position = new Variable<Double>("Position (deg)", 0.0);

    int serial = 0;
    double stepDistance = 1.8; // degrees according to manual

    PiUsbTwisterStage piStage;

    public TwisterPicardStage(String pDeviceName, int serial, double stepDistance) {
        super(pDeviceName);
        this.serial = serial;
        this.stepDistance = stepDistance;
    }

    public TwisterPicardStage(String pDeviceName, int serial) {
        super(pDeviceName);
        this.serial = serial;
    }


    @Override
    public boolean open() {
        super.open();

        piStage = new PiUsbTwisterStage(serial);
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
        piStage.setPosition((int) (piStage.getPosition() + pDistance / stepDistance));
        // todo: wait until motion stopped
        return true;
    }

    @Override
    public Variable<Double> getPositionVariable() {
        position.set((double) piStage.getPosition() * stepDistance);
        return position;
    }
}
