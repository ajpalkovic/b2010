package team298;

import battlecode.common.*;
import battlecode.common.TerrainTile.TerrainType;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class TenaciousTowers extends Base {

    public SporadicSpawning spawning;
    public MexicanMessaging messaging;
    public NaughtyNavigation navigation;
    public SensationalSensing sensing;
    public EnergeticEnergon energon;
    public FluxinFlux flux;
    public MapLocation towerSpawnFromLocation, towerSpawnLocation;
    public MapLocation[] idealTowerSpawnLocations;
    public int turnsWaitedForTowerSpawnLocationMessage = 0, turnsWaitedForMove = 0, turnsLookingForTower = 0;

    public TenaciousTowers(NovaPlayer player) {
        super(player);
        spawning = player.spawning;
        messaging = player.messaging;
        navigation = player.navigation;
        sensing = player.sensing;
        energon = player.energon;
        flux = player.flux;
    }

    public void doTowerStuff(boolean attacking) {
        if(attacking) {
            ArrayList<RobotInfo> robots = sensing.senseGroundRobotInfo();
            for(RobotInfo robot : robots) {
                if(robot.type == RobotType.WOUT) {
                    if(robot.location.isAdjacentTo(controller.getLocation())) {
                        flux.fluxUpWout(robot.location);
                        sensing.senseAlliedTeleporters();
                        if(sensing.knownAlliedTowerLocations == null) {
                            sensing.senseAlliedTowers();
                        }
                        if(sensing.knownAlliedTowerLocations != null && !sensing.knownAlliedTowerLocations.isEmpty()) {
                            MapLocation loc = navigation.findClosest(new ArrayList<MapLocation>(sensing.knownAlliedTowerLocations.values()));
                            messaging.sendTowerPing(sensing.knownAlliedTowerIDs.get(loc.getX() + "," + loc.getY()), loc);
                        }
                    }
                }
            }
        } else {
            placeTower();
        }
    }

    public void askForTowerLocation() {
        turnsWaitedForTowerSpawnLocationMessage++;

        if(idealTowerSpawnLocations != null) {
            //we got the message, lets do something
            if(idealTowerSpawnLocations.length > 0) {
                towerSpawnLocation = spawning.getTowerSpawnLocation(idealTowerSpawnLocations);
                towerSpawnFromLocation = towerSpawnLocation.subtract(controller.getLocation().directionTo(towerSpawnLocation));
                navigation.changeToLocationGoal(towerSpawnFromLocation, true);
                turnsWaitedForTowerSpawnLocationMessage = 0;
                player.setGoal(Goal.movingToTowerSpawnLocation);
            }
        }
        if(turnsWaitedForTowerSpawnLocationMessage > 5) {
            checkKnownTowerLocations();
        }
    }

    public void moveToPreviousTowerLocation() {
        if(navigation.goal.done()) {
            //we shouldn't ever get here, but who knows
            placeTower();
        } else {
            if(sensing.senseAlliedTowers().size() > 0) {
                placeTower();
            } else {
                navigation.moveOnce(false);
            }
        }
    }

    public void placeTeleporter() {
        if(navigation.goal.done()) {
            navigation.faceLocation(towerSpawnLocation);
            if(spawning.spawnTower(RobotType.TELEPORTER) != Status.success) {
                placeTower();
            } else {
                player.setGoal(Goal.collectingFlux);
            }
        } else {
            navigation.moveOnce(false);
        }
    }

    public void moveToTowerSpawnLocation() {
        if(navigation.goal.done()) {
            navigation.faceLocation(towerSpawnLocation);
            if(controller.getLocation().directionTo(towerSpawnLocation).isDiagonal()) {
                navigation.moveOnce(false);
            }
            if(navigation.isLocationFree(towerSpawnLocation, false)) {
                if(spawning.spawnTower(RobotType.AURA) != Status.success) {
                    placeTower();
                } else {
                    player.setGoal(Goal.collectingFlux);
                }
            } else {
                if(turnsWaitedForMove > 5) {
                    placeTower();
                } else {
                    messaging.sendMove(towerSpawnLocation);
                    turnsWaitedForMove++;
                }
            }

        } else {
            if(player.isWout) {
                MapLocation goalLocation = navigation.locationGoalWithBugPlanning.location;
                if(navigation.goal instanceof BendoverBugging && !((BendoverBugging) navigation.goal).isGoalAttainable()) {
                    navigation.changeToLocationGoal(goalLocation.subtract(controller.getLocation().directionTo(goalLocation)), true);
                    //System.out.println("changing goal");
                }
            }
            navigation.moveOnce(false);
        }
    }

    public void placeTower() {
        idealTowerSpawnLocations = null;
        turnsWaitedForTowerSpawnLocationMessage = 0;
        towerSpawnFromLocation = null;
        towerSpawnLocation = null;
        turnsWaitedForMove = 0;

        if(controller.getFlux() < 3000) {
            player.setGoal(Goal.collectingFlux);
        }

        ArrayList<MapLocation> towers; ////sensing.senseAlliedTeleporters();
        int towerID = BroadcastMessage.everyone;
        MapLocation location;
        Robot robot;


        towers = sensing.senseAlliedTowerLocations();
        if(towers.size() > 0) {
            //no teles in range, but there are other towers.  they should be talking to the tele and should know the status of where to build
            try {
                location = navigation.findClosest(towers);
                if(controller.canSenseSquare(location) && location != null) {
                    robot = controller.senseGroundRobotAtLocation(location);
                    if(robot != null) {
                        towerID = robot.getID();
                    } else {
                        sensing.knownAlliedTowerLocations.remove(sensing.knownAlliedTowerIDs.remove(location.getX() + "," + location.getY()));
                    }
                }
                messaging.sendTowerBuildLocationRequest(towerID);
                player.setGoal(Goal.askingForTowerLocation);
                return;
            } catch(Exception e) {
                pa("----Caught exception in place tower. " + e.toString());
            }
        }

        //no towers in range, lets just ask everyone
        messaging.sendTowerBuildLocationRequest(BroadcastMessage.everyone);
        player.setGoal(Goal.askingForTowerLocation);
        return;
    }

    public void checkKnownTowerLocations() {
        Robot robot = null;

        ArrayList<MapLocation> towers = sensing.senseKnownAlliedTowerLocations();
        if(towers.size() > 0) {
            //we remember that there used to be a tower here, so lets try going there.  once we get there, we can ask again
            MapLocation closest = navigation.findClosest(towers);
            if(controller.canSenseSquare(closest) && closest != null) {
                if(player.isWout) {
                    double distance = Math.abs(controller.getLocation().getX() - closest.getX()) + Math.abs(controller.getLocation().getY() - closest.getY());
                    double energonCost = distance * 0.2;
                    if(energonCost + 1 < controller.getEnergonLevel()) {
                        spawnTeleporter();
                        return;
                    }
                }

                try {
                    robot = controller.senseGroundRobotAtLocation(closest);
                } catch(Exception e) {
                    pa("---Caught exception in checkKnownTowerLocations." + e);
                }
                if(sensing.knownAlliedTowerIDs == null) {
                    sensing.senseAlliedTowers();
                }
                if(robot == null && sensing.knownAlliedTowerIDs != null) {
                    int id = sensing.knownAlliedTowerIDs.remove(closest.getX() + "," + closest.getY());
                    sensing.knownAlliedTowerLocations.remove(id);

                    checkKnownTowerLocations();
                    return;
                }
            }
            navigation.changeToLocationGoal(closest, true);
            player.setGoal(Goal.movingToPreviousTowerLocation);

            return;
        }

        spawnTeleporter();
    }

    public void spawnTeleporter() {
        //there were no towers in range ever, so lets just build a new one:
        towerSpawnLocation = spawning.getTowerSpawnLocation();
        if(towerSpawnLocation == null) {
            pa("WTF.  There is nowhere to spawn the tower.");
            return;
        }
        towerSpawnFromLocation = towerSpawnLocation.subtract(controller.getLocation().directionTo(towerSpawnLocation));
        navigation.changeToLocationGoal(towerSpawnFromLocation, true);
        controller.setIndicatorString(2, towerSpawnFromLocation.toString());
        player.setGoal(Goal.placingTeleporter);
    }
}
