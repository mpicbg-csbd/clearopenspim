package openspim.main;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import clearcl.ClearCL;
import clearcl.ClearCLContext;
import clearcl.ClearCLDevice;
import clearcl.backend.ClearCLBackends;
import clearcontrol.core.concurrent.thread.ThreadSleep;
import clearcontrol.core.configuration.MachineConfiguration;
import clearcontrol.core.log.LoggingFeature;
import clearcontrol.microscope.lightsheet.LightSheetMicroscope;
import clearcontrol.microscope.lightsheet.simulation.LightSheetMicroscopeSimulationDevice;
import clearcontrol.microscope.lightsheet.simulation.SimulationUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import openspim.OpenSPIMMicroscope;
import openspim.gui.OpenSPIMGui;

/**
 * Xwing main class
 *
 * @author haesleinhuepf
 */
public class OpenSPIMMain extends Application implements LoggingFeature
{
  static OpenSPIMMain instance = null;
  private boolean headless = false;

  public ClearCL getClearCL()
  {
    return mClearCL;
  }

  private ClearCL mClearCL;

  public static OpenSPIMMain getInstance()
  {
    if (instance == null)
    {
      launch();
    }
    return instance;
  }

  public OpenSPIMMain() {
    super();
  }

  public OpenSPIMMain(boolean headless) {
    headless = true;
  }

  private LightSheetMicroscope lightSheetMicroscope;

  public LightSheetMicroscope getLightSheetMicroscope()
  {
    return lightSheetMicroscope;
  }

  private static Alert alertWindow;
  private static Optional<ButtonType> result;

  static final MachineConfiguration
      sMachineConfiguration =
      MachineConfiguration.get();

  public static void main(String[] args)
  {
    launch(args);
  }

  @Override public void start(Stage primaryStage)
  {
    instance = this;
    if (headless) {
      return;
    }

    boolean has2DDisplay = true;
    boolean has3DDisplay = true;

    BorderPane borderPane = new BorderPane();
    ImageView
        imageView =
        new ImageView(new Image(OpenSPIMMicroscope.class.getResourceAsStream(
                "icon/openspim.png")));

    imageView.fitWidthProperty().bind(primaryStage.widthProperty());
    imageView.fitHeightProperty().bind(primaryStage.heightProperty());

    borderPane.setCenter(imageView);

    Scene scene = new Scene(borderPane, 300, 300, Color.WHITE);
    primaryStage.setScene(scene);
    primaryStage.setX(0);
    primaryStage.setY(0);
    primaryStage.setTitle("ClearOpenSPIM");
    primaryStage.show();

    ButtonType buttonReal = new ButtonType("Real");
    ButtonType buttonSimulation = new ButtonType("Simulation");
    ButtonType buttonCancel = new ButtonType("Cancel");

    alertWindow = new Alert(AlertType.CONFIRMATION);

    alertWindow.setTitle("Dialog");
    alertWindow.setHeaderText("Simulation or Real ?");
    alertWindow.setContentText(
        "Choose whether you want to start in real or simulation mode");

    alertWindow.getButtonTypes()
          .setAll(buttonReal,
                  buttonSimulation,
                  buttonCancel);

    Platform.runLater(() -> {
      result = alertWindow.showAndWait();
      Runnable lRunnable = () -> {
        if (result.get() == buttonSimulation)
        {
          startOpenSPIMMicroscope(true,
                     primaryStage,
                     has2DDisplay,
                     has3DDisplay);
        }
        else if (result.get() == buttonReal)
        {
          startOpenSPIMMicroscope(false,
                     primaryStage,
                     has2DDisplay,
                     has3DDisplay);
        }
        else if (result.get() == buttonCancel)
        {
          Platform.runLater(() -> primaryStage.hide());
        }
      };

      Thread thread = new Thread(lRunnable, "StartXWing");
      thread.setDaemon(true);
      thread.start();
    });

  }

