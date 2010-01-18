package team153;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class AuraPlayer extends TowerPlayer {

    public AuraPlayer(RobotController controller) {
        super(controller);
    }

    public void step() {
        try {
            controller.setAura(AuraType.OFF);
        } catch (Exception e) {
            pa("----Caught exception while setting aura.");
        }
    }
}
