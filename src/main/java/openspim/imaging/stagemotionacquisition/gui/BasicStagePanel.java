package openspim.imaging.stagemotionacquisition.gui;

import clearcontrol.core.variable.bounded.BoundedVariable;
import clearcontrol.devices.stages.BasicStageInterface;
import clearcontrol.gui.jfx.custom.gridpane.CustomGridPane;
import clearcontrol.gui.jfx.var.textfield.NumberVariableTextField;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

@Deprecated
public class BasicStagePanel extends CustomGridPane {
    public BasicStagePanel(BasicStageInterface stage) {
        int lRow = 0;
        System.out.println("Setting up BasicStageInterface Panel");

        Label lCurPos = new Label();
        add(lCurPos, 0, lRow);
        lRow++;

        {
            Button lButton = new Button("Read state");
            lButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    lCurPos.setText("" + stage.getPositionVariable().get());
                }
            });
            add(lButton, 0, lRow);
            lRow++;
        }


        lRow = 0;
        BoundedVariable<Double> lStepVariable =
                new BoundedVariable<Double>(stage.toString(),
                        0.001,
                        Double.MIN_VALUE,
                        Double.MAX_VALUE,
                        0.0001);

        {
            Button lButton = new Button("Up");
            lButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    stage.moveBy(lStepVariable.get(), false);
                }
            });
            add(lButton, 1, lRow);
            lRow++;
        }

        {
            NumberVariableTextField lStepTextField =
                    new NumberVariableTextField("Step",
                            lStepVariable);
            add(lStepTextField, 1, 1);
            lRow++;
        }

        {
            Button lButton = new Button("Down");
            lButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    stage.moveBy(-1 * lStepVariable.get(), false);
                }
            });
            add(lButton, 1, lRow);
            lRow++;
        }

    }
}
