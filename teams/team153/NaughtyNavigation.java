package team153;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class NaughtyNavigation extends Base {

    public MapStore map;
    public MilkyMessaging messaging;
    public SensationalSensing sensing;

    public NaughtyNavigation(NovaPlayer player) {
        super(player);
        map = player.map;
        messaging = player.messaging;
    }

    /**
     * Returns true if the map coordinate is out of bounds of the map.  Map boundaries
     * are initialized to the min/max integer values so that they this will work even
     * if the boundaries haven't been discovered yet.
     */
    public boolean checkWalls(NovaMapData square) {
        return checkWalls(square.toMapLocation());
    }

    public boolean checkWalls(MapLocation square) {
        return square.getX() <= player.leftWall || square.getX() >= player.rightWall || square.getY() <= player.topWall || square.getY() >= player.bottomWall;
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
        Direction newDir = getDirection(new NovaMapData(controller.getLocation()), new NovaMapData(location));
        if(newDir == null) {
            return Status.fail;
        }
        if(newDir == controller.getDirection()) {
            return Status.success;
        }

        if(controller.hasActionSet()) {
            controller.yield();
        }

        while(controller.getRoundsUntilMovementIdle() != 0) {
            controller.yield();
        }

        try {
            controller.setDirection(newDir);
            controller.yield();
        } catch(Exception e) {
            System.out.println("----Caught exception in faceLocation with MapLocation: " + location.toString() + " Exception: " + e.toString());
            return Status.fail;
        }
        return Status.success;
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
     * Finds a tile path between two locations.
     * If the algorithm cannot find a path of 100 or fewer tiles, it returns null.
     *
     * The resulting path is a LinkedList of map locations.  There is no guarantee
     * that the tiles are on the map if the map borders have not been discovered by
     * this robot yet.  There is no guarantee that a path tile will not be obstructed
     * when the robot gets there.
     *
     * The algorithm performs astar search on the map tiles.  The cost function is the
     * number of turns to get from the start location to the current tile.  The heuristic
     * is the straight line distance number of turns to get to the goal.
     *
     * Rather than considering every possible square a robot could move to, the algorithm
     * only considers 3 new squares each time based on the previous direction of the robot.
     * For instance, if the robot moved up, it will only consider the 3 squares at the top,
     * there is no need to consider if the robot should move backwards.
     */
    public LinkedList<NovaMapData> findPath(NovaMapData start, NovaMapData end) {
        //p(start.toString()+"  "+end.toString());
        LinkedList<PathLocation> states = new LinkedList<PathLocation>();
        ArrayList<PathLocation> newStates = new ArrayList<PathLocation>();
        Hashtable<String, Integer> seenStates = new Hashtable<String, Integer>();
        states.add(new PathLocation(start, null, end));
        PathLocation current = null;

        for(int c = 0; c < 100; c++) {
            newStates.clear();
            //for the current level, process each of the states by calculating the cost and estimate for each of the 8 surrounding squares
            //p("New Iteration: "+states.size());
            //p("States: "+states.toString());
            while(!states.isEmpty()) {
                current = states.removeFirst();
                //p("  Current: "+current.toString3());
                NovaMapData[] squares = map.getForwardSquares(current.location.x, current.location.y, current.xDelta, current.yDelta, current.diagonal);
                //p("  Squares: "+squares[0].toString()+" "+squares[1].toString()+" "+squares[2].toString());
                for(NovaMapData square : squares) {
                    // ensure this step is not out of bounds
                    if(checkWalls(square)) {
                        continue;
                    }

                    MapLocation squarel = square.toMapLocation();
                    try {
                        if(controller.canSenseSquare(squarel)) {
                            if(player.isAirRobot) {
                                if(controller.senseAirRobotAtLocation(squarel) != null) {
                                    continue;
                                }
                            } else {
                                if(controller.senseGroundRobotAtLocation(squarel) != null) {
                                    continue;
                                }
                            }
                        }
                    } catch(Exception e) {
                        System.out.println("----Caught exception in findPath while sense the square. Square: " +
                                squarel.toString() + " Exception: " + e.toString());
                    }

                    // check for the goal state
                    if(square.equals(end)) {
                        end.pathCost = current.cost + player.calculateMovementDelay(true);
                        LinkedList<NovaMapData> ret = new LinkedList<NovaMapData>();
                        ret.addFirst(end);
                        // reverse the path pointed to by current.previous and remove the start location
                        while(current.previous != null) {
                            ret.addFirst(current.location);
                            current = current.previous;
                        }
                        return ret;
                    }

                    PathLocation state = new PathLocation(square, current, end);
                    //p("    New state: "+state.toString2());

                    // check if state has already been marked for consideration.  if it has been, but this is cheaper, then still consider it
                    String s = state.toString();
                    Integer prevCost = seenStates.get(s);
                    if(prevCost != null) {
                        if(prevCost.compareTo(state.intCost) <= 0) {
                            //p("        rejecting square");
                            continue;
                        } else {
                            seenStates.remove(s);
                            newStates.remove(state);
                        }
                    }
                    seenStates.put(s, state.intCost);
                    newStates.add(state);
                }
            }

            //newStates contains up to 32 different squares that we haven't yet considered.  choose the 3 cheapest to continue
            //p(newStates.size()+"  "+newStates.toString());
            for(int count = 0; count < 3 && count < newStates.size(); count++) {
                int minId = count;
                PathLocation min = newStates.get(count);
                for(int d = count + 1; d < newStates.size(); d++) {
                    PathLocation test = newStates.get(d);
                    if(test.total < min.total) {
                        minId = d;
                        min = test;
                    }
                }
                newStates.set(minId, newStates.get(count));
                states.add(min);
            }
        }
        //p("returning null");
        //uhoh, we shouldn't be here WUT ARE WE GONNA DO? (return null)
        return null;
    }

    /**
     * Returns the direction object needed to move a robot from the start square to the end square.
     */
    public Direction getDirection(NovaMapData start, NovaMapData end) {
        int x = end.x - start.x;
        int y = end.y - start.y;
        return getDirection(x, y);
    }

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

    public Direction getMoveableWorkerDirection(Direction dir) {
        pr("in get moveable worker direction");
        if(dir == null) {
            return null;
        }
        boolean messageSent = false;
        int pauseTurns = 5;
        do {
            if(controller.canMove(dir)) {
                return dir;
            }
            if(controller.canMove(dir.rotateLeft())) {
                return dir.rotateLeft();
            }
            if(controller.canMove(dir.rotateRight())) {
                return dir.rotateRight();
            }
            if(controller.canMove(dir.rotateRight().rotateRight())) {
                return dir.rotateRight().rotateRight();
            }
            if(controller.canMove(dir.rotateLeft().rotateLeft())) {
                return dir.rotateLeft().rotateLeft();
            }

            if(!messageSent) {
                pr("sending message");
                messageSent = true;
                messaging.sendMove(controller.getLocation().add(dir));
            }
            pauseTurns--;
            controller.yield();
        } while(pauseTurns >= 0);

        pr("returning getMoveableDirection");
        return getMoveableDirection(dir);
    }

    /**
     * Returns an array of the 8 map locations around a robot.  These are sorted so
     * that the first location is the one the robot is facing, and then the 2 next to
     * that location, the 2 next to that, and so on.  The last location is the tile directly
     * behind the robot.
     */
    public NovaMapData[] getOrderedMapLocations() {
        Direction cur = controller.getDirection(), left, right;
        MapLocation start = controller.getLocation();


        NovaMapData[] ret = new NovaMapData[8];
        ret[0] = map.getNotNull(start.add(cur));
        ret[7] = map.getNotNull(start.subtract(cur));

        for(int c = 1; c < 7; c++) {
            left = cur.rotateLeft();
            right = cur.rotateRight();
            ret[c] = map.getNotNull(start.add(right));
            c++;
            ret[c] = map.getNotNull(start.add(left));
        }

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

    public int goByBugging(NovaMapData end) {
        NovaMapData previous = null;
        Direction previousDir = null, dir = controller.getDirection();
        int count = 0;
        int oppositeCount = 0;
        controller.setIndicatorString(2, "Goal: " + end.toMapLocation().toString());
        for(int c = 0; c < 100; c++) {
            controller.setIndicatorString(0, "Loc: " + controller.getLocation().toString());
            previous = map.getNotNull(controller.getLocation());
            if(controller.getLocation().equals(end.toMapLocation())) {
                return Status.success;
            }

            yieldMoving();
            faceLocation(end.toMapLocation());

            if(controller.getLocation().isAdjacentTo(end.toMapLocation())) {
                if(!controller.canMove(getDirection(controller.getLocation(), end.toMapLocation()))) {
                    try {
                        if(controller.canSenseSquare(end.toMapLocation()) && (controller.senseGroundRobotAtLocation(end.toMapLocation()) != null)) {
                            checkBlockedUnitsAndWait(end.toMapLocation());
                        } else {
                            // no one is blocking us but we can't go there, let's fail
                            return Status.fail;
                        }
                    } catch(Exception e) {
                    }
                }
            }

            previous = map.getNotNull(controller.getLocation());
            dir = getMoveableDirection(getDirection(previous, end));

            if(dir == null) {
                System.out.println("null direction");
                controller.yield();
                continue;
            }

            Direction opposite = dir.opposite();
            if(opposite.equals(previousDir) || opposite.rotateLeft().equals(previousDir) || opposite.rotateRight().equals(previousDir)) {
                pr("not gonna go that way");
                oppositeCount++;
                if(oppositeCount > 5) {
                    return Status.fail;
                }
                controller.yield();
                continue;
            }
            oppositeCount = 0;

            if(faceDirection(dir) != Status.success) {
                pr("couldnt face that way");
                controller.yield();
                continue;
            }

            previousDir = dir;

            yieldMoving();

            try {
                boolean good = false;
                for(int d = 0; d < 2; d++) {
                    if(controller.canMove(dir)) {
                        if(!player.beforeMovementCallback(map.getNotNull(controller.getLocation().add(dir)))) {
                            return Status.success;
                        }
                        controller.moveForward();
                        controller.yield();
                        if(!player.pathStepTakenCallback()) {
                            return Status.success;
                        }
                        good = true;
                        count = 0;
                        break;
                    }
                    pr("yielding");
                    controller.yield();
                }
                if(!good) {
                    count++;
                    if(count >= 3) {
                        return Status.cantMoveThere;
                    }
                }

            } catch(Exception e) {
                System.out.println("----Caught Exception in go dir: " + dir.toString() + " Exception: " + e.toString());
            }
        }
        return Status.fail;
    }

    /**
     * Makes the robot move from its current location to end.
     * The method first calculates a path to the location.  It then calls the pathCalculateCallback,
     * which can be overriden in the base class.  If the callback returns false, the method halts.
     *
     * The method then proceeds to traverse the entire path.  If it encounters a simple obstacle,
     * such as one unit in the way, it will attempt to go around.  More complex obstacles will
     * cause it to completely retry the path.
     *
     * Each time the robot moves, it calls the pathStepTakenCallback, which can be
     * overriden in the base class.  If the callback returns false, the method returns.
     *
     * TODO: validate we are at the goal
     * TODO: repair path
     */
    public int go(NovaMapData end) {
        return go(end, 0);
    }

    public int go(NovaMapData end, int depth) {
        if(depth > 5) {
            return Status.fail;
        }

        NovaMapData previous = map.getNotNull(controller.getLocation());
        //p("get path");
        LinkedList<NovaMapData> path = findPath(previous, end);
        //p("got path");
        if(path == null) {
            return Status.fail;
        }

        if(!player.pathCalculatedCallback(path)) {
            return Status.success;
        }

        //System.out.println(controller.getRobot().getID()+": "+previous.toString()+"   "+end.toString()+"   "+path.toString());
        Direction dir = controller.getDirection();
        while(!path.isEmpty()) {
            yieldMoving();

            NovaMapData step = path.removeFirst();
            if(controller.canSenseSquare(step.toMapLocation())) {
                NovaMapData updatedStep = sensing.senseTile(step.toMapLocation());
                step = updatedStep;

                if((player.isAirRobot && step.airRobot != null) || (!player.isAirRobot && step.groundRobot != null)) {
                    // the path is blocked
                    if(path.isEmpty()) {
                        // the goal is blocked
                        return Status.goalBlocked;
                    }
                    return go(end, depth + 1);
                }
            }
            //find if we need to change direction
            Direction newDirection = getDirection(previous, step);
            faceDirection(newDirection);

            //check if the square is free
            if(!controller.canMove(dir)) {
                //uhoh, we can't move here, prolly cuz a robot is in the way, time to plot a path around?
                //check if the square right next to the obstacle is free
                //just plot a path around and recursively call go?
                return go(end, depth + 1);
            } else {
                try {
                    if(!player.beforeMovementCallback(step)) {
                        return Status.success;
                    }

                    if(controller.hasActionSet()) {
                        controller.yield();
                    }

                    controller.moveForward();
                    controller.yield();
                } catch(Exception e) {
                    System.out.println("----Caught exception in go while moving forward. Exception: " + e.toString());
                    return Status.fail;
                }
                if(!player.pathStepTakenCallback()) {
                    return Status.success;
                }

            }
        }

        return Status.success;
    }

    /**
     * Returns true if there is no Air or Ground unit at the given location.
     * If a robot is blind (channeler), this method should not be called.  It does
     * not check if the robot can sense at that location.
     */
    public boolean isLocationFree(MapLocation location, boolean isAirUnit) {
        try {
            if(checkWalls(location)) {
                return false;
            }

            if(!controller.senseTerrainTile(location).isTraversableAtHeight((isAirUnit ? RobotLevel.IN_AIR : RobotLevel.ON_GROUND))) {
                return false;
            }

            if(isAirUnit) {
                return controller.senseAirRobotAtLocation(location) == null;
            } else {
                return controller.senseGroundRobotAtLocation(location) == null;
            }
        } catch(Exception e) {
            System.out.println("----Caught Exception in isLocationFree location: " + location.toString() + " isAirUnit: " +
                    isAirUnit + " Exception: " + e.toString());
            return false;
        }
    }

    /**
     * Moves the robot one step forward if possible.
     */
    public int moveOnce(Direction dir) {
        if(faceDirection(dir) != Status.success) {
            return Status.fail;
        }

        yieldMoving();

        try {
            for(int c = 0; c < 2; c++) {
                if(controller.canMove(dir)) {
                    player.beforeMovementCallback(map.get(controller.getLocation().add(dir)));
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

        return moveOnce(dir);
    }

    public void yieldMoving() {
        String cur = Goal.toString(player.currentGoal);
        controller.setIndicatorString(1, "yielding");
        while(controller.hasActionSet() || controller.getRoundsUntilMovementIdle() != 0) {
            controller.yield();
        }
        controller.setIndicatorString(1, cur);
    }

    class PathLocation {

        public NovaMapData location;
        public PathLocation previous;
        public int cost, estimate, total, xDelta, yDelta;
        public boolean diagonal;
        public Integer intCost;

        public PathLocation(NovaMapData location, PathLocation previous, NovaMapData goal) {
            this.location = location;
            this.previous = previous;
            if(previous != null) {
                //non diagonal squares will have an x or y coordinate which is the same in both the current location and the next one
                diagonal = !(location.x == previous.location.x || location.y == previous.location.y);
                xDelta = location.x - previous.location.x;
                yDelta = location.y - previous.location.y;
                cost = player.calculateMovementDelay(diagonal) + previous.cost;
                //if we have to change direction
                if(xDelta != previous.xDelta || yDelta != previous.yDelta) {
                    cost++;
                }
            } else {
                // the first state has no previous data
                diagonal = false;
                cost = 0;
                xDelta = -2;
                yDelta = -2;
            }
            estimate = calculateEstimate(goal);
            total = cost + estimate;
            intCost = new Integer(total);
        }

        /**
         * Heuristic function for astar search.
         *
         * Currently, this calculates the straight line distance to the goal and returns the number of turns to traverse that number of squares.
         * TODO: Consider the difference in height between the current location and the goal.
         */
        public int calculateEstimate(NovaMapData goal) {
            int x = Math.abs(goal.x - location.x), y = Math.abs(goal.y - location.y);
            int diagonalSquares = (int) Math.sqrt(x * x + y * y);
            return diagonalSquares * player.moveDiagonalDelay;
        }

        /**
         * Returns true if the two objects have the same location only.
         */
        public boolean equals(Object o) {
            PathLocation other = (PathLocation) o;
            return other.location.equals(location);
        }

        public String toString() {
            return location.x + " " + location.y;
        }
    }
}
