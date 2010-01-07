package team153;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class WoutPlayer extends NovaPlayer {

    public WoutPlayer(RobotController controller) {
        super(controller);

    }

    public void run() {
        team = controller.getTeam();
        messaging.sendNewUnit();
        while(true) {
            int startTurn = Clock.getRoundNum();
            energon.autoTransferEnergonBetweenUnits();
            controller.setIndicatorString(0, controller.getLocation().toString());

            if(energon.isEnergonLow()) {
                while(!energon.isEnergonFull()) {
                    energon.requestEnergonTransfer();
                    controller.yield();
                }
                continue;
            }

            if(startTurn == Clock.getRoundNum() || controller.hasActionSet()) {
                controller.yield();
            }
        }
    }
}
