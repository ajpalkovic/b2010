package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class ChainerPlayer extends AttackPlayer {

    public NavigationGoal prevGoal;

    public ChainerPlayer(RobotController controller) {
        super(controller);
        range = 16;
    }

    public boolean tryImmediateAttack(EnemyInfo enemy) {
        MapLocation enemyLocation = enemy.location;
        int enemyDistance = controller.getLocation().distanceSquaredTo(enemy.location);

        if(controller.getRoundsUntilAttackIdle() > 0 || enemyDistance > 16) return false;
        
        enemyLocation = getChainerAttackLocation(enemy, false);
        if(enemyLocation == null) return false;

        if(!controller.canAttackSquare(enemyLocation)) {
            if(controller.getRoundsUntilMovementIdle() <= 1) {
                if(debug) p("face location tryImmediate");
                navigation.faceLocation(enemyLocation);
            } else {
                return false;
            }
        }

        enemyLocation = getChainerAttackLocation(enemy, true);
        if(enemyLocation == null) return false;
        if(!controller.canAttackSquare(enemyLocation)) return false;

        if(debug) p("execute attack called"+enemyLocation);
        return executeAttack(enemyLocation, enemy.type.isAirborne() ? RobotLevel.IN_AIR : RobotLevel.ON_GROUND) == Status.success;
    }

    public void tryMove(MapLocation enemyLocation) {
        //navigation.changeToLocationGoal(enemyLocation, false);
        navigation.changeToDirectionGoal(navigation.getMoveableDirection(controller.getLocation().directionTo(enemyLocation)), false);
        navigation.moveOnce(true);
        navigation.popGoal();
    }

    public void step() {
        MapLocation location = sensing.senseClosestArchon();
        navigation.changeToArchonGoal(true);
        if(location == null) return;
        int distance = location.distanceSquaredTo(controller.getLocation());
        boolean canMove = true;

        //double maxDistance = Math.max(20, Math.pow(controller.getEnergonLevel() / controller.getRobotType().energonUpkeep() / controller.getRobotType().moveDelayOrthogonal(), 2));
        //p(maxDistance+"");
        double maxDistance = 34;

        //always check if we got enough juice to go another round, if u know what i mean
        if(energon.isEnergonLow() || distance > maxDistance) {
            if(debug) p("Archon too far / low energon");
            //navigation.changeToArchonGoal(true);
            //ignoreFollowRequest = true;
            if(distance < 3) {
                messaging.sendLowEnergon();
            } else {
                navigation.moveOnce(false);
            }
            canMove = false;
        } else {
            //restore the follow request goal
            //if(prevGoal != null) navigation.goal = prevGoal;
            //prevGoal = null;
            //ignoreFollowRequest = false;
        }

        //find any enemey to attack.  mode.getEnemeyToAttack could return an out of range enemy too
        processEnemies();
        EnemyInfo enemy = mode.getEnemyToAttack();

        if(enemy != null) {
            MapLocation enemyLocation = enemy.location;
            int enemyDistance = controller.getLocation().distanceSquaredTo(enemy.location);

            if(debug) p(enemy.toString()+" "+enemyDistance);
            if(debug) p(controller.hasActionSet()+" "+controller.getRoundsUntilAttackIdle()+" "+controller.getRoundsUntilMovementIdle());
            
            boolean good = tryImmediateAttack(enemy);
            if(debug) p("result: "+good);
            if(debug) p(controller.hasActionSet()+" "+controller.getRoundsUntilAttackIdle()+" "+controller.getRoundsUntilMovementIdle());
            
            if(canMove && enemy.distance > 2 && (good || controller.getRoundsUntilMovementIdle() == 0 || controller.getRoundsUntilAttackIdle() > controller.getRoundsUntilMovementIdle())) {
                if(debug) p("try move");
                tryMove(enemyLocation);
                if(debug) p(controller.hasActionSet()+" "+controller.getRoundsUntilAttackIdle()+" "+controller.getRoundsUntilMovementIdle());
            }

            if(good) return;
            
            //if the closest enemy is out of range, lets just move towards them first
            if(enemyDistance > 16) {
                if(debug) p("Too far");
                if(canMove && controller.getRoundsUntilMovementIdle() <= 1) {
                    if(debug) p("try move 2");
                    tryMove(enemyLocation);
                }
                return;
            }

            if(!controller.canAttackSquare(enemyLocation)) {
                if(debug) p("Face location");
                navigation.faceLocation(enemyLocation);
                if(debug) p(controller.hasActionSet()+" "+controller.getRoundsUntilAttackIdle()+" "+controller.getRoundsUntilMovementIdle());
            }

            enemyLocation = getChainerAttackLocation(enemy, false);
            //we werent able to find a location in range that wouldnt hit our dudes as well
            if(enemyLocation == null) return;
            if(debug) p("Attack location: "+enemyLocation);
            if(!controller.canAttackSquare(enemyLocation)) {
                if(debug) p("face location 2");
                navigation.faceLocation(enemyLocation);
                if(debug) p(controller.hasActionSet()+" "+controller.getRoundsUntilAttackIdle()+" "+controller.getRoundsUntilMovementIdle());
            }
            
            if(!controller.canAttackSquare(enemyLocation)) {
                if(debug) p("cant attack");
                if(canMove && controller.getRoundsUntilMovementIdle() <= 1) {
                    if(debug) p("try move 3");
                    tryMove(enemyLocation);
                }
                return;
            }
            if(debug) p("execute attack");
            if(debug) p(controller.hasActionSet()+" "+controller.getRoundsUntilAttackIdle()+" "+controller.getRoundsUntilMovementIdle());
            int status = executeAttack(enemyLocation, enemy.type.isAirborne() ? RobotLevel.IN_AIR : RobotLevel.ON_GROUND);
            if(debug) p("attack: "+status);
            attackLocation = enemyLocation;
        } else {
            //navigation.changeToMoveableDirectionGoal(true);
            navigation.moveOnce(true);
        }
    }

    public int getEnemyCount(ArrayList<RobotInfo> enemies, MapLocation location, boolean inAir) {
        int enemiesHit = 0;
        for(RobotInfo e : enemies) {
            if(inAir == (e.type == RobotType.ARCHON)) {
                if(e.location.distanceSquaredTo(location) <= 2) {
                    enemiesHit++;
                }
            }
        }
        return enemiesHit;
    }

    public int getAllyCount(ArrayList<RobotInfo> allies, MapLocation[] archons, MapLocation location, boolean inAir) {
        int alliesHit = 0;
        
        if(location.equals(controller.getLocation())) alliesHit++;

        if(inAir) {
            for(MapLocation archon : archons) {
                if(archon.distanceSquaredTo(location) <= 2) {
                    return Integer.MAX_VALUE;
                }
            }
        } else {
            for(RobotInfo ally : allies) {
                if(ally.type != RobotType.ARCHON) {
                    if(ally.location.distanceSquaredTo(location) <= 2) {
                        alliesHit++;
                    }
                }
            }
        }

        return alliesHit;
    }

    public MapLocation getChainerAttackLocation(EnemyInfo enemy, boolean noTurn) {
        ArrayList<RobotInfo> allies = sensing.senseAlliedRobotInfoInSensorRange();
        ArrayList<RobotInfo> enemies = sensing.senseEnemyRobotInfoInSensorRange();
        MapLocation[] archons = sensing.senseArchonLocations();

        boolean inAir = enemy.type == RobotType.ARCHON;

        MapLocation best = null, location = enemy.location;
        int alliesHit = Integer.MAX_VALUE, enemiesHit = Integer.MIN_VALUE;
        int minAlliesHit = Integer.MAX_VALUE, minEnemiesHit = Integer.MIN_VALUE, minDistance = Integer.MAX_VALUE, distance;

        alliesHit = getAllyCount(allies, archons, location, inAir);
        if(alliesHit <= 1) {
            minEnemiesHit = getEnemyCount(enemies, location, inAir);
            minAlliesHit = alliesHit;
            minDistance = location.distanceSquaredTo(controller.getLocation());
            best = location;
        }


        for(int x = -1; x <= 1; x++) {
            for(int y = -1; y <= 1; y++) {
                if(x == 0 && y == 0) continue;
                location = new MapLocation(enemy.location.getX()+x, enemy.location.getY()+y);
                if(noTurn && !controller.canAttackSquare(location)) continue;
                distance = location.distanceSquaredTo(controller.getLocation());
                if(distance > 9) continue;
                
                alliesHit = getAllyCount(allies, archons, location, inAir);
                if(alliesHit > 1) continue;
                enemiesHit = getEnemyCount(enemies, location, inAir);
                
                if(alliesHit < minAlliesHit) {
                    best = location;
                    minAlliesHit = alliesHit;
                    minEnemiesHit = enemiesHit;
                } else if(alliesHit == minAlliesHit) {
                    if(enemiesHit > minEnemiesHit || distance < minDistance) {
                        best = location;
                        minAlliesHit = alliesHit;
                        minEnemiesHit = enemiesHit;
                    }
                }
            }
        }

        return best;
    }
}
