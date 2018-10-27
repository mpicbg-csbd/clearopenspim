package net.clearcontrol.devices.cameras.simulation;

import clearcl.imagej.ClearCLIJ;
import clearcontrol.core.device.VirtualDevice;
import clearcontrol.stack.StackInterface;
import coremem.ContiguousMemoryInterface;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import net.clearcontrol.devices.cameras.ImagerInterface;

/**
 * SimulatedImager
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 10 2018
 */
public class SimulatedImager extends VirtualDevice implements ImagerInterface {
    private ContiguousMemoryInterface memoryInterface;
    ImagePlus imp;
    int planeIndex = 0;

    /**
     * INstanciates a virtual device with a given name
     *
     */
    public SimulatedImager() {
        super("Simulated Imager");
        imp = IJ.openImage("src/main/resource/simdata.tif");
    }

    @Override
    public void setExposureTimeInSeconds(double exposureTimeInSeconds) {

    }

    @Override
    public void setImageWidth(long imageWidth) {

    }

    @Override
    public void setImageHeight(long imageHeight) {

    }

    @Override
    public void setBinning(int binning) {

    }

    @Override
    public boolean connect() {
        return true;
    }

    @Override
    public boolean disconnect() {
        return true;
    }

    @Override
    public void setMemoryInterface(ContiguousMemoryInterface memoryInterface) {
        this.memoryInterface = memoryInterface;


    }

    @Override
    public boolean acquire() {

        ImagePlus imp = new Duplicator().run(this.imp, planeIndex + 1, planeIndex + 1);
        planeIndex++;
        if (planeIndex >= this.imp.getNSlices()) {
            planeIndex = 0;
        }

        ClearCLIJ clij = ClearCLIJ.getInstance();
        StackInterface stack = clij.converter(imp).getStack();

        memoryInterface.copyFrom(stack.getContiguousMemory());

        return true;

    }
}
