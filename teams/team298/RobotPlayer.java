package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class RobotPlayer implements Runnable {

    private final RobotController controller;

    public RobotPlayer(RobotController rc) {
        controller = rc;
    }

    public void run() {
        NovaPlayer player;
        if(controller.getRobotType().equals(RobotType.ARCHON)) {
            player = new ArchonPlayer(controller);
        } else if(controller.getRobotType().equals(RobotType.SOLDIER)) {
            player = new SoldierPlayer(controller);
        } else if (controller.getRobotType().equals(RobotType.WOUT)) {
            player = new WoutPlayer(controller);
        } else if (controller.getRobotType().equals(RobotType.AURA)) {
            player = new AuraPlayer(controller);
        } else if (controller.getRobotType().equals(RobotType.TELEPORTER)) {
            player = new TeleporterPlayer(controller);
        } else {
            player = new NovaPlayer(controller);
        }

        while(true) {
            try {
                player.run();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
}
