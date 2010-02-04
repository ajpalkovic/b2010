package team002;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class RobotPlayer implements Runnable {

    private final RobotController controller;

    public RobotPlayer(RobotController rc) {
        this.controller = rc;
    }
    public int moveAmount = 15;

    public void run() {
        if(controller.getRobotType() == RobotType.ARCHON) {
            try {
                controller.setDirection(Direction.SOUTH_EAST);
                controller.yield();
                controller.yield();
                controller.yield();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        while(true) {
            try {
                if(controller.isMovementActive() || controller.hasActionSet() || controller.getRoundsUntilMovementIdle() > 0) {
                    controller.yield();
                    continue;
                }

                if(controller.getRobotType() == RobotType.ARCHON) {
                    if(moveAmount > 0) {
                        if(controller.canMove(controller.getDirection())) {
                            controller.moveForward();
                            moveAmount--;
                        }
                    } else {
                        int woutCount = 0;
                        Robot[] groundRobots = controller.senseNearbyGroundRobots();
                        for(Robot robot : groundRobots) {
                            RobotInfo info = controller.senseRobotInfo(robot);
                            if(info.team != controller.getTeam()) continue;
                            woutCount++;
                            if(info.energonLevel+5 > info.maxEnergon) continue;
                            if(!info.location.isAdjacentTo(controller.getLocation())) continue;
                            if(controller.getEnergonLevel() < 5) continue;

                            controller.transferUnitEnergon(1, info.location, RobotLevel.ON_GROUND);
                        }

                        if(controller.getEnergonLevel() > RobotType.TURRET.spawnCost() && woutCount < 9) {
                            if(getSpawnDirection()) {
                                controller.spawn(RobotType.TURRET);
                                controller.yield();
                                continue;
                            }
                        }
                    }
                } else {
                    if(controller.isAttackActive() || controller.getRoundsUntilAttackIdle() > 0 || controller.hasActionSet()) {
                        controller.yield();
                        continue;
                    }
                    
                    Robot[] groundRobots = controller.senseNearbyGroundRobots();
                    boolean good = true;
                    for(Robot robot : groundRobots) {
                        RobotInfo info = controller.senseRobotInfo(robot);
                        if(info.team == controller.getTeam()) continue;
                        good = false;
                        
                        if(!controller.canAttackSquare(info.location)) {
                            controller.setDirection(controller.getLocation().directionTo(info.location));
                            controller.yield();
                        }
                        if(controller.canAttackSquare(info.location)) {
                            controller.attackGround(info.location);
                            break;
                        }
                    }

                    if(good) controller.setDirection(controller.getDirection().rotateLeft());
                }

                controller.yield();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean getSpawnDirection() throws Exception {
        Robot robot = controller.senseGroundRobotAtLocation(controller.getLocation().add(controller.getDirection()));
        if(robot == null) {
            return true;
        }
        controller.setDirection(controller.getDirection().rotateRight());
        controller.yield();
        controller.yield();
        return false;
    }
}
