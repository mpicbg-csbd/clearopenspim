package openspim.imaging.stagemotion;

import clearcl.ClearCLImage;
import clearcl.enums.ImageChannelDataType;
import clearcl.imagej.ClearCLIJ;
import clearcl.imagej.kernels.Kernels;
import clearcontrol.core.log.LoggingFeature;
import clearcontrol.core.variable.Variable;
import clearcontrol.core.variable.bounded.BoundedVariable;
import clearcontrol.devices.stages.BasicStageInterface;
import clearcontrol.instructions.PropertyIOableInstructionInterface;
import clearcontrol.microscope.lightsheet.LightSheetMicroscope;
import clearcontrol.microscope.lightsheet.imaging.SingleViewPlaneImager;
import clearcontrol.microscope.lightsheet.instructions.LightSheetMicroscopeInstructionBase;
import clearcontrol.microscope.lightsheet.state.InterpolatedAcquisitionState;
import clearcontrol.microscope.lightsheet.warehouse.containers.StackInterfaceContainer;
import clearcontrol.microscope.stacks.StackRecyclerManager;
import clearcontrol.stack.OffHeapPlanarStack;
import clearcontrol.stack.StackInterface;
import clearcontrol.stack.StackRequest;
import clearcontrol.stack.metadata.MetaDataVoxelDim;
import clearcontrol.stack.metadata.StackMetaData;
import coremem.ContiguousMemoryInterface;
import coremem.recycling.RecyclerInterface;
import net.clearcontrol.devices.cameras.ImagerInterface;
import net.clearcontrol.devices.cameras.andorsdk.AndorImager;
import openspim.imaging.stagemotionacquisition.StageMotionAcquisitionInstruction2;

import java.util.concurrent.TimeUnit;

import static clearcontrol.stack.metadata.MetaDataChannel.Channel;

/**
 * StageMotionImagingInstruction
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 10 2018
 */
public class StageMotionImagingInstruction extends LightSheetMicroscopeInstructionBase implements LoggingFeature, PropertyIOableInstructionInterface {

    BoundedVariable<Integer> sleepAtStartingPosition = new BoundedVariable<Integer>("Sleep at start position in ms", 0, 0, Integer.MAX_VALUE);
    BoundedVariable<Integer> sleepBeforeImaging = new BoundedVariable<Integer>("Sleep before imaging in ms", 100, 0, Integer.MAX_VALUE);

    BoundedVariable<Double> sliceDistance = new BoundedVariable<Double>("Slice distance in microns", 2.5, 0.001, Double.MAX_VALUE, 0.001);
    BoundedVariable<Integer> numberOfSlices = new BoundedVariable<Integer>("Number of slices", 10, 0, Integer.MAX_VALUE);

    BoundedVariable<Integer> imageWidth = new BoundedVariable<Integer>("Image width in pixels", 512, 0, Integer.MAX_VALUE);
    BoundedVariable<Integer> imageHeight = new BoundedVariable<Integer>("Image height in pixels", 512, 0, Integer.MAX_VALUE);

    BoundedVariable<Double> exposureTimeInSeconds = new BoundedVariable<Double>("Exposure time in seconds", 0.5, 0.001, Double.MAX_VALUE, 0.0001);
    BoundedVariable<Integer> binning = new BoundedVariable<Integer>("Binning", 1, 0, Integer.MAX_VALUE);

    ClearCLIJ clij = ClearCLIJ.getInstance();

    BasicStageInterface stageZ;
    ImagerInterface imager;

    /**
     * INstanciates a virtual device with a given name
     *
     * @param pLightSheetMicroscope
     * @param stageZ
     * @param imager
     */
    public StageMotionImagingInstruction(LightSheetMicroscope pLightSheetMicroscope, BasicStageInterface stageZ, ImagerInterface imager) {
        super("Acquisition: Stage motion imaging", pLightSheetMicroscope);

        clij = ClearCLIJ.getInstance();
        this.stageZ = stageZ;
        this.imager = imager;
    }

    @Override
    public boolean initialize() {
        return true;
    }

