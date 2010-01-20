package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class ChainerPlayer extends AttackPlayer {

    public ChainerPlayer(RobotController controller) {
        super(controller);

    }

    public void boot() {
        messaging.sendNewUnit();
    }

    public void step() {
    }
}
