package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public abstract class AttackPlayer extends NovaPlayer {

    public ArrayList<EnemyInfo> inRangeEnemies = new ArrayList<EnemyInfo>(), inRangeWithoutTurningEnemies = new ArrayList<EnemyInfo>(), outOfRangeEnemies = new ArrayList<EnemyInfo>(), archonEnemies = new ArrayList<EnemyInfo>(), outOfRangeArchonEnemies = new ArrayList<EnemyInfo>();
    public ArrayList<EnemyInfo> inRangeEnemiesTemp = new ArrayList<EnemyInfo>(), inRangeWithoutTurningEnemiesTemp = new ArrayList<EnemyInfo>(), outOfRangeEnemiesTemp = new ArrayList<EnemyInfo>(), archonEnemiesTemp = new ArrayList<EnemyInfo>(), outOfRangeArchonEnemiesTemp = new ArrayList<EnemyInfo>();
    public int noEnemiesCount = 0, seenEnemies = 0;
    public boolean movingToAttack = false, enemyInSightCalled = false, haveInRangeEnemy;
    public MapLocation attackLocation;
    public int range, minRange;

    public AttackMode mode;

    public AttackPlayer(RobotController controller) {
        super(controller);
        mode = new DefaultAttackMode();

        range = controller.getRobotType().attackRadiusMaxSquared();
        minRange = controller.getRobotType().attackRadiusMinSquared();
    }

    public void beginTurn() {
        inRangeEnemiesTemp = inRangeEnemies;
        inRangeWithoutTurningEnemiesTemp = inRangeWithoutTurningEnemies;
        outOfRangeEnemiesTemp = outOfRangeEnemies;
        archonEnemiesTemp = archonEnemies;
        outOfRangeArchonEnemiesTemp = outOfRangeArchonEnemies;

        inRangeEnemies = new ArrayList<EnemyInfo>();
        inRangeWithoutTurningEnemies = new ArrayList<EnemyInfo>();
        outOfRangeEnemies = new ArrayList<EnemyInfo>();
        archonEnemies = new ArrayList<EnemyInfo>();
        outOfRangeArchonEnemies = new ArrayList<EnemyInfo>();

        seenEnemies = 0;
        haveInRangeEnemy = false;
        
        ArrayList<RobotInfo> enemiesInfo = sensing.senseEnemyRobotInfoInSensorRange();
        if(enemiesInfo.size() > 0) {
            noEnemiesCount = 0;
        }
        for(RobotInfo robot : enemiesInfo) {
            sortEnemy(robot);
        }
    }

    /**
     * This method creates a single enemies list and stores it in enemies.
     * It simplifies caching enemy info for 10 turns, so that even if we do not see any new enemies for a bit, we might still try to attack there.
     */
    public void processEnemies() {
        if(seenEnemies == 0) {
            // no new enemy info was received, so stick with the old stuff
            if(noEnemiesCount < 4) {
                inRangeEnemies = inRangeEnemiesTemp;
                inRangeWithoutTurningEnemies = inRangeWithoutTurningEnemiesTemp;
                outOfRangeEnemies = outOfRangeEnemiesTemp;
                archonEnemies = archonEnemiesTemp;
                outOfRangeArchonEnemies = outOfRangeArchonEnemiesTemp;
                noEnemiesCount++;
            }
        }
    }

    /**
     * This method separates the enemies into four groups.
     * It sorts them both by in-range vs. out-of-range.
     * The idea is that if there is an in range enemy, attack immediately.
     * Otherwise, it may be worthwhile to move a little in order to attack.
     *
     * The enemies are also sorted by archon vs. not-archon.  Sometimes archons are harder to kill.
     */
    public void sortEnemy(RobotInfo robot) {
        EnemyInfo current = new EnemyInfo(robot);
        sortEnemy(current);
    }

    public void sortEnemy(EnemyInfo current) {
        seenEnemies++;
        if(current.type == RobotType.ARCHON) {
            if(controller.canAttackSquare(current.location)) {
                inRangeWithoutTurningEnemies.add(current);
                haveInRangeEnemy = true;
            } else if(current.distance <= range && current.distance >= minRange) {
                archonEnemies.add(current);
                haveInRangeEnemy = true;
            } else {
                outOfRangeArchonEnemies.add(current);
            }
        } else if(current.distance > range || current.distance < minRange) {
            outOfRangeEnemies.add(current);
        } else {
            if(controller.canAttackSquare(current.location)) {
                inRangeWithoutTurningEnemies.add(current);
                haveInRangeEnemy = true;
            } else {
                inRangeEnemies.add(current);
                haveInRangeEnemy = true;
            }
        }
    }

    public void enemyInSight(MapLocation[] locations, int[] ints, String[] strings, int locationStart, int intStart, int stringStart, int count) {
        noEnemiesCount = 0;

        if(!haveInRangeEnemy) {
            for(int c = 0; c < count; c++) {
                sortEnemy(new EnemyInfo(locations[locationStart], ints[intStart], strings[stringStart]));
                locationStart++;
                intStart++;
                stringStart++;
            }
        }
    }
    public void enemyInSight(MapLocation location, int energon, String type) {
        noEnemiesCount = 0;
        if(!haveInRangeEnemy) {
            sortEnemy(new EnemyInfo(location, energon, type));
        }
    }

    class EnemyInfo {

        public MapLocation location;
        public int distance, value;
        public double energon;
        public RobotType type;

        public EnemyInfo(RobotInfo info) {
            location = info.location;
            type = info.type;
            energon = info.energonLevel;
            distance = controller.getLocation().distanceSquaredTo(location);
            value = (int) energon * distance;
            if(this.type == RobotType.ARCHON) value /= 5;
        }

        public EnemyInfo(MapLocation location, int energonLevel, String type) {
            this.location = location;
            this.type = RobotType.valueOf(type);
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
            if(level == RobotLevel.ON_GROUND) {
                controller.attackGround(location);
            } else if(level == RobotLevel.IN_AIR) {
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
            if(inRangeWithoutTurningEnemies.size() > 0) return getCheapestEnemy(inRangeWithoutTurningEnemies);
            if(archonEnemies.size() > 0) return getCheapestEnemy(archonEnemies);
            if(inRangeEnemies.size() > 0) return getCheapestEnemy(inRangeEnemies);
            if(outOfRangeEnemies.size() > 0) return getCheapestEnemy(outOfRangeEnemies);
            if(outOfRangeArchonEnemies.size() > 0) return getCheapestEnemy(outOfRangeArchonEnemies);
            return null;
        }
    }

    class SoldierAttackMode extends AttackMode {
        /**
         * This method figures out which enemy to attack.
         * It will first attack in range enemies, and then it will attack archons.
         */
        public EnemyInfo getEnemyToAttack() {
            if(archonEnemies.size() > 0) return getCheapestEnemy(archonEnemies);
            if(inRangeWithoutTurningEnemies.size() > 0) return getCheapestEnemy(inRangeWithoutTurningEnemies);
            if(outOfRangeArchonEnemies.size() > 0) return getCheapestEnemy(outOfRangeArchonEnemies);
            if(inRangeEnemies.size() > 0) return getCheapestEnemy(inRangeEnemies);
            if(outOfRangeEnemies.size() > 0) return getCheapestEnemy(outOfRangeEnemies);
            return null;
        }
    }
}
