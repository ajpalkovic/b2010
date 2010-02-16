package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

import team298.Base.BroadcastMessage;
import team298.Base.Goal;
import team298.Base.Status;
import team298.NaughtyNavigation.LocationGoalWithBugPlanning;

public class WoutPlayer extends AttackPlayer {

    public int[] verticalDeltas = new int[] {-5, 0, -4, 3, -3, 4, -2, 4, -1, 4, 0, 5, 1, 4, 2, 4, 3, 4, 4, 3, 5, 0};
    public int[] horizontalDeltas = new int[] {0, -5, 3, -4, 4, -3, 4, -2, 4, -1, 5, 0, 4, 1, 4, 2, 4, 3, 3, 4, 0, 5};
    public int[] diagonalDeltas = new int[] {4, -3, 4, -2, 4, -1, 4, 0, 5, 0, 4, 1, 4, 2, 4, 3, 3, 3, 3, 4, 2, 4, 1, 4, 0, 4, 0, 5};
    public MapLocation[] idealTowerSpawnLocations;
    public MapLocation towerSpawnFromLocation, towerSpawnLocation;
    public SporadicSpawning spawning;
    public int turnsWaitedForTowerSpawnLocationMessage = 0, turnsSinceLastSpawn = 0, turnsWaitedForMove = 0;
    public int[][] fluxDeltas = new int[][] {
        {-3, 0, 0},
        {-3, 1, 0},
        {-2, 2, 0},
        {-1, 3, 0},
        {0, 3, 0},
        {1, 3, 0},
        {2, 2, 0},
        {3, 1, 0},
        {3, 0, 0},
        {3, -1, 0},
        {2, -2, 0},
        {1, -3, 0},
        {0, -3, 0},
        {-1, -3, 0},
        {-2, -2, 0},
        {-3, -1, 0}
    };

    public WoutPlayer(RobotController controller) {
        super(controller);
        spawning = new SporadicSpawning(this);
        setGoal(Goal.collectingFlux);
    }

