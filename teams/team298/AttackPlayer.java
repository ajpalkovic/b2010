package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public abstract class AttackPlayer extends NovaPlayer {

    public ArrayList<EnemyInfo> enemies = new ArrayList<EnemyInfo>(), enemiesTemp = new ArrayList<EnemyInfo>();
    public ArrayList<EnemyInfo> inRangeEnemies, inRangeWithoutTurningEnemies, outOfRangeEnemies, archonEnemies, outOfRangeArchonEnemies;
    public int noEnemiesCount = 0, maxDistanceAway = 3;
    public boolean movingToAttack = false, enemyInSightCalled = false;
    public MapLocation attackLocation;
    public int range, minRange;

    public AttackMode mode;

    public AttackPlayer(RobotController controller) {
        super(controller);
        enemies = new ArrayList<EnemyInfo>();
        mode = new DefaultAttackMode();

        range = controller.getRobotType().attackRadiusMaxSquared();
        minRange = controller.getRobotType().attackRadiusMinSquared();
    }

    /**
     * This method creates a single enemies list and stores it in enemies.
     * It simplifies caching enemy info for 10 turns, so that even if we do not see any new enemies for a bit, we might still try to attack there.
     */
    public void processEnemies() {
        if(!enemyInSightCalled) {
            enemiesTemp = enemies;
            enemies = new ArrayList<EnemyInfo>();
        }
        enemyInSightCalled = false;
        
        ArrayList<RobotInfo> enemiesInfo = sensing.senseEnemyRobotInfoInSensorRange();
        if(enemiesInfo.size() > 0) {
            noEnemiesCount = 0;
        }
        
        for(RobotInfo enemy : enemiesInfo) {
            enemies.add(new EnemyInfo(enemy));
        }

        if(enemies.size() > 0) {
            // new enemies were found in range, lets use that info
            enemiesTemp = null;
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
        navigation.changeToLocationGoal(mode.getEnemyToAttack().location, false);
        navigation.moveOnce(false);
        navigation.popGoal();
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
        inRangeWithoutTurningEnemies = new ArrayList<EnemyInfo>();
        outOfRangeEnemies = new ArrayList<EnemyInfo>();
        archonEnemies = new ArrayList<EnemyInfo>();
        outOfRangeArchonEnemies = new ArrayList<EnemyInfo>();

        for(int c = 0; c < enemies.size(); c++) {
            EnemyInfo current = enemies.get(c);

            if(current.type == RobotType.ARCHON) {
                if(controller.canAttackSquare(current.location)) {
                    inRangeWithoutTurningEnemies.add(current);
                } else if(current.distance <= range && current.distance >= minRange) {
                    archonEnemies.add(current);
                } else {
                    outOfRangeArchonEnemies.add(current);
                }
            } else if(current.distance > range || current.distance < minRange) {
                outOfRangeEnemies.add(current);
            } else {
                if(controller.canAttackSquare(current.location)) {
                    inRangeWithoutTurningEnemies.add(current);
                } else {
                    inRangeEnemies.add(current);
                }
            }
        }
    }

    public void enemyInSight(MapLocation[] locations, int[] ints, String[] strings, int locationStart, int intStart, int stringStart, int count) {
        if(!enemyInSightCalled) {
            enemiesTemp = enemies;
            enemies = new ArrayList<EnemyInfo>();
        }
        enemyInSightCalled = true;
        noEnemiesCount = 0;

        for(int c = 0; c < count; c++) {
            enemies.add(new EnemyInfo(locations[locationStart], ints[intStart], strings[stringStart]));
            locationStart++;
            intStart++;
            stringStart++;
        }
    }
    public void enemyInSight(MapLocation location, int energon, String type) {
        if(!enemyInSightCalled) {
            enemiesTemp = enemies;
            enemies = new ArrayList<EnemyInfo>();
        }
        enemyInSightCalled = true;
        noEnemiesCount = 0;

        enemies.add(new EnemyInfo(location, energon, type));
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
            if(this.type == RobotType.ARCHON) value /= 5;
        }

        public EnemyInfo(MapLocation location, int energonLevel, String type) {
            this.location = location;
            this.id = -1;
            this.type = RobotType.valueOf(type);
            this.info = null;
            this.energon = energonLevel;
            distance = controller.getLocation().distanceSquaredTo(location);
            value = (int) energon * distance;
            if(this.type == RobotType.ARCHON) value /= 5;
        }

        public String toString() {
            return location+" "+type;
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

    public void changeToDefaultAttackMode() {
        if(!(mode instanceof DefaultAttackMode)) {
            mode = new DefaultAttackMode();
        }
    }

    public void changeToSoldierAttackMode() {
        if(!(mode instanceof SoldierAttackMode)) {
            mode = new SoldierAttackMode();
        }
    }

    abstract class AttackMode {
        public abstract EnemyInfo getEnemyToAttack();

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
    }
    
    class DefaultAttackMode extends AttackMode {
        /**
         * This method figures out which enemy to attack.
         * It will first attack in range enemies, and then it will attack archons.
         */
        public EnemyInfo getEnemyToAttack() {
            if(enemies.size() == 0) {
                return null;
            }

            if(inRangeWithoutTurningEnemies.size() > 0) return getCheapestEnemy(inRangeWithoutTurningEnemies);
            if(archonEnemies.size() > 0) return getCheapestEnemy(archonEnemies);
            if(inRangeEnemies.size() > 0) return getCheapestEnemy(inRangeEnemies);
            if(outOfRangeEnemies.size() > 0) return getCheapestEnemy(outOfRangeEnemies);
            return getCheapestEnemy(outOfRangeArchonEnemies);
        }
    }

    class SoldierAttackMode extends AttackMode {
        /**
         * This method figures out which enemy to attack.
         * It will first attack in range enemies, and then it will attack archons.
         */
        public EnemyInfo getEnemyToAttack() {
            if(enemies.size() == 0) {
                return null;
            }

            if(archonEnemies.size() > 0) return getCheapestEnemy(archonEnemies);
            if(inRangeWithoutTurningEnemies.size() > 0) return getCheapestEnemy(inRangeWithoutTurningEnemies);
            if(outOfRangeArchonEnemies.size() > 0) return getCheapestEnemy(outOfRangeArchonEnemies);
            if(inRangeEnemies.size() > 0) return getCheapestEnemy(inRangeEnemies);
            return getCheapestEnemy(outOfRangeEnemies);
        }
    }
}
