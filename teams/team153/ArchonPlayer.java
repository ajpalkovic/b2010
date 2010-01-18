package team153;

import battlecode.common.*;
import battlecode.common.TerrainTile.TerrainType;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class ArchonPlayer extends NovaPlayer {

    public int[] verticalDeltas = new int[] {-6, 0, -5, 3, -4, 4, -3, 5, -2, 5, -1, 5, 0, 6, 1, 5, 2, 5, 3, 5, 4, 4, 5, 3, 6, 0};
    public int[] horizontalDeltas = new int[] {0, -6, 3, -5, 4, -4, 5, -3, 5, -2, 5, -1, 6, 0, 5, 1, 5, 2, 5, 3, 4, 4, 3, 5, 0, 6};
    public int[] diagonalDeltas = new int[] {5, -3, 5, -2, 5, 0, 6, 0, 5, 1, 5, 2, 5, 3, 4, 3, 4, 4, 3, 4, 3, 5, 2, 5, 1, 5, 0, 5, 0, 6};
    public int archonNumber = 0;
    public int archonGroup = -1;
    public SporadicSpawning spawning;
    public int minMoveTurns = 0, moveTurns = 0;

    public MapLocation spawnFromLocation;
    public MapLocation[] idealSpawnLocations;
    public int turnsWaitedForSpawnLocationMessage = 0;

    public ArchonPlayer(RobotController controller) {
        super(controller);
        spawning = new SporadicSpawning(this);
        minMoveTurns = RobotType.ARCHON.moveDelayDiagonal() + 2;
    }

    public void step() {
        // reevaluate goal here?
        //sensing.senseAllTiles();
        switch(currentGoal) {
            case Goal.idle:
            case Goal.collectingFlux:
                spawning.changeModeToCollectingFlux();
                navigation.changeToMoveableDirectionGoal(true);

                spawning.spawnRobot();
                if(moveTurns >= minMoveTurns && controller.getRoundsUntilMovementIdle() == 0) {
                    navigation.moveOnce(true);
                    moveTurns = 0;
                }
                if(spawning.canSupportTower(RobotType.TELEPORTER)) {
                    //System.out.println("Can support it");
                    placeTower();
                }
                moveTurns++;
                break;
            case Goal.askingForTowerLocation:

                break;
            case Goal.movingToPreviousTowerLocation:

                break;
            case Goal.placingTeleporter:
                if(navigation.goal.done()) {
                    if(spawning.spawnTower(RobotType.TELEPORTER) != Status.success) {
                        placeTower();
                    } else {
                        setGoal(Goal.collectingFlux);
                    }
                } else {
                    navigation.moveOnce(false);
                }
                break;
            case Goal.movingToTowerSpawnLocation:
                if(navigation.goal.done()) {
                    if(spawning.spawnTower(RobotType.AURA) != Status.success) {
                        placeTower();
                    } else {
                        setGoal(Goal.collectingFlux);
                    }
                } else {
                    navigation.moveOnce(false);
                }
                
                break;
        }
    }

    public void placeTower() {
        idealSpawnLocations = null;
        turnsWaitedForSpawnLocationMessage = 0;
        spawnFromLocation = null;

        ArrayList<MapLocation> towers = sensing.senseAlliedTeleporters();
        if(towers.size() > 0) {
            //there are teles in range, ask them where to build
            setGoal(Goal.askingForTowerLocation);
            return;
        }

        towers = sensing.senseAlliedTowerLocations();
        if(towers.size() > 0) {
            //no teles in range, but there are other towers.  they should be talking to the tele and should know the status of where to build
            setGoal(Goal.askingForTowerLocation);
            return;
        }


        towers = sensing.senseKnownAlliedTowerLocations();
        if(towers.size() > 0) {
            //we remember that there used to be a tower here, so lets try going there.  once we get there, we can ask again
            setGoal(Goal.movingToPreviousTowerLocation);
            return;
        }

        //there were no towers in range ever, so lets just build a new one:
        MapLocation location = spawning.getTowerSpawnLocation();
        if(location == null) {
            pa("WTF.  There is nowhere to spawn the tower.");
            return;
        }
        spawnFromLocation = location.subtract(controller.getLocation().directionTo(location));
        navigation.changeToLocationGoal(spawnFromLocation, true);
        controller.setIndicatorString(2, spawnFromLocation.toString());
        setGoal(Goal.placingTeleporter);
    }

    public void boot() {
        team = controller.getTeam();
        senseArchonNumber();
        setGoal(Goal.collectingFlux);
        if(archonNumber < 5) {
            setGoal(Goal.collectingFlux);
            archonGroup = 1;
        }
        if(archonNumber % 2 == 0) {
            //message to other archon
        }

    }

    /**
     * Calculates the order in which the archons were spawned.
     */
    public void senseArchonNumber() {
        Message[] messages = controller.getAllMessages();
        int min = 1;
        for(Message m : messages) {
            if(m.ints[0] >= min) {
                min = m.ints[0] + 1;
            }
        }

        archonNumber = min;

        Message m = new Message();
        m.ints = new int[] {min};
        try {
            controller.broadcast(m);
        } catch(Exception e) {
            System.out.println("----Caught Exception in senseArchonNumber.  Exception: " + e.toString());
        }
        System.out.println("Number: " + min);
    }

    public void senseNewTiles() {
        sensing.senseDeltas(verticalDeltas, horizontalDeltas, diagonalDeltas);
    }
}
