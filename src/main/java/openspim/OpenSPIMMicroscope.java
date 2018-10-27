
package openspim;

import clearcl.ClearCLContext;
import clearcontrol.core.variable.Variable;
import clearcontrol.devices.cameras.StackCameraDeviceInterface;
import clearcontrol.devices.cameras.devices.hamamatsu.HamStackCamera;
import clearcontrol.devices.cameras.devices.sim.StackCameraDeviceSimulator;
import clearcontrol.devices.cameras.devices.sim.StackCameraSimulationProvider;
import clearcontrol.devices.cameras.devices.sim.providers.FractalStackProvider;
import clearcontrol.devices.lasers.LaserDeviceInterface;
import clearcontrol.devices.lasers.instructions.*;
import clearcontrol.devices.signalamp.ScalingAmplifierDeviceInterface;
import clearcontrol.devices.signalamp.devices.sim.ScalingAmplifierSimulator;
import clearcontrol.devices.signalgen.devices.nirio.NIRIOSignalGenerator;
import clearcontrol.devices.signalgen.devices.sim.SignalGeneratorSimulatorDevice;
import clearcontrol.devices.stages.kcube.sim.SimulatedBasicStageDevice;
import clearcontrol.microscope.lightsheet.DefaultLightSheetMicroscope;
import clearcontrol.microscope.lightsheet.component.detection.DetectionArm;
import clearcontrol.microscope.lightsheet.component.lightsheet.LightSheet;
import clearcontrol.microscope.lightsheet.component.opticalswitch.LightSheetOpticalSwitch;
import clearcontrol.microscope.lightsheet.signalgen.LightSheetSignalGeneratorDevice;
import clearcontrol.microscope.lightsheet.simulation.LightSheetMicroscopeSimulationDevice;
import net.clearcontrol.devices.cameras.andorsdk.AndorImager;
import net.clearcontrol.devices.cameras.andorsdk.AndorStackInterfaceImager;
import net.clearcontrol.devices.cameras.simulation.SimulatedImager;
import net.clearcontrol.devices.lasers.coherent.obis.SingleCoherentObisLaserDevice;
import net.clearcontrol.devices.stages.picard.LinearPicardStage;
import net.clearcontrol.devices.stages.picard.TwisterPicardStage;
import net.imagej.patcher.LegacyInjector;
import openspim.imaging.stagemotion.StageMotionImagingInstruction;
import openspim.imaging.stagemotionacquisition.StageMotionAcquisitionWithAndorSDKImagingInstruction;

import java.util.ArrayList;

/**
 * OpenSPIM microscope assembly
 *
 * @author haesleinhuepf
 */
public class OpenSPIMMicroscope extends DefaultLightSheetMicroscope
{
  static {
    LegacyInjector.preinit();
  }
  /**
   * Instantiates an OpenSPIM microscope
   * 
   * @param fusionContext
   *          ClearCL context for stack fusion
   * @param maxStackProcessingQueueLength
   *          max stack processing queue length
   * @param threadPoolSize
   *          thread pool size
   */
  public OpenSPIMMicroscope(ClearCLContext fusionContext,
                         int maxStackProcessingQueueLength,
                         int threadPoolSize)
  {
    super("OpenSPIM",
          fusionContext,
          maxStackProcessingQueueLength,
          threadPoolSize);

  }