    @Override
    public boolean enqueue(long pTimePoint) {
        if (stageZ == null)
        {
            warning("did not find stage interface!");
            return false;
        }

        double positionBefore = stageZ.getPositionVariable().get();
        double currentPosition = positionBefore;

        InterpolatedAcquisitionState state = (InterpolatedAcquisitionState) getLightSheetMicroscope().getAcquisitionStateManager().getCurrentState();

        double sliceDistanceInMillimeters = this.sliceDistance.get() / 1000.0;
        int numberOfImagesToTake = numberOfSlices.get();

        double imagingRangeZ = numberOfImagesToTake * sliceDistanceInMillimeters;
        double startZ = positionBefore - imagingRangeZ / 2;

        double lightSheetPosition = state.getStackZLowVariable().get().doubleValue() + (state.getStackZHighVariable().get().doubleValue() - state.getStackZLowVariable().get().doubleValue()) / 2;

        // go to startPosition
        stageZ.moveBy(startZ - positionBefore, true);
        sleep(sleepAtStartingPosition.get());

        //ClearCLImage clStack = clij.createCLImage(new long[]{imageWidth.get(), imageHeight.get().longValue(), numberOfImagesToTake}, ImageChannelDataType.UnsignedInt16);
        //OffHeapPlanarStack stack = getLightSheetMicroscope().
        StackRecyclerManager lStackRecyclerManager =
                getLightSheetMicroscope().getDevice(StackRecyclerManager.class,
                        0);
        RecyclerInterface<StackInterface, StackRequest> lRecycler =
                lStackRecyclerManager.getRecycler("warehouse",
                        1024,
                        1024);

        StackInterface stack = lRecycler.getOrWait(1000,
                            TimeUnit.SECONDS,
                            StackRequest.build(new long[]{imageWidth.get(), imageHeight.get().longValue(), numberOfImagesToTake}));


        imager.setExposureTimeInSeconds(getExposureTimeInSeconds().get());
        imager.setBinning(binning.get());
        imager.setImageWidth(imageWidth.get());
        imager.setImageHeight(imageHeight.get());

        long acquisitionRequestTime = System.nanoTime();
        if (imager.connect()) {
            for (int i = 0; i < numberOfImagesToTake; i++) {

                if (i > 0) {
                    stageZ.moveBy(sliceDistanceInMillimeters, true);
                }
                sleep(sleepBeforeImaging.get());

                imager.setMemoryInterface(stack.getContiguousMemory(i));
                imager.acquire();

                if (getLightSheetMicroscope().getTimelapse().getStopSignalVariable().get()) {
                    break;
                }
            }
            imager.disconnect();
        } else {
            warning("Couldn't connect to imager!");
        }

        currentPosition = stageZ.getPositionVariable().get();
        stageZ.moveBy(positionBefore - currentPosition, true);

        StackInterfaceContainer container = new StackInterfaceContainer(pTimePoint) {
            @Override
            public boolean isDataComplete() {
                return true;
            }
        };

        // store the container in the warehouse

        stack.setMetaData(new StackMetaData());
        stack.getMetaData().addEntry(MetaDataVoxelDim.VoxelDimX, imager.getPixelSizeInMicrons());
        stack.getMetaData().addEntry(MetaDataVoxelDim.VoxelDimY, imager.getPixelSizeInMicrons());
        stack.getMetaData().addEntry(MetaDataVoxelDim.VoxelDimZ, sliceDistanceInMillimeters * 1000);
        stack.getMetaData().addEntry(Channel, "C0L0");
        stack.getMetaData().setTimeStampInNanoseconds(acquisitionRequestTime);

        container.put("C0L0", stack);

        getLightSheetMicroscope().getDataWarehouse().put("stagemotionacquisitionstack_" + pTimePoint, container);

        return true;
    }

    private void sleep(int timeInMilliSeconds) {
        if (timeInMilliSeconds < 1) {
            return;
        }
        try {
            Thread.sleep(timeInMilliSeconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public StageMotionImagingInstruction copy() {
        StageMotionImagingInstruction stageMotionImagingInstruction = new StageMotionImagingInstruction(getLightSheetMicroscope(), stageZ, imager);

        stageMotionImagingInstruction.binning.set(binning.get());
        stageMotionImagingInstruction.imageHeight.set(imageHeight.get());
        stageMotionImagingInstruction.imageWidth.set(imageWidth.get());
        stageMotionImagingInstruction.numberOfSlices.set(numberOfSlices.get());
        stageMotionImagingInstruction.exposureTimeInSeconds.set(exposureTimeInSeconds.get());
        stageMotionImagingInstruction.sleepAtStartingPosition.set(sleepAtStartingPosition.get());
        stageMotionImagingInstruction.sleepBeforeImaging.set(sleepBeforeImaging.get());
        stageMotionImagingInstruction.sliceDistance.set(sliceDistance.get());

        return stageMotionImagingInstruction;
    }

    public BoundedVariable<Double> getSliceDistance() {
        return sliceDistance;
    }

    public BoundedVariable<Integer> getNumberOfSlices() {
        return numberOfSlices;
    }

    public BoundedVariable<Integer> getSleepAtStartingPosition() {
        return sleepAtStartingPosition;
    }

    public BoundedVariable<Integer> getSleepBeforeImaging() {
        return sleepBeforeImaging;
    }

    public BoundedVariable<Integer> getImageWidth() {
        return imageWidth;
    }

    public BoundedVariable<Integer> getImageHeight() {
        return imageHeight;
    }

    public BoundedVariable<Double> getExposureTimeInSeconds() {
        return exposureTimeInSeconds;
    }

    public BoundedVariable<Integer> getBinning() {
        return binning;
    }

    @Override
    public Variable[] getProperties() {
        return new Variable[] {
                getImageHeight(),
                getImageWidth(),
                getNumberOfSlices(),
                getSleepAtStartingPosition(),
                getSleepBeforeImaging(),
                getSliceDistance(),
                getExposureTimeInSeconds(),
                getBinning()
        };
    }
}
