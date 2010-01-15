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

    public ArchonPlayer(RobotController controller) {
        super(controller);
        spawning = new SporadicSpawning(this);
        minMoveTurns = RobotType.ARCHON.moveDelayDiagonal() + 2;
    }

    public void step() {
        // reevaluate goal here?
        //sensing.senseAllTiles();
        switch(currentGoal) {
            case Goal.collectingFlux:
                spawning.changeMode(SpawnMode.collectingFlux);
                navigation.changeToMoveableDirectionGoal(true);

                spawning.spawnRobot();
                if(moveTurns >= minMoveTurns && controller.getRoundsUntilMovementIdle() == 0) {
                    navigation.moveOnce(true);
                    moveTurns = 0;
                }
                if(spawning.canSupportTower(RobotType.TELEPORTER)) {
                    //System.out.println("Can support it");
                    setGoal(Goal.placingTower);
                }
                moveTurns++;
                break;
            case Goal.placingTower:
                navigation.changeToTowerGoal(true);
                if(navigation.goal.done()) {
                    try {
                        controller.spawn(RobotType.TELEPORTER);
                        sensing.teleporterLocations.add(controller.getLocation());
                        setGoal(Goal.collectingFlux);
                    } catch(Exception e) {
                        pa("---Caught exception while spawning tower.");
                    }
                } else {
                    navigation.moveOnce(false);
                }
                
                break;
            case Goal.idle:
                // reevaluate the goal
                break;
        }
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
