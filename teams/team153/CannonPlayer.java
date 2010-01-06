package team153;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class CannonPlayer extends AttackPlayer {

    public CannonPlayer(RobotController controller) {
        super(controller);

    }

    public void run() {
        team = controller.getTeam();
        messaging.sendNewUnit();
        while(true) {
            int startTurn = Clock.getRoundNum();
            energon.autoTransferEnergonBetweenUnits();
            controller.setIndicatorString(0, controller.getLocation().toString());
            processEnemies();

            if(energon.isEnergonLow()) {
                while(!energon.isEnergonFull()) {
                    energon.requestEnergonTransfer();
                    controller.yield();
                }
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
                if(outOfRangeEnemies.size() > 0 || outOfRangeArchonEnemies.size() > 0) {
                    // only move if we can do it in 1 turn or less
                    if(controller.getRoundsUntilMovementIdle() == 0) {
                        moveToAttack();
                    }
                } else {
                    MapLocation archon = navigation.findNearestArchon();
                    if(archon != null && !controller.getLocation().isAdjacentTo(archon) && controller.getRoundsUntilMovementIdle() < 2) {
                        navigation.moveOnceTowardsLocation(archon);
                    }
                }
            }

            switch(currentGoal) {
                case Goal.followingArchon:
                    for(Robot r : controller.senseNearbyAirRobots()) {
                        if(r.getID() == followingArchonNumber) {
                            try {
                                navigation.goByBugging(map.getNotNull(controller.senseRobotInfo(r).location));
                            } catch(Exception e) {
                                p("----------------cannot sense robot info in following archon");
                            }
                        }
                    }

                    break;
            }

            if(startTurn == Clock.getRoundNum() || controller.hasActionSet()) {
                controller.yield();
            }
        }
    }
}
