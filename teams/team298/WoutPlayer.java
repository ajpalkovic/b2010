package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class WoutPlayer extends AttackPlayer {
    public int[] verticalDeltas = new int[] {-5, 0, -4, 3, -3, 4, -2, 4, -1, 4, 0, 5, 1, 4, 2, 4, 3, 4, 4, 3, 5, 0};
    public int[] horizontalDeltas = new int[] {0, -5, 3, -4, 4, -3, 4, -2, 4, -1, 5, 0, 4, 1, 4, 2, 4, 3, 3, 4, 0, 5};
    public int[] diagonalDeltas = new int[] {4, -3, 4, -2, 4, -1, 4, 0, 5, 0, 4, 1, 4, 2, 4, 3, 3, 3, 3, 4, 2, 4, 1, 4, 0, 4, 0, 5};
    public int[][] fluxDeltas = new int[][] {
        {-3, 0, 0},
        {-3, 1, 0},
        {-2, 2, 0},
        {-1, 3, 0},
        {0, 3, 0},
        {1, 3, 0},
        {2, 2, 0},
        {3, 1, 0},
        {3, 0, 0},
        {3, -1, 0},
        {2, -2, 0},
        {1, -3, 0},
        {0, -3, 0},
        {-1, -3, 0},
        {-2, -2, 0},
        {-3, -1, 0}
    };

    public WoutPlayer(RobotController controller) {
        super(controller);
    }

    public void step() {
        MapLocation location = sensing.senseClosestArchon();
        if(location == null) return;
        int distance = location.distanceSquaredTo(controller.getLocation());

        if(energon.isEnergonLow() || energon.isFluxFull() || distance > 50) {
            navigation.changeToArchonGoal(true);
        } else {
            navigation.changeToWoutCollectingFluxGoal(true);
        }

        if((energon.isEnergonLow() || energon.isFluxFull()) && distance < 3) {
            energon.transferFlux(location);
            energon.requestEnergonTransfer();
            controller.yield();
        } else {
            navigation.moveOnce(false);
        }
        
        // Transfer Flux to towers if they are nearby.
        ArrayList<MapLocation> towers = sensing.senseAlliedTowerLocations();
        MapLocation transferTarget = null;
        for (MapLocation towerLoc : towers) {
        	if (controller.getLocation().isAdjacentTo(towerLoc)) {
        		transferTarget = towerLoc;
        		break;
        	}
        }
        if (transferTarget != null) {
        	energon.transferFluxToTower(transferTarget);
        }

        messaging.sendMessageForEnemyRobots();

        processEnemies();
        sortEnemies();
        EnemyInfo enemy = mode.getEnemyToAttack();

        if(enemy != null && enemy.location.distanceSquaredTo(controller.getLocation()) <= 2) {
            executeAttack(enemy.location, enemy.type.isAirborne() ? RobotLevel.IN_AIR : RobotLevel.ON_GROUND);
        }

    }

    public void enemyInSight(MapLocation[] locations, int[] ints, String[] strings, int locationStart, int intStart, int stringStart, int count) {
    }

    public void followRequestMessageCallback(MapLocation location, int idOfSendingArchon, int idOfRecipient) {

    }

    public void senseNewTiles() {
        sensing.senseDeltas(verticalDeltas, horizontalDeltas, diagonalDeltas);
        sensing.senseFlux(fluxDeltas);
    }

    public void lowEnergonMessageCallback(MapLocation location1, int amount, int isAirUnit) {
    }
}
