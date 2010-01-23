package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class WoutPlayer extends NovaPlayer {

    public WoutPlayer(RobotController controller) {
        super(controller);
    }

    public void step() {
        MapLocation location = sensing.senseClosestArchon();
        int distance = location.distanceSquaredTo(controller.getLocation());

        if(energon.isEnergonLow() || energon.isFluxFull() || distance > 34) {
            navigation.changeToArchonGoal(true);
        } else {
            navigation.changeToMoveableDirectionGoal(true);
        }

        if((energon.isEnergonLow() || energon.isFluxFull()) && distance < 3) {
            energon.transferFlux(location);
            energon.requestEnergonTransfer();
            controller.yield();
        } else {
            navigation.moveOnce(false);
        }
    }

    public void followRequestMessageCallback(MapLocation location, int idOfSendingArchon, int idOfRecipient) {
        
    }
}