  /**
   * Assembles the microscope
   * 
   * @param numberOfDetectionArms
   *          number of detection arms
   * @param numberOfLightSheets
   *          number of lightsheets
   */
  public void addRealHardwareDevices(int numberOfDetectionArms,
                                     int numberOfLightSheets,
                                     LightSheetMicroscopeSimulationDevice pSimulatorDevice) {
    long defaultStackWidth = 512;
    long defaultStackHeight = 512;


    // Setting up lasers:
    //if (false)

      //final OmicronLaserDevice laser488 = new OmicronLaserDevice(0);
//      LaserDeviceInterface laser488 =
//              new LaserDeviceSimulator("Laser 488 sim",
//                      0,
//                      488,
//                      100);
      LaserDeviceInterface laser488 = new SingleCoherentObisLaserDevice("COM3", 115200, 488);

      addDevice(0, laser488);
      addDevice(0, new SwitchLaserOnOffInstruction(laser488, true));
      addDevice(0, new SwitchLaserOnOffInstruction(laser488, false));
      addDevice(0, new SwitchLaserPowerOnOffInstruction(laser488, true));
      addDevice(0, new SwitchLaserPowerOnOffInstruction(laser488, false));
      addDevice(0, new ChangeLaserPowerInstruction(laser488));


    // setting up cameras
    if (false)
    {
      AndorImager andorImager = new AndorImager(0);
      andorImager.setPixelSizeInMicrons(0.26);
      andorImager.setLaserDevice(laser488);
      addDevice(0, andorImager);
      addDevice(0, new StageMotionAcquisitionWithAndorSDKImagingInstruction(this));
    }

    {
      AndorStackInterfaceImager andorImager = new AndorStackInterfaceImager(0);
      andorImager.setPixelSizeInMicrons(0.26);
      andorImager.setLaserTriggerOnVariable(laser488.getLaserOnVariable());
      addDevice(0, andorImager);
      addDevice(0, new StageMotionAcquisitionWithAndorSDKImagingInstruction(this));
    }

    // Setting up stages:
    {
      addDevice(0, new LinearPicardStage("X", 122));
      addDevice(0, new LinearPicardStage("Y", 123));
      addDevice(0, new LinearPicardStage("Z", 121));
      addDevice(0, new TwisterPicardStage("R", 28));
    }

    // Setting up cameras:
    if (false)
    {
      for (int c = 0; c < numberOfDetectionArms; c++)
      {
        StackCameraDeviceInterface<?> camera =
                                              HamStackCamera.buildWithExternalTriggering(c);

        camera.getStackWidthVariable().set(defaultStackWidth);
        camera.getStackHeightVariable().set(defaultStackHeight);
        camera.getExposureInSecondsVariable().set(0.010);

        addDevice(c, camera);
      }
    }

    // Adding signal Generator:

    if (false)
    {
      LightSheetSignalGeneratorDevice lightSheetSignalGenerator;
      NIRIOSignalGenerator nirioSignalGenerator =
                                                 new NIRIOSignalGenerator();
      lightSheetSignalGenerator =
                         LightSheetSignalGeneratorDevice.wrap(nirioSignalGenerator,
                                                              true);
      // addDevice(0, nirioSignalGenerator);
      addDevice(0, lightSheetSignalGenerator);
    }

    // Setting up detection arms:
    if (false)
    {
      for (int c = 0; c < numberOfDetectionArms; c++)
      {
        final DetectionArm detectionArm = new DetectionArm("D" + c);
        detectionArm.getPixelSizeInMicrometerVariable().set(getDevice(StackCameraDeviceInterface.class, c).getPixelSizeInMicrometersVariable().get());

        addDevice(c, detectionArm);
      }
    }

    // Setting up lightsheets:
    if (false)
    {
      for (int l = 0; l < numberOfLightSheets; l++)
      {
        final LightSheet lightSheet =
                                     new LightSheet("I" + l,
                                                    9.4,
                                                    getNumberOfLaserLines());
        addDevice(l, lightSheet);
      }
    }

    // syncing exposure between cameras and lightsheets, as well as camera image
    // height:
    if (false)
    {
      for (int l = 0; l < numberOfLightSheets; l++)
        for (int c = 0; c < numberOfDetectionArms; c++)
        {
          StackCameraDeviceInterface<?> camera =
                                                getDevice(StackCameraDeviceInterface.class,
                                                          c);
          LightSheet lLightSheet = getDevice(LightSheet.class, l);

          camera.getExposureInSecondsVariable()
                 .sendUpdatesTo(lLightSheet.getEffectiveExposureInSecondsVariable());

          camera.getStackHeightVariable()
                 .sendUpdatesTo(lLightSheet.getImageHeightVariable());
        }
    }

    // Setting up lightsheets selector
    if (false)
    {
      LightSheetOpticalSwitch lightSheetOpticalSwitch =
                                                       new LightSheetOpticalSwitch("OpticalSwitch",
                                                                                   numberOfLightSheets);
      addDevice(0, lightSheetOpticalSwitch);
    }


    // ----------------------------------------------
    // add simulated devices to the real hardware scope

    int lNumberOfDetectionArms = numberOfDetectionArms;
    int lNumberOfLightSheets = numberOfLightSheets;
    boolean pSharedLightSheetControl = true;
    boolean pDummySimulation = true;


    // Setting up trigger:

    Variable<Boolean> lTrigger =
            new Variable<Boolean>("CameraTrigger",
                    false);

    ArrayList<StackCameraDeviceSimulator> lCameraList =
            new ArrayList<>();

    // Setting up cameras:
    {

      for (int c = 0; c < lNumberOfDetectionArms; c++)
      {
        final StackCameraDeviceSimulator lCamera =
                new StackCameraDeviceSimulator("StackCamera"
                        + c,
                        lTrigger);

        long lMaxWidth = pSimulatorDevice.getSimulator()
                .getCameraRenderer(c)
                .getMaxWidth();

        long lMaxHeight = pSimulatorDevice.getSimulator()
                .getCameraRenderer(c)
                .getMaxHeight();

        lCamera.getMaxWidthVariable().set(lMaxWidth);
        lCamera.getMaxHeightVariable().set(lMaxHeight);
        lCamera.getStackWidthVariable().set(lMaxWidth / 2);
        lCamera.getStackHeightVariable().set(lMaxHeight);
        lCamera.getExposureInSecondsVariable().set(0.010);

        // lCamera.getStackVariable().addSetListener((o,n)->
        // {System.out.println("camera output:"+n);} );

        addDevice(c, lCamera);

        lCameraList.add(lCamera);
      }
    }

    // Scaling Amplifier:
    {
      ScalingAmplifierDeviceInterface lScalingAmplifier1 =
              new ScalingAmplifierSimulator("ScalingAmplifier1");
      addDevice(0, lScalingAmplifier1);

      ScalingAmplifierDeviceInterface lScalingAmplifier2 =
              new ScalingAmplifierSimulator("ScalingAmplifier2");
      addDevice(1, lScalingAmplifier2);
    }

    // Signal generator:

    {
      SignalGeneratorSimulatorDevice lSignalGeneratorSimulatorDevice =
              new SignalGeneratorSimulatorDevice();

      // addDevice(0, lSignalGeneratorSimulatorDevice);
      lSignalGeneratorSimulatorDevice.getTriggerVariable()
              .sendUpdatesTo(lTrigger);/**/

      final LightSheetSignalGeneratorDevice lLightSheetSignalGeneratorDevice =
              LightSheetSignalGeneratorDevice.wrap(lSignalGeneratorSimulatorDevice,
                      pSharedLightSheetControl);

      addDevice(0, lLightSheetSignalGeneratorDevice);
    }

    // setting up staging score visualization:

    /*final ScoreVisualizerJFrame lVisualizer = ScoreVisualizerJFrame.visualize("LightSheetDemo",
                                                                              lStagingScore);/**/

    // Setting up detection arms:

    {
      for (int c = 0; c < lNumberOfDetectionArms; c++)
      {
        final DetectionArm lDetectionArm = new DetectionArm("D" + c);
        lDetectionArm.getPixelSizeInMicrometerVariable()
                .set(pSimulatorDevice.getSimulator()
                        .getPixelWidth(c));

        addDevice(c, lDetectionArm);
      }
    }

    // Setting up lightsheets:
    {
      for (int l = 0; l < lNumberOfLightSheets; l++)
      {
        final LightSheet lLightSheet =
                new LightSheet("I" + l,
                        9.4,
                        getNumberOfLaserLines());
        addDevice(l, lLightSheet);

      }
    }

    // Setting up lightsheets selector
    {
      LightSheetOpticalSwitch lLightSheetOpticalSwitch =
              new LightSheetOpticalSwitch("OpticalSwitch",
                      lNumberOfLightSheets);

      addDevice(0, lLightSheetOpticalSwitch);
    }

    // Setting up simulator:
    {
      // Now that the microscope has been setup, we can connect the simulator to
      // it:

      // first, we connect the devices in the simulator so that parameter
      // changes
      // are forwarded:
      pSimulatorDevice.connectTo(this);

      // second, we make sure that the simulator is used as provider for the
      // simulated cameras:
      for (int c = 0; c < lNumberOfDetectionArms; c++)
      {
        StackCameraSimulationProvider lStackProvider;
        if (pDummySimulation)
          lStackProvider = new FractalStackProvider();
        else
          lStackProvider = pSimulatorDevice.getStackProvider(c);
        lCameraList.get(c)
                .setStackCameraSimulationProvider(lStackProvider);
      }
    }



  }

  @Override
  public void addSimulatedDevices(boolean dummySimulation,
                                  boolean hasXYZRStage,
                                  boolean hasSharedLightSheetControl,
                                  LightSheetMicroscopeSimulationDevice simulatorDevice)
  {
    super.addSimulatedDevices(dummySimulation, hasXYZRStage, hasSharedLightSheetControl, simulatorDevice);

    SimulatedImager simulatedImager = new SimulatedImager();
    addDevice(0, simulatedImager);

    addDevice(0, new StageMotionImagingInstruction(this, getDevice(SimulatedBasicStageDevice.class, 0), simulatedImager));
  }

  @Override
  public void addStandardDevices(int pNumberOfControlPlanes) {
    super.addStandardDevices(pNumberOfControlPlanes);
  }

}
