package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class WoutPlayer extends NovaPlayer {
    public int[] verticalDeltas = new int[] {-5, 0, -4, 3, -3, 4, -2, 4, -1, 4, 0, 5, 1, 4, 2, 4, 3, 4, 4, 3, 5, 0};
    public int[] horizontalDeltas = new int[] {0, -5, 3, -4, 4, -3, 4, -2, 4, -1, 5, 0, 4, 1, 4, 2, 4, 3, 3, 4, 0, 5};
    public int[] diagonalDeltas = new int[] {4, -3, 4, -2, 4, -1, 4, 0, 5, 0, 4, 1, 4, 2, 4, 3, 3, 3, 3, 4, 2, 4, 1, 4, 0, 4, 0, 5};

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

    public void senseNewTiles() {
        sensing.senseDeltas(verticalDeltas, horizontalDeltas, diagonalDeltas);
    }
}
