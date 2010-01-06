package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class ScoutPlayer extends AttackPlayer {

    public int[] verticalDeltas = new int[] {-6, 0, -5, 3, -4, 4, -3, 5, -2, 5, -1, 5, 0, 6, 1, 5, 2, 5, 3, 5, 4, 4, 5, 3, 6, 0};
    public int[] horizontalDeltas = new int[] {0, -6, 3, -5, 4, -4, 5, -3, 5, -2, 5, -1, 6, 0, 5, 1, 5, 2, 5, 3, 4, 4, 3, 5, 0, 6};
    public int[] diagonalDeltas = new int[] {5, -3, 5, -2, 5, 0, 6, 0, 5, 1, 5, 2, 5, 3, 4, 3, 4, 4, 3, 4, 3, 5, 2, 5, 1, 5, 0, 5, 0, 6};

    public ScoutPlayer(RobotController controller) {
        super(controller);
        controller.setIndicatorString(2, "Scout");
        System.out.println("Scout spawned!");
        messaging.sendNewUnit();
    }

    public void run() {
        boot();
        while(true) {
            int startTurn = Clock.getRoundNum();
            messaging.parseMessages();
            if(energon.isEnergonLow()) {
                energon.requestEnergonTransfer();
            } else {
                energon.autoTransferEnergonBetweenUnits();
            }

            switch(currentGoal) {
                case Goal.alliedUnitRelay:
                    sendLowAlliedUnits();
                    break;
                case Goal.scouting:
                    break;
            }

            if(startTurn == Clock.getRoundNum() || controller.hasActionSet()) {
                controller.yield();
            }
        }

    }

    public void boot() {
        setGoal(Goal.scouting);
    }

    public void sendLowAlliedUnits() {
        ArrayList<RobotInfo> robots = sensing.robotCache.getGroundRobotInfo();
        ArrayList<RobotInfo> lowRobots = new ArrayList<RobotInfo>();

        for(RobotInfo robot : robots) {
            //if(isEnergonLow(robot))
            lowRobots.add(robot);
        }

        int[] ints = new int[(lowRobots.size() * 3) + 3];
        MapLocation[] locations = new MapLocation[lowRobots.size()];

        ints[0] = BroadcastMessage.LOW_ALLIED_UNITS;
        ints[1] = -1;
        ints[2] = lowRobots.size();

        int c = 3, d = 0;
        for(RobotInfo robot : lowRobots) {
            ints[c] = (int) robot.energonLevel;
            ints[c + 1] = (int) robot.energonReserve;
            ints[c + 2] = (int) robot.maxEnergon;
            locations[d] = robot.location;
            c += 3;
            d++;
        }

        messaging.addMessage(ints, null, locations);
    }

    @Override
    public int calculateMovementDelay(int heightFrom, int heightTo, boolean diagonal) {
        return diagonal ? moveDiagonalDelay : moveStraightDelay;
    }

    public void senseNewTiles() {
        sensing.senseDeltas(verticalDeltas, horizontalDeltas, diagonalDeltas);
    }
}
