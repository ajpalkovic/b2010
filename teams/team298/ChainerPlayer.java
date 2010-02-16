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
            //p(enemy.toString());
            
            //if the closest enemy is out of range, lets just move towards them first
            int enemyDistance = controller.getLocation().distanceSquaredTo(enemy.location);
            if(enemyDistance > 16) {
                navigation.changeToDirectionGoal(navigation.getMoveableDirection(controller.getLocation().directionTo(enemy.location)), false);
                navigation.moveOnce(false);
                navigation.popGoal();
                return;
            }

            int t = Clock.getRoundNum(), b = Clock.getBytecodeNum();
            MapLocation enemyLocation = enemy.location;
            if(!controller.canAttackSquare(enemyLocation)) navigation.faceLocation(enemyLocation);

            enemyLocation = getChainerAttackLocation(enemy);
            //we werent able to find a location in range that wouldnt hit our dudes as well
            if(enemyLocation == null) return;
            if(!controller.canAttackSquare(enemyLocation)) navigation.faceLocation(enemyLocation);
            
            if(!controller.canAttackSquare(enemyLocation) && canMove) {
                navigation.changeToLocationGoal(enemyLocation, false);
                navigation.moveOnce(false);
                navigation.popGoal();
                return;
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

    public MapLocation getChainerAttackLocation(EnemyInfo enemy) {
        ArrayList<RobotInfo> enemies = sensing.senseEnemyRobotInfoInSensorRange();
        ArrayList<RobotInfo> allies = sensing.senseAlliedRobotInfoInSensorRange();
        MapLocation[] archons = sensing.senseArchonLocations();

        boolean inAir = enemy.type == RobotType.ARCHON;

        MapLocation best = null, location = enemy.location;
        int alliesHit = Integer.MAX_VALUE, enemiesHit = Integer.MIN_VALUE;
        int minAlliesHit = Integer.MAX_VALUE, minEnemiesHit = Integer.MIN_VALUE;

        alliesHit = getAllyCount(allies, archons, location, inAir);
        if(alliesHit <= 1) {
            minEnemiesHit = getEnemyCount(enemies, location, inAir);
            minAlliesHit = alliesHit;
            best = location;
        }


        for(int x = -1; x <= 1; x++) {
            for(int y = -1; y <= 1; y++) {
                if(x == 0 && y == 0) continue;
                location = new MapLocation(enemy.location.getX()+x, enemy.location.getY()+y);
                if(location.distanceSquaredTo(controller.getLocation()) > 9) continue;
                
                alliesHit = getAllyCount(allies, archons, location, inAir);
                if(alliesHit > 1) continue;
                enemiesHit = getEnemyCount(enemies, location, inAir);
                
                if(alliesHit < minAlliesHit) {
                    best = location;
                    minAlliesHit = alliesHit;
                    minEnemiesHit = enemiesHit;
                } else if(alliesHit == minAlliesHit) {
                    if(enemiesHit > minEnemiesHit) {
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
