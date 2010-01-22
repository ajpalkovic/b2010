package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public abstract class BendoverBugging extends NavigationGoal {

    public boolean[][] terrain;
    public MapLocation goal, current;
    public Direction currentDirection, originalDirection;
    public int size, currentX, currentY, maxPath, index, currentPathIndex;
    public boolean tracing, tracingLeft, pathCalculated, bugging;
    public MapLocation[] path;
    public RobotController _controller;

    public abstract MapLocation getGoal();

    public BendoverBugging(RobotController controller, MapStore map) {
        this._controller = controller;
        terrain = map.boolMap;
        size = terrain.length;

        maxPath = 50;
        bugging = pathCalculated = tracing = tracingLeft = false;
        path = new MapLocation[maxPath];
    }

    public Direction getDirection() {
        MapLocation newGoal = getGoal();
        if(newGoal == null) {
            return null;
        }

        if(!bugging) {
            Direction dir = _controller.getLocation().directionTo(newGoal);
            if(_controller.canMove(dir)) {
                return dir;
            }
        }

        bugging = true;

        if(pathCalculated) {
            if(!goal.equals(newGoal) && canGo(goal, newGoal)) {
                goal = newGoal;
                path[index - 1] = newGoal;
                return getNextPathDirection();
            } else {
                planPath();
                return getNextPathDirection();
            }
        } else {
            planPath();
            return getNextPathDirection();
        }
    }

    public Direction getNextPathDirection() {
        MapLocation goal = null, start = _controller.getLocation();
        while(true) {
            while(currentPathIndex < index && path[currentPathIndex] == null) currentPathIndex++;
            goal = path[currentPathIndex];
            if(goal == null) return null;
            
            if(goal.equals(start)) {
                currentPathIndex++;
            } else {
                break;
            }
        }

        return getMoveableDirection(start.directionTo(goal));

    }

    /**
     * Returns the first direction that the robot can move in, starting with the given direction.
     */
    public Direction getMoveableDirection(Direction dir) {
        if(dir == null) {
            return null;
        }
        Direction leftDir = dir, rightDir = dir;
        if(_controller.canMove(dir)) {
            return dir;
        } else {
            for(int d = 0; d < 3; d++) {
                leftDir = leftDir.rotateLeft();
                rightDir = rightDir.rotateRight();

                if(_controller.canMove(leftDir)) {
                    return leftDir;
                }
                if(_controller.canMove(rightDir)) {
                    return rightDir;
                }
            }
        }
        return null;
    }

    public boolean canMove(int x, int y) {
        return !terrain[x % size][y % size];
    }

    public void planPath() {
        goal = getGoal();
        currentDirection = _controller.getDirection();
        pathCalculated = true;

        Direction dir;
        tracing = false;
        index = 1;
        currentPathIndex = 0;
        int pathLength = 1;

        path[0] = current = _controller.getLocation();
        currentX = current.getX();
        currentY = current.getY();

        while(true) {
            if(current.equals(goal)) {
                //System.out.println("Arrived");
                break;
            }

            if(pathLength >= maxPath) {
                System.out.println("Path to big");
                break;
            }

            dir = getNextDirection();

            current = current.add(dir);
            if(dir != currentDirection) {
                path[index] = current;
                index++;
            }
            currentX += dir.dx;
            currentY += dir.dy;
            currentDirection = dir;

            pathLength++;
        }

        optimizePath();
    }

    public int optimizePath() {
        MapLocation start, goal;

        int osize = index;
        int waypointIndex = 1;
        start = path[0];
        for(int c = 0; c < index - 2; c++) {
            goal = path[waypointIndex + 1];

            //try to go straight from start to goal
            if(canGo(start, goal)) {
                path[waypointIndex] = null;
                osize--;
            } else {
                start = path[waypointIndex];
            }
            waypointIndex++;
        }
        return osize;
    }

    public boolean canGo(MapLocation start, MapLocation goal) {
        while(!start.equals(goal)) {
            start = start.add(start.directionTo(goal));
            if(!canMove(start.getX(), start.getY())) {
                return false;
            }
        }
        return true;
    }

    public Direction getDirectionToGoal() {
        return current.directionTo(goal);
    }

    public Direction getNextDirection() {
        Direction dir = getDirectionToGoal();
        int x, y;
        if(tracing) {
            dir = tryToUndoTrace(currentDirection);
            x = currentX + dir.dx;
            y = currentY + dir.dy;
            if(!canMove(x, y)) {
                dir = getInitialTracingDirection(dir);
            }
            return dir;
        } else {
            x = currentX + dir.dx;
            y = currentY + dir.dy;
            if(canMove(x, y)) {
                return dir;
            } else {
                tracing = true;
                originalDirection = dir;
                tracingLeft = !(dir == Direction.NORTH || dir == Direction.NORTH_EAST || dir == Direction.EAST || dir == Direction.SOUTH_WEST);
                dir = getInitialTracingDirection(dir);
                return dir;
            }
        }
    }

    public Direction getInitialTracingDirection(Direction dir) {
        int x, y;
        for(int c = 0; c < 8; c++) {
            x = currentX + dir.dx;
            y = currentY + dir.dy;
            if(!canMove(x, y)) {
                dir = tracingLeft ? dir.rotateLeft() : dir.rotateRight();
            } else {
                return dir;
            }
        }

        return null;
    }

    public Direction tryToUndoTrace(Direction dir) {
        Direction tmp;
        int x, y;

        for(int c = 0; c < 8; c++) {
            tmp = tracingLeft ? dir.rotateRight() : dir.rotateLeft();
            x = currentX + tmp.dx;
            y = currentY + tmp.dy;

            if(canMove(x, y)) {
                if(tmp == originalDirection) {
                    tracing = false;
                    return tmp;
                }
                dir = tmp;
            } else {
                return dir;
            }
        }

        return dir;
    }
}
