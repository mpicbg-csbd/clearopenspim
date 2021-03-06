package openspim.gui;

import clearcontrol.microscope.lightsheet.LightSheetMicroscope;
import clearcontrol.microscope.lightsheet.gui.LightSheetMicroscopeGUI;
import javafx.stage.Stage;

/**
 * OpenSPIM microscope GUI
 *
 * @author haesleinhuepf
 */
public class OpenSPIMGui extends LightSheetMicroscopeGUI
{

  /**
   * Instantiates XWing microscope GUI
   * 
   * @param lightSheetMicroscope
   *          microscope
   * @param jfxPrimaryStage
   *          JFX primary stage
   * @param has2DDisplay
   *          2D display
   * @param has3DDisplay
   *          3D display
   */
  public OpenSPIMGui(LightSheetMicroscope lightSheetMicroscope,
                     Stage jfxPrimaryStage,
                     boolean has2DDisplay,
                     boolean has3DDisplay)
  {
    super(lightSheetMicroscope,
          jfxPrimaryStage,
          has2DDisplay,
          has3DDisplay);
  }

}
