package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class TurretPlayer extends AttackPlayer {

    public NavigationGoal prevGoal;

    public TurretPlayer(RobotController controller) {
        super(controller);
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
                messaging.sendLowEnergon();
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
            navigation.faceLocation(enemy.location);

            //deploy();
            
            if(!controller.canAttackSquare(enemy.location) && canMove) {
                navigation.changeToLocationGoal(enemy.location, false);
                navigation.moveOnce(true);
                navigation.popGoal();
                return;
            }
            int status = executeAttack(enemy.location, enemy.type.isAirborne() ? RobotLevel.IN_AIR : RobotLevel.ON_GROUND);
            //if(status == Status.success) p("take that bitch");
            processEnemies();
            attackLocation = enemy.location;
        } else {
            //navigation.changeToMoveableDirectionGoal(true);
            navigation.moveOnce(true);
        }
    }

    public void undeploy() {
        if(controller.isDeployed()) {
            try {
                while(controller.hasActionSet() || controller.getRoundsUntilAttackIdle() > 0 || controller.getRoundsUntilMovementIdle() > 0) {
                    controller.yield();
                }
                controller.undeploy();
                controller.setIndicatorString(2, "UnDeployed");
            } catch (Exception e) {
                pa("----Caught exception while undeploying "+e.toString());
            }
        }
    }

    public void deploy() {
        if(!controller.isDeployed()) {
            try {
                while(controller.hasActionSet() || controller.getRoundsUntilAttackIdle() > 0 || controller.getRoundsUntilMovementIdle() > 0) {
                    controller.yield();
                }
                controller.deploy();
                controller.setIndicatorString(2, "Deployed");
            } catch (Exception e) {
                pa("----Caught exception while deploying "+e.toString());
            }
        }
    }
}
