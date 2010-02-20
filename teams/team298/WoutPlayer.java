package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

import team298.Base.*;
import team298.NaughtyNavigation.LocationGoalWithBugPlanning;

public class WoutPlayer extends AttackPlayer {

    public int[] verticalDeltas = new int[] {-5, 0, -4, 3, -3, 4, -2, 4, -1, 4, 0, 5, 1, 4, 2, 4, 3, 4, 4, 3, 5, 0};
    public int[] horizontalDeltas = new int[] {0, -5, 3, -4, 4, -3, 4, -2, 4, -1, 5, 0, 4, 1, 4, 2, 4, 3, 3, 4, 0, 5};
    public int[] diagonalDeltas = new int[] {4, -3, 4, -2, 4, -1, 4, 0, 5, 0, 4, 1, 4, 2, 4, 3, 3, 3, 3, 4, 2, 4, 1, 4, 0, 4, 0, 5};
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
        towers = new TenaciousTowers(this);

        setGoal(Goal.collectingFlux);
    }

    public void step() {

        if(controller.getFlux() < 3000) {
            setGoal(Goal.collectingFlux);
        }

        switch(currentGoal) {
            case Goal.movingToTowerSpawnLocation:
                towers.moveToTowerSpawnLocation();
                break;
            case Goal.placingTeleporter:
                towers.placeTeleporter();
                break;
            case Goal.askingForTowerLocation:
                towers.askForTowerLocation();
                break;
            case Goal.movingToPreviousTowerLocation:
                towers.moveToPreviousTowerLocation();
                break;

            case Goal.collectingFlux:
                MapLocation location = sensing.senseClosestArchon();
                if(controller.getFlux() >= 3000) {
                    //p("Place Tower");
                    towers.placeTower();
                }
                if(location == null) {
                    return;
                }
                int distance = location.distanceSquaredTo(controller.getLocation());
                double maxDistance = Math.pow(controller.getEnergonLevel() / (controller.getRobotType().energonUpkeep() + 0.05) / controller.getRobotType().moveDelayOrthogonal(), 2);

                RobotInfo fluxTower = flux.autoTransferFlux();
                if(energon.isEnergonLow() || flux.isFluxFull() || distance > maxDistance || (energon.isEnergonSortaLow() && distance > 36)) {
                    //p("Archon Goal");
                    navigation.changeToArchonGoal(true);
                } else if(fluxTower != null) {
                    navigation.changeToLocationGoal(fluxTower.location, true);
                } else {
                    navigation.changeToWoutCollectingFluxGoal(true);
                }

                if((energon.isEnergonLow() || flux.isFluxFull()) && distance < 3) {
                    //p("request");
                    messaging.sendLowEnergon();
                    flux.transferFlux(location);
                } else {
                    //p("move once");
                    navigation.moveOnce(false);
                }

                //messaging.sendMessageForEnemyRobots();

                processEnemies();
                sortEnemies();
                EnemyInfo enemy = mode.getEnemyToAttack();

                if(enemy != null) {
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

    public void lowEnergonMessageCallback(MapLocation location1, int amount, int isAirUnit, int round) {
        if(isAirUnit == 1 && controller.getLocation().isAdjacentTo(location1)) {
            energon.transferEnergon(amount, location1, true);
        }
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
        towers.idealTowerSpawnLocations = locations;
    }
}
