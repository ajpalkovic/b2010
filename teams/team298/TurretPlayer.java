package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class TurretPlayer extends AttackPlayer {

    public TurretPlayer(RobotController controller) {
        super(controller);

    }

    public void boot() {
        messaging.sendNewUnit();
    }

    public void step() {
        processEnemies();

        if(energon.isEnergonLow()) {
            while(!energon.isEnergonFull()) {
                energon.requestEnergonTransfer();
                controller.yield();
            }
            return;
        }

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
            if(outOfRangeEnemies.size() > 0 || outOfRangeArchonEnemies.size() > 0) {
                // only move if we can do it in 1 turn or less
                if(controller.getRoundsUntilMovementIdle() == 0) {
                    moveToAttack();
                }
            } else {
                navigation.changeToArchonGoal(true);
                if(!navigation.goal.done()) navigation.moveOnce(true);
            }
        }
    }
}
