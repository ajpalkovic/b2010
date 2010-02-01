package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class SporadicSpawning extends Base {

    public static Random gen = new Random();
    public MapStore map;
    public MexicanMessaging messaging;
    public NaughtyNavigation navigation;
    public SensationalSensing sensing;
    public EnergeticEnergon energon;
    public SpawnMode mode;
    public MapLocation spawnLocation;
    public RobotType previousSpawnType;

    public SporadicSpawning(NovaPlayer player) {
        super(player);
        map = player.map;
        messaging = player.messaging;
        navigation = player.navigation;
        sensing = player.sensing;
        energon = player.energon;
        mode = new CollectingFluxSpawnMode();
    }

    /**
     * Spawns a tower.
     */
    public int spawnTower(RobotType towerType) {
        MapLocation spawnLocation = getSpawnLocation(false);

        if(spawnLocation == null) {
            return Status.fail;
        }

        navigation.faceLocation(spawnLocation);

        try {
            if(navigation.isLocationFree(controller.getLocation().add(controller.getDirection()), false)) {
                controller.spawn(towerType);
                sensing.teleporterLocations.add(controller.getLocation());
                controller.yield();
            } else {
                return spawnTower(towerType);
            }
        } catch(GameActionException e) {
            pa("----Caught Exception in spawnTower.  robot: " + towerType.toString() + " spawnLocation: " + spawnLocation.toString() + " Exception: " + e.toString());
            return Status.fail;
        }
        return Status.success;
    }

    /**
     * Uses the spawning mode to get the next robot type to spawn and calls spawnRobot(type).
     * In almost all cases, this method should be called instead of the other spawnRobot method.
     */
    public int spawnRobot() {
        RobotType type = mode.getNextRobotSpawnType();
        if(canSupportUnit(type))
            return spawnRobot(type);
        return Status.cannotSupportUnit;
    }

    /**
     * Returns true if the robot has enough flux to make the tower.
     */
    public boolean canSupportTower(RobotType tower) {
        return (player.controller.getFlux() >= tower.spawnFluxCost());
    }

    /**
     * Returns true if this archon can support the unit's energon requirements.
     */
    public boolean canSupportUnit(RobotType unit) {
        if(controller.getEnergonLevel() < Math.min(unit.spawnCost() + 25, 74)) {
            return false;
        }

        ArrayList<RobotInfo> air = sensing.senseAirRobotInfo(), ground = sensing.senseAlliedRobotInfoInSensorRange();
        double energonCost = 0;
        for(RobotInfo robot : ground) {
            energonCost += robot.type.energonUpkeep();
        }

        double energonProduction = 1;
        for(RobotInfo robot : air) {
            if(robot.team.equals(player.team)) {
                energonProduction += 0.8;
            }
        }

        // each archon should keep .3 for itself?
        return energonProduction > energonCost + (energonProduction * .4);
    }

    /**
     * Returns the first MapLocation of the 8 around an Archon which does not have a unit at that location.
     */
    public MapLocation getSpawnLocation(boolean isAirUnit) {
        ArrayList<MapLocation> locations = new ArrayList<MapLocation>();
        MapLocation[] orderedLocations = navigation.getOrderedMapLocations();
        for(MapLocation location : orderedLocations) {
            if(navigation.isLocationFree(location, isAirUnit)) {
                locations.add(location);
            }
        }
        if(locations.size() > 0) {
            return locations.get(0);
        }

        return null;
    }

    /**
     * Returns the MapLocation where a Tower should be spawned, using only the map locations surrounding the robot.
     */
    public MapLocation getTowerSpawnLocation() {
        return _getTowerSpawnLocation(navigation.getOrderedMapLocations());
    }

    /**
     * Returns the MapLocation where a Tower should be spawned.
     * It accepts an array of map locations to consider first, such as the locations a tower recommends.
     */
    public MapLocation getTowerSpawnLocation(MapLocation[] idealLocations) {
        MapLocation ret = _getTowerSpawnLocation(idealLocations);
        if(ret != null) return ret;
        return _getTowerSpawnLocation(navigation.getOrderedMapLocations());
    }

    /**
     * Returns the MapLocation where a Tower should be spawned.
     */
    private MapLocation _getTowerSpawnLocation(MapLocation[] locations) {
        for(int c = 0; c < locations.length; c++) {
            if(!navigation.isLocationFree(locations[c], false)) {
                locations[c] = null;
            }
        }

        MapLocation closestLocation = navigation.findClosest(locations);
        return closestLocation;
    }

    /**
     * Spawns a robot.
     * The method checks the energon level of the robot first.
     * It first calculates the first free spot that the archon can turn to face to
     * spawn the unit.  If no spaces are available, the archon returns with an error code.
     *
     * If a robot gets in the way when the archon attempts to execute the spawn, it will recursively call the method.
     */
    private int spawnRobot(RobotType robot) {
        if(controller.getEnergonLevel() < robot.spawnCost() + 10) {
            return Status.notEnoughEnergon;
        }

        //type - true = ground, false = air
        boolean isAirUnit = robot == RobotType.ARCHON;
        spawnLocation = getSpawnLocation(isAirUnit);

        if(spawnLocation == null) {
            return Status.fail;
        }

        navigation.faceLocation(spawnLocation);

        //check the 8 tiles around me for a spawn location
        try {
            if(navigation.isLocationFree(controller.getLocation().add(controller.getDirection()), isAirUnit)) {
                previousSpawnType = robot;
                controller.spawn(robot);
                //p("Spawned "+robot);
                controller.yield();

                // send data
            } else {
                return spawnRobot(robot);
            }
        } catch(GameActionException e) {
            pa("----Caught Exception in spawnRobot.  robot: " + robot.toString() + " spawnLocation: " + spawnLocation.toString() + " Exception: " + e.toString());
            return Status.fail;
        }
        return Status.success;
    }

    /**
     * Updates the spawning mode to the 2009 spawning code.
     */
    public void changeModeToOld() {
        mode = new OldSpawnMode();
    }

    /**
     * Updates the spawning mode to spawn flux collecting units.
     */
    public void changeModeToCollectingFlux() {
        mode = new CollectingFluxSpawnMode();
    }

    public void changeModeToAttacking() {
        if(!(mode instanceof AttackingSpawnMode)) mode = new AttackingSpawnMode();
    }

    /**
     * The purpose of this class is to provide a way to change the spawning mechanism throughout the game.
     */
    abstract class SpawnMode {

        /**
         * Returns the type of robot that should be spawned next.
         */
        public abstract RobotType getNextRobotSpawnType();

        public int woutCount, chainerCount, turretCount, archonCount, soldierCount, totalCount;
        public double woutPercent, turretPercent, chainerPercent, soldierPercent;

        public void senseRobotCount() {
            ArrayList<RobotInfo> robots = sensing.senseGroundRobotInfo();
            woutCount = chainerCount = turretCount = archonCount = soldierCount = 0;

            for(RobotInfo robot : robots) {
                if(robot.team == player.team) {
                    switch(robot.type) {
                        case WOUT:
                            woutCount++;
                            break;
                        case CHAINER:
                            chainerCount++;
                            break;
                        case SOLDIER:
                            soldierCount++;
                            break;
                        case TURRET:
                            turretCount++;
                            break;
                    }
                }
            }
            archonCount = sensing.senseArchonLocations().length;
            totalCount = chainerCount+soldierCount+woutCount+turretCount;
        }

        public void senseRobotPercents() {
            senseRobotCount();

            woutPercent = ((double) woutCount) / ((double) totalCount);
            turretPercent = ((double) turretCount) / ((double) totalCount);
            chainerPercent = ((double) chainerCount) / ((double) totalCount);
            soldierPercent = ((double) soldierCount) / ((double) totalCount);
        }
    }

    class AttackingSpawnMode extends SpawnMode {
        public int[] numWouts;
        public int index;

        public int mostWouts, mostWoutsTurn;

        public Hashtable<Integer, Integer> robotTable;
        public LinkedList<Integer> robotList;
        public final int tolerance = 40;

        public AttackingSpawnMode() {
            super();
            numWouts = new int[30];
            index = 0;
            robotTable = new Hashtable<Integer, Integer>();
            robotList = new LinkedList<Integer>();
        }

        public RobotType getNextRobotSpawnType() {
            ArrayList<RobotInfo> robots = sensing.senseGroundRobotInfo();
            int round = Clock.getRoundNum(), id, result;
            soldierCount = chainerCount = 0;
            for(RobotInfo robot : robots) {
                if(robot.team == player.team) {
                    if(robot.type == RobotType.WOUT) {
                        id = robot.id;
                        if(robotTable.containsKey(id)) {
                            result = robotTable.get(id);
                            if(result+tolerance < round) {
                                robotList.add(id);
                            }
                            robotTable.put(id, round);
                        } else {
                            robotList.add(id);
                            robotTable.put(id, round);
                        }
                    } else if(robot.type == RobotType.SOLDIER) {
                        soldierCount++;
                    } else if(robot.type == RobotType.CHAINER) {
                        chainerCount++;
                    }
                }
            }
            Iterator<Integer> i = robotList.iterator();
            int robot;
            while(i.hasNext()) {
                robot = i.next();
                if(robotTable.get(robot)+tolerance < round) {
                    i.remove();
                }
            }
            
            //senseRobotCount();
            /*numWouts[index] = woutCount;
            index = (index+1) % numWouts.length;
            
            int sum = 0;
            for(int c = 0; c < numWouts.length; c++) {
                sum += numWouts[c];
            }
            int average = sum / numWouts.length;*/


            if(mostWoutsTurn+40 > Clock.getRoundNum() || woutCount > mostWouts) {
                mostWouts = woutCount;
                mostWoutsTurn = Clock.getRoundNum();
            }

            
            //p(woutCount+" "+sum+" "+average+" "+index);
            if(robotList.size() > 1) {
                //p("Returning Chainer");
                if(soldierCount > 1 || chainerCount < 2) {
                    return RobotType.CHAINER;
                }
                return RobotType.SOLDIER;
                //return RobotType.CHAINER;
            }
            //p("Returning Wout");
            return RobotType.WOUT;
            //return previousSpawnType == null || previousSpawnType == RobotType.CHAINER ? RobotType.WOUT : RobotType.CHAINER;
        }
    }

    class CollectingFluxSpawnMode extends SpawnMode {
        public RobotType getNextRobotSpawnType() {
            return RobotType.WOUT;
        }
    }

    class OldSpawnMode extends SpawnMode {
        public double goalChainers = 0.6, goalTurrets = -0.2, goalSoldiers = -0.2, goalWouts = 0.4;
        
        public RobotType getNextRobotSpawnType() {

            senseRobotPercents();
            
            double woutDifference = goalWouts - woutPercent;
            double cannonDifference = goalTurrets - turretPercent;
            double channelerDifference = goalChainers - chainerPercent;
            double soldierDifference = goalSoldiers - soldierPercent;

            for(double minDiff = 0.; minDiff >= -.2; minDiff -= .1) {
                for(double diff = .3; diff >= -.1; diff -= .1) {
                    if(woutDifference > diff && woutDifference > minDiff) {
                        return RobotType.WOUT;
                    }
                    if(cannonDifference > diff && cannonDifference > minDiff) {
                        return RobotType.TURRET;
                    }
                    if(soldierDifference > diff && soldierDifference > minDiff) {
                        return RobotType.SOLDIER;
                    }
                    if(channelerDifference > diff && channelerDifference > minDiff) {
                        return RobotType.CHAINER;
                    }

                }
            }
            return null;
        }
    }
}
