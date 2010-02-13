package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class SoldierPlayer extends AttackPlayer {

    public NavigationGoal prevGoal;

    public SoldierPlayer(RobotController controller) {
        super(controller);
    }

    public void boot() {
        super.boot();
        changeToSoldierAttackMode();
    }

    public void step() {
        MapLocation location = sensing.senseClosestArchon();
        if(location == null) return;
        int distance = location.distanceSquaredTo(controller.getLocation());
        boolean canMove = true;

        //if the robot goes to get energon, then we need to save the followArchon goal for later
        if(prevGoal != null) prevGoal = navigation.goal;


        //always check if we got enough juice to go another round, if u know what i mean
        if(energon.isEnergonLow() || distance > 34) {
            navigation.changeToArchonGoal(true);
            ignoreFollowRequest = true;
            if(distance < 3) {
                energon.requestEnergonTransfer();
                controller.yield();
            } else {
                navigation.moveOnce(false);
            }
            canMove = false;
        } else {
            //restore the follow request goal
            if(prevGoal != null) navigation.goal = prevGoal;
            prevGoal = null;
            ignoreFollowRequest = false;
        }

        //find any enemey to attack.  mode.getEnemeyToAttack could return an out of range enemy too
        processEnemies();
        sortEnemies();
        EnemyInfo enemy = mode.getEnemyToAttack();

        if(enemy != null) {
            MapLocation enemyLocation = enemy.location;

            if(enemyLocation.distanceSquaredTo(controller.getLocation()) > 2) {
                if(controller.getRoundsUntilMovementIdle() <= 1) {
                    navigation.changeToLocationGoal(enemyLocation, false);
                    navigation.moveOnce(true);
                    navigation.popGoal();
                }
                return;
            }

            if(!controller.canAttackSquare(enemyLocation)) {
                navigation.faceLocation(enemyLocation);
            }
            
            int status = executeAttack(enemyLocation, enemy.type.isAirborne() ? RobotLevel.IN_AIR : RobotLevel.ON_GROUND);
            //if(status == Status.success) p("take that bitch");
            processEnemies();
            attackLocation = enemyLocation;
        } else {
            //navigation.changeToMoveableDirectionGoal(true);
            navigation.moveOnce(true);
        }
    }
}
