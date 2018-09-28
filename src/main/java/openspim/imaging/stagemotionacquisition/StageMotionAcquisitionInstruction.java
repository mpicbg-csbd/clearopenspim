package openspim.imaging.stagemotionacquisition;

import clearcl.ClearCLImage;
import clearcl.enums.ImageChannelDataType;
import clearcl.imagej.ClearCLIJ;
import clearcl.imagej.kernels.Kernels;
import clearcontrol.core.log.LoggingFeature;
import clearcontrol.core.variable.Variable;
import clearcontrol.core.variable.bounded.BoundedVariable;
import clearcontrol.devices.stages.BasicStageInterface;
import clearcontrol.instructions.InstructionInterface;
import clearcontrol.instructions.PropertyIOableInstructionInterface;
import clearcontrol.microscope.lightsheet.LightSheetMicroscope;
import clearcontrol.microscope.lightsheet.imaging.SingleViewPlaneImager;
import clearcontrol.microscope.lightsheet.imaging.sequential.SequentialImageDataContainer;
import clearcontrol.microscope.lightsheet.instructions.LightSheetMicroscopeInstructionBase;
import clearcontrol.microscope.lightsheet.state.InterpolatedAcquisitionState;
import clearcontrol.microscope.lightsheet.warehouse.containers.StackInterfaceContainer;
import clearcontrol.stack.StackInterface;
import clearcontrol.stack.metadata.MetaDataVoxelDim;
import clearcontrol.stack.metadata.StackMetaData;

import static clearcontrol.stack.metadata.MetaDataChannel.Channel;

/**
 * StageMotionAcquisitionInstruction
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 09 2018
 */
public class StageMotionAcquisitionInstruction extends LightSheetMicroscopeInstructionBase implements LoggingFeature, PropertyIOableInstructionInterface {

    BoundedVariable<Integer> sleepAtStartingPosition = new BoundedVariable<Integer>("Sleep at start position in ms", 0, 0, Integer.MAX_VALUE);
    BoundedVariable<Integer> sleepBeforeImaging = new BoundedVariable<Integer>("Sleep before imaging in ms", 100, 0, Integer.MAX_VALUE);

    BoundedVariable<Double> sliceDistance = new BoundedVariable<Double>("Slice distance in microns", 2.5, 0.001, Double.MAX_VALUE, 0.001);
    BoundedVariable<Integer> numberOfSlices = new BoundedVariable<Integer>("Number of slices", 10, 0, Integer.MAX_VALUE);

    BoundedVariable<Integer> imageWidth = new BoundedVariable<Integer>("Image width in pixels", 512, 0, Integer.MAX_VALUE);
    BoundedVariable<Integer> imageHeight = new BoundedVariable<Integer>("Image height in pixels", 512, 0, Integer.MAX_VALUE);

    BoundedVariable<Double> exposureTimeInSeconds = new BoundedVariable<Double>("Exposure time in seconds", 0.5, 0.001, Double.MAX_VALUE, 0.0001);

    ClearCLIJ clij = ClearCLIJ.getInstance();

    BasicStageInterface stageZ = null;

    /**
     * INstanciates a virtual device with a given name
     *
     * @param pLightSheetMicroscope
     */
    public StageMotionAcquisitionInstruction(LightSheetMicroscope pLightSheetMicroscope) {
        super("Acquisition: Stage motion acquisition", pLightSheetMicroscope);
    }

    public StageMotionAcquisitionInstruction(String pDeviceName, LightSheetMicroscope pLightSheetMicroscope) {
        super(pDeviceName, pLightSheetMicroscope);
    }


    @Override
    public boolean initialize() {
        clij = ClearCLIJ.getInstance();

        stageZ = null;

        for (BasicStageInterface stage : getLightSheetMicroscope().getDevices(BasicStageInterface.class))
        {
            if (stage.toString().contains("Z"))
            {
                stageZ = stage;
            }
        }
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

        ClearCLImage clStack = clij.createCLImage(new long[]{imageWidth.get(), imageHeight.get().longValue(), numberOfImagesToTake}, ImageChannelDataType.UnsignedInt16);

        long acquisitionRequestTime = System.nanoTime();
        for (int i = 0; i < numberOfImagesToTake; i++) {

            if (i > 0) {
                stageZ.moveBy(sliceDistanceInMillimeters, true);
            }
            sleep(sleepBeforeImaging.get());
            ClearCLImage planeImage = acquireSinglePlane(lightSheetPosition);

            Kernels.copySlice(clij, planeImage, clStack, i);

            planeImage.close();
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
        StackInterface stack = clij.converter(clStack).getStack();

        stack.setMetaData(new StackMetaData());
        stack.getMetaData().addEntry(MetaDataVoxelDim.VoxelDimX, 1.0);
        stack.getMetaData().addEntry(MetaDataVoxelDim.VoxelDimY, 1.0);
        stack.getMetaData().addEntry(MetaDataVoxelDim.VoxelDimZ, sliceDistanceInMillimeters);
        stack.getMetaData().addEntry(Channel, "C0L0");
        stack.getMetaData().setTimeStampInNanoseconds(acquisitionRequestTime);

        container.put("C0L0", stack);

        getLightSheetMicroscope().getDataWarehouse().put("stagemotionacquisitionstack_" + pTimePoint, container);

        return true;
    }

    protected ClearCLImage acquireSinglePlane(double z) {


        SingleViewPlaneImager imager =
                new SingleViewPlaneImager(getLightSheetMicroscope(), z);
        imager.setImageHeight(imageHeight.get());
        imager.setImageWidth(imageWidth.get());
        imager.setExposureTimeInSeconds(getExposureTimeInSeconds().get());
        imager.setLightSheetIndex(0);
        imager.setDetectionArmIndex(0);

        // acquire an image
        StackInterface acquiredImageStack = imager.acquire();

        // convert and return
        ClearCLImage clImage = clij.converter(acquiredImageStack).getClearCLImage();
        ClearCLImage sliceImage = clij.createCLImage(new long[] {clImage.getWidth(), clImage.getHeight()}, clImage.getChannelDataType());
        Kernels.copySlice(clij, clImage, sliceImage, 0);
        clImage.close();

        return sliceImage;
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
    public StageMotionAcquisitionInstruction copy() {
        return new StageMotionAcquisitionInstruction(getLightSheetMicroscope());
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

    @Override
    public Variable[] getProperties() {
        return new Variable[] {
                getImageHeight(),
                getImageWidth(),
                getNumberOfSlices(),
                getSleepAtStartingPosition(),
                getSleepBeforeImaging(),
                getSliceDistance(),
                getExposureTimeInSeconds()
        };
    }
}
