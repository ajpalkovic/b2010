package team153;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class TeleporterPlayer extends TowerPlayer {

    public TeleporterPlayer(RobotController controller) {
        super(controller);
    }

    public void step() {
        updateTowerBuildLocations();
    }
}
