package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class ChainerPlayer extends AttackPlayer {

    public NavigationGoal prevGoal;
    
    public ChainerPlayer(RobotController controller) {
        super(controller);
    }

    public void step() {
        MapLocation location = sensing.senseClosestArchon();
        int distance = location.distanceSquaredTo(controller.getLocation());

        //if the robot goes to get energon, then we need to save the followArchon goal for later
        if(prevGoal != null) prevGoal = navigation.goal;
        
        if(energon.isEnergonLow() || energon.isFluxFull() || distance > 34) {
            navigation.changeToArchonGoal(true);
            ignoreFollowRequest = true;
            if(distance < 3) {
                energon.requestEnergonTransfer();
                controller.yield();
            } else {
                navigation.moveOnce(false);
            }
            return;
        }

        if(prevGoal != null) navigation.goal = prevGoal;
        prevGoal = null;
        ignoreFollowRequest = false;

        processEnemies();
        sortEnemies();
        EnemyInfo enemy = mode.getEnemyToAttack();
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
                //navigation.changeToMoveableDirectionGoal(true);
                navigation.moveOnce(true);
            }
        }
    }
}
