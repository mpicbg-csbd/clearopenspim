
package openspim;

import clearcl.ClearCLContext;
import clearcontrol.devices.cameras.StackCameraDeviceInterface;
import clearcontrol.devices.cameras.devices.hamamatsu.HamStackCamera;
import clearcontrol.devices.lasers.devices.omicron.OmicronLaserDevice;
import clearcontrol.devices.lasers.instructions.*;
import clearcontrol.devices.signalgen.devices.nirio.NIRIOSignalGenerator;
import clearcontrol.microscope.lightsheet.DefaultLightSheetMicroscope;
import clearcontrol.microscope.lightsheet.component.detection.DetectionArm;
import clearcontrol.microscope.lightsheet.component.lightsheet.LightSheet;
import clearcontrol.microscope.lightsheet.component.opticalswitch.LightSheetOpticalSwitch;
import clearcontrol.microscope.lightsheet.signalgen.LightSheetSignalGeneratorDevice;
import clearcontrol.microscope.lightsheet.simulation.LightSheetMicroscopeSimulationDevice;
import net.clearcontrol.devices.stages.picard.LinearPicardStage;

/**
 * OpenSPIM microscope assembly
 *
 * @author haesleinhuepf
 */
public class OpenSPIMMicroscope extends DefaultLightSheetMicroscope
{

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
                                     int numberOfLightSheets)
  {
    long defaultStackWidth = 512;
    long defaultStackHeight = 512;


    // Setting up lasers:
    {
      final OmicronLaserDevice laser488 = new OmicronLaserDevice(0);
      addDevice(0, laser488);
      addDevice(0, new SwitchLaserOnOffInstruction(laser488, true));
      addDevice(0, new SwitchLaserOnOffInstruction(laser488, false));
      addDevice(0, new SwitchLaserPowerOnOffInstruction(laser488, true));
      addDevice(0, new SwitchLaserPowerOnOffInstruction(laser488, false));
      addDevice(0, new ChangeLaserPowerInstruction(laser488));
    }

    // Setting up stages:
    {
      addDevice(0, new LinearPicardStage("X", 1));
      addDevice(0, new LinearPicardStage("Y", 1));
      addDevice(0, new LinearPicardStage("Z", 1));
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

  }

  @Override
  public void addSimulatedDevices(boolean dummySimulation,
                                  boolean hasXYZRStage,
                                  boolean hasSharedLightSheetControl,
                                  LightSheetMicroscopeSimulationDevice simulatorDevice)
  {
    super.addSimulatedDevices(dummySimulation, hasXYZRStage, hasSharedLightSheetControl, simulatorDevice);
  }

  @Override
  public void addStandardDevices(int pNumberOfControlPlanes) {
    super.addStandardDevices(pNumberOfControlPlanes);
  }

}
