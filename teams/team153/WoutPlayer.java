package team153;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class WoutPlayer extends NovaPlayer {

    public WoutPlayer(RobotController controller) {
        super(controller);
    }
    
    public void step() {
        MapLocation location = navigation.findNearestArchon();
        int distance = location.distanceSquaredTo(controller.getLocation());
        if(energon.isEnergonLow() || controller.getFlux() > 300) {
            if(distance < 3) {
                energon.transferFlux(location);
                energon.requestEnergonTransfer();
                controller.yield();
            } else {
                navigation.moveOnceTowardsLocation(location, false);
            }
        } else {
            if(distance > 50 || controller.getFlux() > 300) {
                navigation.moveOnceTowardsLocation(location, false);
            } else {
                Direction dir = navigation.getMoveableDirection(controller.getDirection());
                navigation.moveOnceInDirection(dir, false);
            }
        }
    }
}
