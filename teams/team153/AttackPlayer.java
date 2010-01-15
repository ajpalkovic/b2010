package team153;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public abstract class AttackPlayer extends NovaPlayer {

    public ArrayList<EnemyInfo> enemies, enemiesTemp;
    public ArrayList<EnemyInfo> inRangeEnemies, outOfRangeEnemies, archonEnemies, outOfRangeArchonEnemies;
    public int noEnemiesCount = 0, maxDistanceAway = 3;
    public boolean movingToAttack = false;
    public MapLocation attackLocation;

    public AttackPlayer(RobotController controller) {
        super(controller);
        enemies = new ArrayList<EnemyInfo>();
    }

    /**
     * This is called during moveOnceTowardsLocation.
     * The idea is to keep the cannons from getting too far from the archons.
     */
    public boolean directionCalculatedCallback(Direction dir) {
        if(!movingToAttack) {
            return true;
        }
        MapLocation location = controller.getLocation().add(dir);
        MapLocation[] archons = controller.senseAlliedArchons();
        for(MapLocation archon : archons) {
            if(location.isAdjacentTo(archon) || location.equals(archon)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method creates a single enemies list and stores it in enemies.
     * It simplifies caching enemy info for 10 turns, so that even if we do not see any new enemies for a bit, we might still try to attack there.
     */
    public void processEnemies() {
        enemiesTemp = enemies;
        enemies = new ArrayList<EnemyInfo>();

        ArrayList<RobotInfo> enemiesInfo = sensing.senseEnemyRobotInfoInSensorRange();
        for(RobotInfo enemy : enemiesInfo) {
            enemies.add(new EnemyInfo(enemy));
        }
        messaging.parseMessages();

        if(enemies.size() > 0) {
            // new enemies were found in range, lets use that info
            enemiesTemp = null;
            noEnemiesCount = 0;
        } else {
            // no new enemy info was received, so stick with the old stuff
            if(noEnemiesCount < 10) {
                enemies = enemiesTemp;
                noEnemiesCount++;
            } else {
                enemies.clear();
                enemiesTemp.clear();
                noEnemiesCount = 0;
            }
        }
    }

    /**
     * This method moves the robot one location towards the nearest enemy.
     * It should only be called if there are no inrange enemies.
     */
    public void moveToAttack() {
        movingToAttack = true;
        navigation.changeToLocationGoal(getCheapestEnemy(outOfRangeEnemies.size() > 0 ? outOfRangeEnemies : outOfRangeArchonEnemies).location, true);
        navigation.moveOnce(false);
        movingToAttack = false;
    }

    /**
     * This method separates the enemies into four groups.
     * It sorts them both by in-range vs. out-of-range.
     * The idea is that if there is an in range enemy, attack immediately.
     * Otherwise, it may be worthwhile to move a little in order to attack.
     *
     * The enemies are also sorted by archon vs. not-archon.  Sometimes archons are harder to kill.
     */
    public void sortEnemies() {
        inRangeEnemies = new ArrayList<EnemyInfo>();
        outOfRangeEnemies = new ArrayList<EnemyInfo>();
        archonEnemies = new ArrayList<EnemyInfo>();
        outOfRangeArchonEnemies = new ArrayList<EnemyInfo>();

        int range = controller.getRobotType().attackRadiusMaxSquared(), minRange = controller.getRobotType().attackRadiusMinSquared();

        for(int c = 0; c < enemies.size(); c++) {
            EnemyInfo current = enemies.get(c);

            if(current.type == RobotType.ARCHON) {
                if(current.distance <= range && current.distance >= minRange) {
                    archonEnemies.add(current);
                } else {
                    outOfRangeArchonEnemies.add(current);
                }
            } else if(current.distance > range || current.distance < minRange) {
                outOfRangeEnemies.add(current);
            } else {
                inRangeEnemies.add(current);
            }
        }
    }

    /**
     * This method figures out which enemy to attack.
     * It will first attack in range enemies, and then it will attack archons.
     */
    public EnemyInfo selectEnemy() {
        if(enemies.size() == 0) {
            return null;
        }

        if(inRangeEnemies.size() > 0) {
            return getCheapestEnemy(inRangeEnemies);
        } else {
            // if an enemy is just 1 or 2 hops a way, kill him but still come back
            if(outOfRangeEnemies.size() == 0) {
                if(archonEnemies.size() > 0) {
                    return getCheapestEnemy(archonEnemies);
                }
            }
        }
        return null;
    }

    /**
     * EnemyInfo stores a heuristic in it.
     * Currently the heuristic is energon level * distance.  The idea is to attack close and weak enemies first.
     * This method selects the cheapest of those.
     */
    public EnemyInfo getCheapestEnemy(ArrayList<EnemyInfo> enemyList) {
        EnemyInfo min = enemyList.get(0);
        for(int c = 1; c < enemyList.size(); c++) {
            EnemyInfo current = enemyList.get(c);
            if(controller.canSenseSquare(current.location)) {
                try {
                    if((current.type.isAirborne() && controller.senseAirRobotAtLocation(current.location) == null) ||
                            (!current.type.isAirborne() && controller.senseGroundRobotAtLocation(current.location) == null)) {
                        continue;
                    }
                } catch(Exception e) {
                    System.out.println("----Caught Exception in getCheapestEnemy Exception: " + e.toString());
                }
            }
            if(current.value < min.value) {
                min = current;
            }
        }
        return min;
    }

    /**
     * Unused callback that creates a list of any enemy in sensor range.
     * TODO: This 'logic' should be updated with the senseEnemyRobotInfo method instead.
     */
    public void enemyInSight(MapLocation enemyLocation, int energonLevel, String enemyType) {
        enemies.add(new EnemyInfo(enemyLocation, energonLevel, enemyType));
    }

    class EnemyInfo {

        public MapLocation location;
        public int id, distance, value;
        public double energon;
        public RobotType type;
        public RobotInfo info;

        public EnemyInfo(RobotInfo info) {
            location = info.location;
            id = -1;
            type = info.type;
            energon = info.energonLevel;
            distance = controller.getLocation().distanceSquaredTo(location);
            this.info = info;
            value = (int) energon * distance;
        }

        public EnemyInfo(MapLocation location, int energonLevel, String type) {
            this.location = location;
            this.id = -1;
            this.type = RobotType.valueOf(type);
            this.info = null;
            this.energon = energonLevel;
            distance = controller.getLocation().distanceSquaredTo(location);
            value = (int) energon * distance;
        }
    }

    /**
     * Executes an attack at the specified level and location.
     * The method does NOT wait until roundUntilAttackIdle is zero, but returns immediately
     * if it is not.  This is ideally used like in a callback function as the robot is
     * moving to make a quick attack or as part of a larger attack strategy.
     */
    public int executeAttack(MapLocation location, RobotLevel level) {
        if(controller.hasActionSet()) {
            return Status.turnsNotIdle;
        }

        if(controller.getRoundsUntilAttackIdle() != 0) {
            return Status.turnsNotIdle;
        }

        if(!controller.canAttackSquare(location)) {
            return Status.outOfRange;
        }

        try {
            if(level == RobotLevel.ON_GROUND && controller.canAttackGround()) {
                controller.attackGround(location);
            } else if(level == RobotLevel.IN_AIR && controller.canAttackAir()) {
                controller.attackAir(location);
            }
            controller.yield();
        } catch(Exception e) {
            System.out.println("----Caught exception in executeAttack with location: " + location.toString() + " level: " + level.toString() + " Exception: " + e.toString());
            return Status.fail;
        }
        return Status.success;
    }

    public MapLocation findBestLocation(MapLocation location) {
        return null;
    }
    /*
     * Returns ArrayList of MapData where this unit can attack this location from.
     */

    public ArrayList<MapLocation> getAttackLocations(MapLocation enemyLocation) {
        ArrayList<MapLocation> ableSpots = new ArrayList<MapLocation>();
        RobotType type = controller.getRobotType();
        //
        int x = enemyLocation.getX(), y = enemyLocation.getY();
        int maxDistance = type.attackRadiusMaxSquared();
        int minDistance = type.attackRadiusMinSquared();
        for(int i = x - maxDistance; i <= x + maxDistance; i++) {
            if(i <= x - minDistance && i >= x + minDistance) {
                continue;
            } else {
                for(int j = y - maxDistance; j < y + maxDistance; j++) {
                    if(j <= y - minDistance && j >= y + minDistance) {
                        ableSpots.add(new MapLocation(i, j));
                    }
                }
            }
        }
        return ableSpots;
    }

    /*
     * Represents a pending AttackRequest
     * AttackRequest is created when a
     */
    class AttackRequest {//i hate having the { up here, but I will do it for code uniformity like up there ^^^^^^

        public MapLocation unitLocation, enemyLocation;
        public int partyID, size = 0;
        MapLocation location;

        public void defend() {
        }

        public AttackRequest(int requestId, MapLocation unitLocation, MapLocation enemyLocation, int initSize) {
            this.partyID = requestId;
            this.unitLocation = unitLocation;
            this.enemyLocation = enemyLocation;
            size = initSize;
        }
    }
    //notes: we have a method for go-iing.  need one for go thing to go
    //somewhere that attacks enemy en-route hostile move.
}
