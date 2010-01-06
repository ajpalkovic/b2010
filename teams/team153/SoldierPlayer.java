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

    public void run() {
        team = controller.getTeam();
        messaging.sendNewUnit();
        while(true) {
            int startTurn = Clock.getRoundNum();
            //autoTransferEnergonBetweenUnits();
            controller.setIndicatorString(0, controller.getLocation().toString());
            processEnemies();
            messaging.parseMessages();
            if(energon.isEnergonLow()) {
                energon.requestEnergonTransfer();
                controller.yield();
                continue;
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

            if(startTurn == Clock.getRoundNum() || controller.hasActionSet()) {
                controller.yield();
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
        ArrayList<MapData> locations = getAttackLocations(new MapData(x, y));

        MapLocation returnData = enemyLocation;
        int minDistance = Integer.MAX_VALUE;
        int distance;
        for(MapData m : locations) {
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
        MapLocation archons[] = controller.senseAlliedArchons();
        if(archons != null) {
            int minDistance = controller.getLocation().distanceSquaredTo(archons[0]);
            MapLocation closestArchon = archons[0];
            for(int i = 0; i < archons.length; i++) {
                int dist = controller.getLocation().distanceSquaredTo(archons[i]);
                if(dist < minDistance) {
                    minDistance = dist;
                    closestArchon = archons[i];
                }
            }
            if(!controller.getLocation().isAdjacentTo(closestArchon)) {
                //           if (controller.getLocation().distanceSquaredTo(closestArchon) > ARCHON_DISTANCE) {
                navigation.goByBugging(new MapData(closestArchon));
                //                System.out.println("returning");
                //            }
            }
        }
    }
}