    public void step() {

        if(controller.getFlux() < 3000) {
            setGoal(Goal.collectingFlux);
        }

        switch(currentGoal) {
            case Goal.movingToTowerSpawnLocation:
                if(navigation.goal.done()) {
                    navigation.faceLocation(towerSpawnLocation);
                    if(navigation.isLocationFree(towerSpawnLocation, false)) {
                        if(spawning.spawnTower(RobotType.AURA) != Status.success) {
                            placeTower();
                        } else {
                            setGoal(Goal.collectingFlux);
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
                    //p("move once");
                	MapLocation goalLocation = ((LocationGoalWithBugPlanning) navigation.goal).location;
                	if (navigation.goal instanceof BendoverBugging && !((BendoverBugging)navigation.goal).isGoalAttainable()){
                		navigation.changeToLocationGoal(goalLocation.subtract(controller.getLocation().directionTo(goalLocation)),true);
                		System.out.println("changing goal");
                	}
                	navigation.moveOnce(false);
                }
                break;
            case Goal.placingTeleporter:
                if(navigation.goal.done()) {
                    navigation.faceLocation(towerSpawnLocation);
                    if(spawning.spawnTower(RobotType.TELEPORTER) != Status.success) {
                        placeTower();
                    } else {
                        setGoal(Goal.collectingFlux);
                    }
                } else {
                    navigation.moveOnce(false);
                }
                break;
            case Goal.askingForTowerLocation:
                turnsWaitedForTowerSpawnLocationMessage++;
                if(turnsWaitedForTowerSpawnLocationMessage > 5) {
                    checkKnownTowerLocations();
                }

                if(idealTowerSpawnLocations != null) {
                    //we got the message, lets do something
                    if(idealTowerSpawnLocations.length > 0) {
                        towerSpawnLocation = spawning.getTowerSpawnLocation(idealTowerSpawnLocations);
                        towerSpawnFromLocation = towerSpawnLocation.subtract(controller.getLocation().directionTo(towerSpawnLocation));
                        navigation.changeToLocationGoal(towerSpawnFromLocation, true);
                        setGoal(Goal.movingToTowerSpawnLocation);
                    }
                }
                break;
            case Goal.movingToPreviousTowerLocation:
                controller.setIndicatorString(2, "" + ((LocationGoalWithBugPlanning) navigation.goal).location);
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
                break;

            case Goal.collectingFlux:
                MapLocation location = sensing.senseClosestArchon();
                if(controller.getFlux() >= 3000) {
                    //p("Place Tower");
                    placeTower();
                }
                if(location == null) {
                    return;
                }
                int distance = location.distanceSquaredTo(controller.getLocation());

                RobotInfo fluxTower = energon.autoTransferFlux();
                if(energon.isEnergonLow() || energon.isFluxFull() || distance > 70 || (energon.isEnergonSortaLow() && distance > 36)) {
                    //p("Archon Goal");
                    navigation.changeToArchonGoal(true);
                } 
                else if(fluxTower != null) {
                    navigation.changeToLocationGoal(fluxTower.location, true);
                } else {
                    navigation.changeToWoutCollectingFluxGoal(true);
                }

                if((energon.isEnergonLow() || energon.isFluxFull()) && distance < 3) {
                    //p("request");
                    energon.transferFlux(location);
                    energon.requestEnergonTransfer();
                    controller.yield();
                } else {
                    //p("move once");
                    navigation.moveOnce(false);
                }

                //messaging.sendMessageForEnemyRobots();

                processEnemies();
                sortEnemies();
                EnemyInfo enemy = mode.getEnemyToAttack();

                if(enemy != null) {
                    messaging.sendClosestEnemyInSight();
                    turnsSinceEnemiesSeen = 0;
                    if(enemy.location.distanceSquaredTo(controller.getLocation()) <= 2) {
                        executeAttack(enemy.location, enemy.type.isAirborne() ? RobotLevel.IN_AIR : RobotLevel.ON_GROUND);
                    }
                } else {
                    turnsSinceEnemiesSeen++;
                }
                break;
        }
    }

    public void enemyInSight(MapLocation[] locations, int[] ints, String[] strings, int locationStart, int intStart, int stringStart, int count) {
    }

    public void followRequestMessageCallback(MapLocation location, int idOfSendingArchon, int idOfRecipient) {
    }

    public void senseNewTiles() {
        sensing.senseDeltas(verticalDeltas, horizontalDeltas, diagonalDeltas);
        sensing.senseFlux(fluxDeltas);
    }

    public void lowEnergonMessageCallback(MapLocation location1, int amount, int isAirUnit) {
    }

    public void placeTower() {
        idealTowerSpawnLocations = null;
        turnsWaitedForTowerSpawnLocationMessage = 0;
        towerSpawnFromLocation = null;
        towerSpawnLocation = null;
        turnsWaitedForMove = 0;
        if(controller.getFlux() < 3000) {
            setGoal(Goal.collectingFlux);
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
                setGoal(Goal.askingForTowerLocation);
                return;
            } catch(Exception e) {
                pa("----Caught exception in place tower. " + e.toString());
            }
        }

        //no towers in range, lets just ask everyone
        messaging.sendTowerBuildLocationRequest(BroadcastMessage.everyone);
        setGoal(Goal.askingForTowerLocation);
        return;
    }

    public void checkKnownTowerLocations() {
        Robot robot = null;

        ArrayList<MapLocation> towers = sensing.senseKnownAlliedTowerLocations();
        if(towers.size() > 0) {
            //we remember that there used to be a tower here, so lets try going there.  once we get there, we can ask again
            MapLocation closest = navigation.findClosest(towers);
            if(closest != null) {
                double distance = Math.abs(controller.getLocation().getX()-closest.getX()) + Math.abs(controller.getLocation().getY()-closest.getY());
                double energonCost = distance * 0.2;
                if(energonCost+2 > controller.getEnergonLevel()) {
                    if(controller.canSenseSquare(closest)) {
                        try {
                            robot = controller.senseGroundRobotAtLocation(closest);
                        } catch(Exception e) {
                            pa("---Caught exception in checkKnownTowerLocations."+e);
                        }
                        if(robot == null) {
                            int id = sensing.knownAlliedTowerIDs.remove(closest.getX() + "," + closest.getY());
                            sensing.knownAlliedTowerLocations.remove(id);
                        }
                    }
                    navigation.changeToLocationGoal(closest, true);
                    setGoal(Goal.movingToPreviousTowerLocation);
                    return;
                }
            }
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
        setGoal(Goal.placingTeleporter);
    }

    public void towerPingLocationCallback(MapLocation location, int robotID) {
        sensing.senseAlliedTowerLocations();
        if(!sensing.knownAlliedTowerLocations.containsKey(robotID)) {
            sensing.knownAlliedTowerLocations.put(new Integer(robotID), location);
            sensing.knownAlliedTowerIDs.put(location.getX() + "," + location.getY(), robotID);
        }
    }

    public void newUnit(int senderID, MapLocation location, String robotType) {
        if(RobotType.valueOf(robotType).isBuilding()) {
            if(sensing.knownAlliedTowerLocations == null) {
                sensing.senseAlliedTowerLocations();
            }

            if(!sensing.knownAlliedTowerLocations.containsKey(senderID)) {
                sensing.knownAlliedTowerLocations.put(new Integer(senderID), location);
                sensing.knownAlliedTowerIDs.put(location.getX() + "," + location.getY(), senderID);
            }
        }
    }

    public void towerBuildLocationResponseCallback(MapLocation[] locations) {
        idealTowerSpawnLocations = locations;
    }
}
