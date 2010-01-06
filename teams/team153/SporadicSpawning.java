package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class SporadicSpawning extends Base {

    public MapStore map;
    public Messaging messaging;
    public Navigation navigation;
    public SensationalSensing sensing;
    public EnergeticEnergon energon;
    public double goalChannelers = -0.2, goalCannons = .6, goalSoldiers = -0.2, goalWorkers = .4, goalScouts = .1;
    public int maxWorkers = 2;
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
        if(controller.getEnergonLevel() < Math.min(unit.spawnCost() + 15, 74)) {
            return false;
        }

        ArrayList<RobotInfo> air = sensing.robotCache.getAirRobotInfo(), ground = sensing.robotCache.getGroundRobotInfo();
        double energonCost = 0;
        for(RobotInfo robot : ground) {
            if(robot.team.equals(player.team)) {
                energonCost += robot.type.energonUpkeep();
            }
        }

        double energonProduction = 1;
        for(RobotInfo robot : air) {
            if(robot.team.equals(player.team)) {
                if(robot.type == RobotType.ARCHON) {
                    energonProduction += 1;
                } else {
                    energonCost += robot.type.energonUpkeep();
                }
            }
        }

        // each archon should keep .3 for itself?
        return energonProduction > energonCost + (energonProduction * .2);
    }

    /**
     * Returns the type of robot that should be spawned next.
     */
    public RobotType getNextRobotSpawnType() {
        ArrayList<RobotInfo> robots = sensing.robotCache.getGroundRobotInfo(), robots2 = sensing.robotCache.getAirRobotInfo();
        ArrayList<RobotInfo> channelers = new ArrayList<RobotInfo>(), cannons = new ArrayList<RobotInfo>(), soldiers = new ArrayList<RobotInfo>(),
                workers = new ArrayList<RobotInfo>(), scouts = new ArrayList<RobotInfo>();
        for(RobotInfo robot : robots) {
            if(robot.team == player.team) {
                if(robot.type == RobotType.CANNON) {
                    cannons.add(robot);
                } else if(robot.type == RobotType.CHANNELER) {
                    channelers.add(robot);
                } else if(robot.type == RobotType.SOLDIER) {
                    soldiers.add(robot);
                } else if(robot.type == RobotType.WORKER) {
                    workers.add(robot);
                }
            }
        }
        for(RobotInfo robot : robots2) {
            if(robot.team == player.team) {
                if(robot.type == RobotType.SCOUT) {
                    scouts.add(robot);
                }
            }
        }

        int total = cannons.size() + soldiers.size() + workers.size() + channelers.size() + scouts.size();
        if(total == 0) {
            return RobotType.WORKER;
        }

        double workerPercent = (double) workers.size() / total;
        double cannonPercent = (double) cannons.size() / total;
        double channelerPercent = (double) channelers.size() / total;
        double soldierPercent = (double) soldiers.size() / total;
        double scoutPercent = (double) scouts.size() / total;

        double workerDifference = goalWorkers - workerPercent;
        double cannonDifference = goalCannons - cannonPercent;
        double channelerDifference = goalChannelers - channelerPercent;
        double soldierDifference = goalSoldiers - soldierPercent;
        double scoutDifference = goalScouts - scoutPercent;

        for(double minDiff = 0.; minDiff >= -.2; minDiff -= .1) {
            for(double diff = .3; diff >= -.1; diff -= .1) {
                if(workers.size() < maxWorkers && workerDifference > diff && workerDifference > minDiff && workers.size() < 2 && player.currentGoal == Goal.collectingFlux) {
                    return RobotType.WORKER;
                }
                if(cannonDifference > diff && cannonDifference > minDiff) {
                    return RobotType.CANNON;
                }
                if(soldierDifference > diff && soldierDifference > minDiff) {
                    return RobotType.SOLDIER;
                }
                if(channelerDifference > diff && channelerDifference > minDiff) {
                    return RobotType.CHANNELER;
                }
                if(scoutDifference > diff && scoutDifference > minDiff) {
                    return RobotType.SCOUT;
                }

            }
        }
        return null;
    }

    /**
     * Returns the first MapLocation of the 8 around an Archon which does not have a unit at that location.
     */
    public MapData getSpawnLocation(boolean isAirUnit) {
        MapData flux = null;
        try {
            FluxDeposit[] f = controller.senseNearbyFluxDeposits();
            if(f.length > 0) {
                flux = map.get(controller.senseFluxDepositInfo(f[0]).location);
            }
        } catch(Exception e) {
        }

        ArrayList<MapData> locations = new ArrayList<MapData>();
        MapData[] orderedLocations = navigation.getOrderedMapLocations();
        for(MapData location : orderedLocations) {
            if(location != null && navigation.isLocationFree(location.toMapLocation(), isAirUnit)) {
                locations.add(location);
            }
        }

        if(flux != null && controller.getLocation().equals(flux.toMapLocation())) {
            // i am on the flux deposit, spawn a unit around me
            // maybe try to balance an equal number of units on each side, but take care of walls too
            if(locations.size() > 0) {
                return locations.get(0);
            }
        } else if(player.currentGoal == Goal.supporttingFluxDeposit) {
            if(flux == null) {
                if(locations.size() > 0) {
                    return locations.get(0);
                } else {
                    return null;
                }
            }

            // spawn units away from the flux deposit
            MapData ret = null;
            int distance = Integer.MIN_VALUE;
            for(MapData location : locations) {
                int d = location.toMapLocation().distanceSquaredTo(flux.toMapLocation());
                if(d > distance) {
                    distance = d;
                    ret = location;
                }
            }

            return ret;
        } else {
            if(locations.size() > 0) {
                return locations.get(0);
            }
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
        boolean isAirUnit = (robot == RobotType.SCOUT || robot == RobotType.ARCHON);
        MapData spawnLocation = getSpawnLocation(isAirUnit);

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
                if(robot == RobotType.SCOUT) {
                    messaging.sendScoutAlliedUnitRelay();
                }
                // send data
            } else {
                return spawnRobot(robot);
            }
        } catch(GameActionException e) {
            System.out.println("----Caught Exception in spawnRobot.  robot: " + robot.toString() + " spawnLocation: " + spawnLocation.toString() + " Exception: " + e.toString());
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
