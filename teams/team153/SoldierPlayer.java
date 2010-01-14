package team153;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class SoldierPlayer extends AttackPlayer {

    final int ARCHON_DISTANCE = 6;

    public SoldierPlayer(RobotController controller) {
        super(controller);
        maxDistanceAway = 10;
    }

    public void step() {
        processEnemies();
        if(energon.isEnergonLow()) {
            energon.requestEnergonTransfer();
            controller.yield();
            return;
        }

        sortEnemies();
        EnemyInfo enemy = selectEnemy();
        if(enemy != null) {
            // attack
            if(!controller.canAttackSquare(enemy.location)) {
                navigation.faceLocation(enemy.location);
                processEnemies();
            }
            executeAttack(enemy.location, enemy.type.isAirborne() ? RobotLevel.IN_AIR : RobotLevel.ON_GROUND);
            processEnemies();
            attackLocation = enemy.location;
        } else {
            if(outOfRangeEnemies.size() > 0) {
                // only move if we can do it in 1 turn or less
                if(controller.getRoundsUntilMovementIdle() < 2) {
                    moveToAttack();
                }
            } else {
                MapLocation archon = navigation.findNearestArchon();
                if(!controller.getLocation().isAdjacentTo(archon) && controller.getRoundsUntilMovementIdle() < 2) {
                    navigation.moveOnceTowardsLocation(archon);
                }
            }
        }
    }

    public MapLocation findBestLocation(MapLocation enemyLocation) {
        if(true) {
            return enemyLocation;
        }
        controller.getLocation();
        int x, y;
        Direction dir = navigation.getDirection(controller.getLocation(), enemyLocation);
        x = enemyLocation.getX();
        y = enemyLocation.getY();
        ArrayList<NovaMapData> locations = getAttackLocations(new NovaMapData(x, y));

        MapLocation returnData = enemyLocation;
        int minDistance = Integer.MAX_VALUE;
        int distance;
        for(NovaMapData m : locations) {
            distance = m.toMapLocation().distanceSquaredTo(m.toMapLocation());
            if(distance < minDistance) {
                distance = minDistance;
                returnData = m.toMapLocation();
            }
        }
        return returnData;
    }

    public void returnToArchon() {
        //SENSE ARCHONS, ITERATE TO FIND CLOSEST, GO TO THAT ONE
        while(true) {
            MapLocation location = navigation.findNearestArchon();
            if(location.distanceSquaredTo(controller.getLocation()) > 2) {
                navigation.moveOnceTowardsLocation(location);
            } else {
                break;
            }
        }
    }
}
