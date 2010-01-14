package team153;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class NaughtyNavigation extends Base {

    public MapStore map;
    public MexicanMessaging messaging;
    public SensationalSensing sensing;

    public NaughtyNavigation(NovaPlayer player) {
        super(player);
        map = player.map;
        messaging = player.messaging;
    }

    /**
     * Causes the robot to the face the specified direction if necessary.
     */
    public int faceDirection(Direction dir) {
        if(dir == null) {
            return Status.fail;
        }

        if(controller.getDirection().equals(dir)) {
            return Status.success;
        }

        if(dir.equals(Direction.OMNI)) {
            return Status.success;
        }

        while(controller.hasActionSet() || controller.getRoundsUntilMovementIdle() != 0) {
            controller.yield();
        }

        try {
            controller.setDirection(dir);
            controller.yield();
            return Status.success;
        } catch(Exception e) {
            System.out.println("----Caught Exception in faceDirection with dir: " + dir.toString() + " Exception: " + e.toString());
        }

        return Status.fail;
    }

    /**
     * Calculates the direction needed to turn and face the given location, such as
     * for when an Archon needs to turn to face an empty space to spawn a unit.
     * The robot will then wait until it's movement is idle and proceed to turn to that direction.
     */
    public int faceLocation(MapLocation location) {
        Direction newDir = getDirection(controller.getLocation(), location);
        return faceDirection(newDir);
    }

    /**
     * Returns the MapLocation of the archon closest to this robot.
     */
    public MapLocation findNearestArchon() {
        MapLocation current = controller.getLocation();
        MapLocation[] locations = controller.senseAlliedArchons();

        MapLocation min = null;
        int minDistance = Integer.MAX_VALUE;

        for(MapLocation location : locations) {
            int distance = current.distanceSquaredTo(location);
            if(distance < minDistance && distance >= 1) {
                minDistance = distance;
                min = location;
            }
        }

        return min;
    }

    /**
     * Returns the direction object needed to move a robot from the start square to the end square.
     */
    public Direction getDirection(MapLocation start, MapLocation end) {
        int x = end.getX() - start.getX();
        int y = end.getY() - start.getY();
        return getDirection(x, y);
    }

    public Direction getDirection(int x, int y) {
        if(y < 0) {
            if(x > 0) {
                return Direction.NORTH_EAST;
            }
            if(x == 0) {
                return Direction.NORTH;
            }
            if(x < 0) {
                return Direction.NORTH_WEST;
            }
        } else if(y == 0) {
            if(x > 0) {
                return Direction.EAST;
            }
            if(x < 0) {
                return Direction.WEST;
            }
        } else if(y > 0) {
            if(x > 0) {
                return Direction.SOUTH_EAST;
            }
            if(x == 0) {
                return Direction.SOUTH;
            }
            if(x < 0) {
                return Direction.SOUTH_WEST;
            }
        }
        return null;
    }

    /**
     * Returns the change to the location of an object if it moves one tile in the specified direction.
     */
    public int[] getDirectionDelta(Direction direction) {
        if(direction == Direction.NORTH_WEST) {
            return new int[] {-1, -1};
        }
        if(direction == Direction.NORTH) {
            return new int[] {0, -1};
        }
        if(direction == Direction.NORTH_EAST) {
            return new int[] {1, -1};
        }

        if(direction == Direction.EAST) {
            return new int[] {1, 0};
        }
        if(direction == Direction.WEST) {
            return new int[] {-1, 0};
        }

        if(direction == Direction.SOUTH_WEST) {
            return new int[] {-1, 1};
        }
        if(direction == Direction.SOUTH) {
            return new int[] {0, 1};
        }
        if(direction == Direction.SOUTH_EAST) {
            return new int[] {1, 1};
        }

        return new int[] {0, 0};
    }

    /**
     * Returns the Manhattan Distance
     */
    public int getDistanceTo(MapLocation location) {
        int x = location.getX() - controller.getLocation().getX();
        int y = location.getY() - controller.getLocation().getY();
        return Math.abs(x) + Math.abs(y);
    }

    /**
     * Returns the Manhattan Distance to the nearest archon
     */
    public int getDistanceToNearestArchon() {
        MapLocation location = findNearestArchon();
        int x = location.getX() - controller.getLocation().getX();
        int y = location.getY() - controller.getLocation().getY();
        return Math.abs(x) + Math.abs(y);
    }

    /**
     * Returns the first direction that the robot can move in, starting with the given direction.
     */
    public Direction getMoveableDirection(Direction dir) {
        if(dir == null) {
            return null;
        }
        Direction leftDir = dir, rightDir = dir;
        if(controller.canMove(dir)) {
            return dir;
        } else {
            for(int d = 0; d < 3; d++) {
                leftDir = leftDir.rotateLeft();
                rightDir = rightDir.rotateRight();

                if(controller.canMove(leftDir)) {
                    return leftDir;
                }
                if(controller.canMove(rightDir)) {
                    return rightDir;
                }
            }
        }
        return null;
    }

    /**
     * Returns an array of the 8 map locations around a robot.  These are sorted so
     * that the first location is the one the robot is facing, and then the 2 next to
     * that location, the 2 next to that, and so on.  The last location is the tile directly
     * behind the robot.
     */
    public MapLocation[] getOrderedMapLocations() {
        Direction cur = controller.getDirection(), left, right;
        MapLocation start = controller.getLocation();


        MapLocation[] ret = new MapLocation[8];
        ret[0] = start.add(cur);
        ret[7] = start.subtract(cur);

        left = cur.rotateLeft();
        right = cur.rotateRight();
        ret[1] = start.add(right);
        ret[2] = start.add(left);

        left = cur.rotateLeft();
        right = cur.rotateRight();
        ret[3] = start.add(right);
        ret[4] = start.add(left);

        left = cur.rotateLeft();
        right = cur.rotateRight();
        ret[5] = start.add(right);
        ret[6] = start.add(left);

        return ret;
    }

    public void checkBlockedUnitsAndWait(MapLocation location) {
        boolean messageSent = false;
        int pauseCount = 5;
        do {
            try {
                if(controller.canSenseSquare(location) && controller.senseGroundRobotAtLocation(location) != null) {
                    if(!messageSent) {
                        messaging.sendMove(location);
                        messageSent = true;
                    }
                    controller.yield();
                } else {
                    break;
                }
            } catch(Exception e) {
            }
            pauseCount--;
        } while(pauseCount >= 0);
    }

    /**
     * Returns true if there is no Air or Ground unit at the given location.
     * If a robot is blind (channeler), this method should not be called.  It does
     * not check if the robot can sense at that location.
     */
    public boolean isLocationFree(MapLocation location, boolean isAirUnit) {
        try {
            if(!map.onMap(location)) {
                return false;
            }

            if(!controller.canSenseSquare(location)) {
                return true;
            }

            TerrainTile tile = map.get(location);
            if(tile != null && !tile.isTraversableAtHeight((isAirUnit ? RobotLevel.IN_AIR : RobotLevel.ON_GROUND))) {
                return false;
            }

            if(isAirUnit) {
                return controller.senseAirRobotAtLocation(location) == null;
            } else {
                return controller.senseGroundRobotAtLocation(location) == null;
            }
        } catch(Exception e) {
            pa("----Caught Exception in isLocationFree location: " + location.toString() + " isAirUnit: " +
                    isAirUnit + " Exception: " + e.toString());
            return false;
        }
    }

    /**
     * Makes the robot face a certain direction and then moves forawrd.
     */
    public int moveOnceInDirection(Direction dir) {
        if(faceDirection(dir) != Status.success) {
            return Status.fail;
        }
        return moveOnce();
    }

    /*
     * Moves the robot one step forward if possible.
     */
    public int moveOnce() {
        Direction dir = controller.getDirection();
        yieldMoving();
        try {
            for(int c = 0; c < 2; c++) {
                if(controller.canMove(dir)) {
                    controller.moveForward();
                    controller.yield();
                    player.pathStepTakenCallback();
                    return Status.success;
                }
                controller.yield();
            }
            return Status.cantMoveThere;

        } catch(Exception e) {
            System.out.println("----Caught Exception in moveOnce dir: " + dir.toString() + " Exception: " + e.toString());
        }
        return Status.fail;
    }

    public int moveOnceTowardsLocation(MapLocation location) {
        Direction dir = getDirection(controller.getLocation(), location);
        dir = getMoveableDirection(dir);

        if(dir == null) {
            return Status.fail;
        }
        if(!player.directionCalculatedCallback(dir)) {
            return Status.success;
        }

        return moveOnceInDirection(dir);
    }

    public int moveOnceTowardsArchon() {
        MapLocation archonLocation = findNearestArchon();
        if(isAdjacent(archonLocation, controller.getLocation())) {
            return Status.success;
        }
        return moveOnceTowardsLocation(archonLocation);
    }

    /**
     * Returns true if the two squares are next to each other or are equal.
     */
    public boolean isAdjacent(MapLocation start, MapLocation end) {
        return start.distanceSquaredTo(end) < 3;
    }

    public void yieldMoving() {
        String cur = Goal.toString(player.currentGoal);
        controller.setIndicatorString(1, "yielding");
        while(controller.hasActionSet() || controller.getRoundsUntilMovementIdle() != 0) {
            controller.yield();
        }
        controller.setIndicatorString(1, cur);
    }
}
