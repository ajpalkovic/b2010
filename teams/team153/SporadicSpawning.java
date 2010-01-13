package team153;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class SporadicSpawning extends Base {

    public MapStore map;
    public MexicanMessaging messaging;
    public NaughtyNavigation navigation;
    public SensationalSensing sensing;
    public EnergeticEnergon energon;
    public double goalChainers = -0.2, goalTurrets = .6, goalSoldiers = -0.2, goalWouts = .4;
    public int unitSpawned = 0;

    public SporadicSpawning(NovaPlayer player) {
        super(player);
        map = player.map;
        messaging = player.messaging;
        navigation = player.navigation;
        sensing = player.sensing;
        energon = player.energon;
    }

    /**
     * Returns true if this archon can support the unit's energon requirements.
     */
    public boolean canSupportUnit(RobotType unit) {
        if(controller.getEnergonLevel() < Math.min(unit.spawnCost() + 25, 74)) {
            return false;
        }

        ArrayList<RobotInfo> air = sensing.robotCache.getAirRobotInfo(), ground = sensing.senseAlliedRobotInfoInSensorRange();
        double energonCost = 0;
        for(RobotInfo robot : ground) {
            energonCost += robot.type.energonUpkeep();
        }

        double energonProduction = 1;
        for(RobotInfo robot : air) {
            if(robot.team.equals(player.team)) {
                energonProduction += 1;
            }
        }

        // each archon should keep .3 for itself?
        return energonProduction > energonCost + (energonProduction * .2);
    }

    /**
     * Returns the type of robot that should be spawned next.
     */
    public RobotType getNextRobotSpawnType() {
        ArrayList<RobotInfo> robots = sensing.robotCache.getGroundRobotInfo();
        ArrayList<RobotInfo> chainers = new ArrayList<RobotInfo>(),
                turrets = new ArrayList<RobotInfo>(),
                soldiers = new ArrayList<RobotInfo>(),
                wouts = new ArrayList<RobotInfo>();

        for(RobotInfo robot : robots) {
            if(robot.team == player.team) {
                if(robot.type == RobotType.WOUT) {
                    wouts.add(robot);
                } else if(robot.type == RobotType.CHAINER) {
                    chainers.add(robot);
                } else if(robot.type == RobotType.SOLDIER) {
                    soldiers.add(robot);
                } else if(robot.type == RobotType.TURRET) {
                    turrets.add(robot);
                }
            }
        }

        int total = chainers.size() + soldiers.size() + wouts.size() + turrets.size();
        if(total == 0) {
            return RobotType.WOUT;
        }

        double woutPercent = (double) wouts.size() / total;
        double turretPercent = (double) turrets.size() / total;
        double chainerPercent = (double) chainers.size() / total;
        double soldierPercent = (double) soldiers.size() / total;

        double workerDifference = goalWouts - woutPercent;
        double cannonDifference = goalTurrets - turretPercent;
        double channelerDifference = goalChainers - chainerPercent;
        double soldierDifference = goalSoldiers - soldierPercent;

        for(double minDiff = 0.; minDiff >= -.2; minDiff -= .1) {
            for(double diff = .3; diff >= -.1; diff -= .1) {
                if(workerDifference > diff && workerDifference > minDiff && wouts.size() < 2) {
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

    /**
     * Returns the first MapLocation of the 8 around an Archon which does not have a unit at that location.
     */
    public NovaMapData getSpawnLocation(boolean isAirUnit) {
        ArrayList<NovaMapData> locations = new ArrayList<NovaMapData>();
        NovaMapData[] orderedLocations = navigation.getOrderedMapLocations();
        for(NovaMapData location : orderedLocations) {
            if(navigation.isLocationFree(location.toMapLocation(), isAirUnit)) {
                locations.add(location);
            }
        }
        if(locations.size() > 0) {
            return locations.get(0);
        }

        return null;
    }

    /**
     * Spawns a robot.
     * The method checks the energon level of the robot first.
     * It first calculates the first free spot that the archon can turn to face to
     * spawn the unit.  If no spaces are available, the archon returns with an error code.
     *
     * If a robot gets in the way when the archon attempts to execute the spawn, it will recursively call the method.
     */
    public int spawnRobot(RobotType robot) {
        if(controller.getEnergonLevel() < robot.spawnCost() + 10) {
            return Status.notEnoughEnergon;
        }

        //type - true = ground, false = air
        boolean isAirUnit = robot == RobotType.ARCHON;
        NovaMapData spawnLocation = getSpawnLocation(isAirUnit);

        if(spawnLocation == null) {
            return Status.fail;
        }

        navigation.faceLocation(spawnLocation.toMapLocation());

        //check the 8 tiles around me for a spawn location
        try {
            if(navigation.isLocationFree(controller.getLocation().add(controller.getDirection()), isAirUnit)) {
                controller.spawn(robot);
                unitSpawned++;
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
     * If the Archon can support another unit, then it will spawn a unit in accordance with the unit percentages above.
     */
    public void spawnFluxUnits() {
        RobotType spawnType = getNextRobotSpawnType();
        if(spawnType == null) {
            return;
        }

        if(!canSupportUnit(spawnType)) {
            return;
        }

        int turnsToWait = new Random().nextInt(4) + Clock.getRoundNum() + 1;
        while(Clock.getRoundNum() < turnsToWait) {
            messaging.parseMessages();
            energon.processEnergonTransferRequests();
            controller.yield();
        }

        RobotType spawnType2 = getNextRobotSpawnType();
        if(spawnType2 == null) {
            return;
        }

        if(spawnType == spawnType2) {
            spawnRobot(spawnType);
        }
    }
}
