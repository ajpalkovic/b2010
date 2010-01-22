package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public abstract class BendoverBugging extends NavigationGoal {
    public boolean[][] terrain;
    public MapLocation goal, start, current;
    public Direction currentDirection, originalDirection;
    public int size, currentX, currentY, maxPath, index;
    public boolean tracing, tracingLeft;
    public MapLocation[] path;
    public RobotController _controller;

    public abstract MapLocation getGoal();
    
    public BendoverBugging(RobotController controller, MapLocation goal, MapStore map) {
        this.goal = goal;
        this._controller = controller;
        this.start = controller.getLocation();
        this.currentDirection = controller.getDirection();
        terrain = map.boolMap;
        size = terrain.length;

        maxPath = 300;
        index = 0;
        path = new MapLocation[maxPath];


        //int turn = Clock.getRoundNum();
        planPath();
        //int doneTurn = Clock.getRoundNum(), turns = (Clock.getRoundNum() - turn);
        int osize = optimizePath();
        //int oturns = Clock.getRoundNum() - doneTurn;
        //System.out.println("Path took: "+turns+".  Optimization took: "+oturns+"  OriginalSize:  "+index+"  OptimizedSize:  "+osize);
    }

    public Direction getDirection() {
        return null;
    }

    public boolean canMove(int x, int y) {
        return !terrain[x%size][y%size];
    }

    public void planPath() {
        Direction dir;
        int pathLength = 1;
        currentX = start.getX();
        currentY = start.getY();
        tracing = false;

        current = start;
        path[index] = current;
        index++;

        while(true) {
            if(pathLength >= maxPath) {
                System.out.println("Path to big");
                break;
            }

            if(current.equals(goal)) {
                //System.out.println("Arrived");
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
    }

    public int optimizePath() {
        MapLocation start, goal;

        int osize = index;
        int waypointIndex = 1;
        start = path[0];
        for(int c = 0; c < index-2; c++) {
            goal = path[waypointIndex+1];

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
            if(!canMove(start.getX(), start.getY())) return false;
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