  /**
   * Starts the microscope
   *
   * @param simulation   true
   * @param jfxPrimaryStage JFX primary stage
   * @param use2DDisplay    true: use 2D displays
   * @param use3DDisplay    true: use 3D displays
   */
  public OpenSPIMMicroscope startOpenSPIMMicroscope(boolean simulation,
                                                    Stage jfxPrimaryStage,
                                                    boolean use2DDisplay,
                                                    boolean use3DDisplay)
  {
    int numberOfDetectionArms = 1;
    int numberOfLightSheets = 1;

    int maxStackProcessingQueueLength = 32;
    int threadPoolSize = 1;
    int numberOfControlPlanes = 8;

    try (ClearCL clearCL = new ClearCL(ClearCLBackends.getBestBackend()))
    {
      for (ClearCLDevice clDevice : clearCL.getAllDevices())
        info("OpenCl devices available: %s \n",
             clDevice.getName());

      ClearCLContext
          fusionContext =
          clearCL.getDeviceByName(sMachineConfiguration.getStringProperty(
              "clearcl.device.fusion",
              "")).createContext();

      info("Using device %s for stack fusion \n",
           fusionContext.getDevice());

      OpenSPIMMicroscope
          openSpimMicroscope =
          new OpenSPIMMicroscope(fusionContext,
                              maxStackProcessingQueueLength,
                              threadPoolSize);
      lightSheetMicroscope = openSpimMicroscope;
      if (simulation)
      {
        ClearCLContext
            simulationContext =
            clearCL.getDeviceByName(sMachineConfiguration.getStringProperty(
                "clearcl.device.simulation",
                "HD")).createContext();

        info("Using device %s for simulation (Simbryo) \n",
             simulationContext.getDevice());

        LightSheetMicroscopeSimulationDevice
            simulatorDevice =
            SimulationUtils.getSimulatorDevice(simulationContext,
                                               numberOfDetectionArms,
                                               numberOfLightSheets,
                                               2048,
                                               11,
                                               320,
                                               320,
                                               320,
                                               false);

        openSpimMicroscope.addSimulatedDevices(false,
                                             false,
                                             true,
                                             simulatorDevice);
      }
      else
      {
        ClearCLContext
                simulationContext =
                clearCL.getDeviceByName(sMachineConfiguration.getStringProperty(
                        "clearcl.device.simulation",
                        "HD")).createContext();

        LightSheetMicroscopeSimulationDevice
                simulatorDevice =
                SimulationUtils.getSimulatorDevice(simulationContext,
                        numberOfDetectionArms,
                        numberOfLightSheets,
                        2048,
                        11,
                        320,
                        320,
                        320,
                        false);

        openSpimMicroscope.addRealHardwareDevices(numberOfDetectionArms,
                                                numberOfLightSheets,
                                                simulatorDevice);
      }
      openSpimMicroscope.addStandardDevices(numberOfControlPlanes);

      info("Opening microscope devices...");
      if (openSpimMicroscope.open())
      {
        info("Starting microscope devices...");
        if (openSpimMicroscope.start())
        {
          if (jfxPrimaryStage != null)
          {

            info("Setting up XWing GUI...");
            OpenSPIMGui openSpimGui =
                new OpenSPIMGui(openSpimMicroscope,
                             jfxPrimaryStage,
                             use2DDisplay,
                             use3DDisplay);
            openSpimGui.setup();
            info("Opening XWing GUI...");
            openSpimGui.open();

            openSpimGui.waitForVisible(true, 1L, TimeUnit.MINUTES);

            openSpimGui.connectGUI();
            openSpimGui.waitForVisible(false, null, null);

            openSpimGui.disconnectGUI();
            info("Closing XWing GUI...");
            openSpimGui.close();

            info("Stopping microscope devices...");
            openSpimMicroscope.stop();
            info("Closing microscope devices...");
            openSpimMicroscope.close();
           }
         else {
            mClearCL = clearCL;
            return openSpimMicroscope;
          }
        }
        else
          severe("Not all microscope devices started!");
      }
      else
        severe("Not all microscope devices opened!");

      ThreadSleep.sleep(100, TimeUnit.MILLISECONDS);
    }

    if (jfxPrimaryStage != null)
    {
      System.exit(0);
    }
    return null;
  }

}
