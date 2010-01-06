package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class WorkerPlayer extends NovaPlayer {

    public int[] verticalDeltas = new int[] {-4, 0, -3, 2, -2, 3, -1, 3, 0, 4, 1, 3, 2, 3, 3, 2, 4, 0};
    public int[] horizontalDeltas = new int[] {0, -4, 2, -3, 3, -2, 3, -1, 4, 0, 3, 1, 3, 2, 2, 3, 0, 4};
    public int[] diagonalDeltas = new int[] {3, -2, 3, 0, 4, 0, 3, 1, 3, 2, 2, 2, 2, 3, 1, 3, 0, 3, 0, 4};
    public int exploreDistance = 5;
    public int status;
    ArrayList<MapData> steps;
    Direction stepDirection, searchDirection = Direction.EAST;
    MapData fluxLocation, blockLocation, lastBlockLocation = null, currentStepLocation, goalLocation;
    ArrayList<MapData> badBlocks = new ArrayList<MapData>();

    public WorkerPlayer(RobotController controller) {
        super(controller);
        steps = new ArrayList<MapData>();
    }

    public void run() {
        team = controller.getTeam();
        sensing.senseAllTiles();
        messaging.sendNewUnit();
        setGoal(Goal.idle);

        while(true) {
            int startTurn = Clock.getRoundNum();
            energon.autoTransferEnergonBetweenUnits();

            messaging.parseMessages();

            switch(currentGoal) {
                case Goal.findBlock:
                    findBlockGoal();
                    break;
                case Goal.foundBlock:
                    foundBlockGoal();
                    break;
                case Goal.goingToSteps:
                    goingToStepsGoal();
                    break;
            }

            if(startTurn == Clock.getRoundNum() || controller.hasActionSet()) {
                controller.yield();
            }
        }
    }

    public void findBlockGoal() {
        blockLocation = findBlock();
        if(blockLocation != null) {
            if(getNumBlocksInCargo() == 0) {
                setGoal(Goal.foundBlock);
                //p("setting found block goal");
            }
        } else {
            searchForBlocks();
        }
    }

    public int getNumBlocksInCargo() {
        try {
            return controller.senseNumBlocksInCargo(controller.getRobot());
        } catch(Exception e) {
            System.out.println("caught exception in findBlockGoal " + e.toString());
        }
        return 0;
    }

    public void foundBlockGoal() {
        if(getNumBlocksInCargo() > 0) {
            setGoal(Goal.goingToSteps);
            return;
        }

        gotoBlock();

        try {
            if(controller.canLoadBlockFromLocation(blockLocation.toMapLocation())) {
                controller.loadBlockFromLocation(blockLocation.toMapLocation());
                lastBlockLocation = blockLocation;
                //p("loaded block");
                setGoal(Goal.goingToSteps);

            } else {
                badBlocks.add(blockLocation);
                //p("coudl not load block");
                setGoal(Goal.findBlock);
            }
        } catch(Exception e) {
            System.out.println("Caught exception in foundBlockGoal " + e.toString());
            setGoal(Goal.goingToSteps);
        }
    }

    public void gotoBlock() {
        badBlocks.clear();

        Direction dir = blockLocation.toMapLocation().directionTo(controller.getLocation());
        dir = dir.isDiagonal() ? dir.rotateRight() : dir;
        MapLocation goal = blockLocation.toMapLocation().add(dir);

        navigation.goByBugging(map.getOrCreate(goal));
        status = navigation.goByBugging(map.get(goal));
        navigation.faceLocation(blockLocation.toMapLocation());

        navigation.yieldMoving();
        checkBlockingUnit(blockLocation.location);
    }

    public void checkBlockingUnit(MapLocation location) {
        try {
            if(controller.senseGroundRobotAtLocation(location) != null) {
                messaging.sendMove(location);
            }
        } catch(Exception e) {
            System.out.println("Caught Exception in checkBlockingUnit: " + e.toString());
        }
    }

    public void gotoGoal() {
        Direction dir = navigation.getDirection(controller.getLocation(), fluxLocation.toMapLocation());
        if(dir != null) {
            MapData goal = map.getOrCreate(fluxLocation.toMapLocation().subtract(dir));
            navigation.goByBugging(goal);
        }

        int currentStep = 0;
        boolean keepGoing = true;
        do {
            //p("getting info");
            int[] goalInformation = findGoalLocation();
            if(goalInformation == null) {
                return;
            }
            currentStep = goalInformation[1];

            goalLocation = steps.get(goalInformation[1]);
            /*p("got some stuff");
            p(steps.get(goalInformation[0]).toString());
            p(steps.get(goalInformation[1]).toString());

            controller.setIndicatorString(0, controller.getLocation().toString());
            controller.setIndicatorString(1, "going to step base");
            controller.setIndicatorString(2, goalLocation.toString());

            p("going to goal"); */
            status = navigation.goByBugging(goalLocation);
            status = navigation.goByBugging(goalLocation);
            //p("status"+status);

            if(status == Status.fail) {
                //p("----gonna retry");
            } else {
                keepGoing = false;
            }

            goalLocation = steps.get(goalInformation[0]);
        } while(keepGoing);

        // now i am at the base of the steps
        while(true) {
            //p("Goal Location: "+goalLocation.toStringFull());
            //p("My Location: "+controller.getLocation());
            if(controller.getLocation().isAdjacentTo(goalLocation.toMapLocation()) &&
                    goalLocation.height - map.get(controller.getLocation()).height <= 1) {
                navigation.faceLocation(goalLocation.toMapLocation());
                break;
            }
            if(currentStep <= 1) {
                break;
            }

            /* controller.setIndicatorString(0, controller.getLocation().toString());
            controller.setIndicatorString(1, "going to dropoff point");
            controller.setIndicatorString(2, goalLocation.toString());*/

            currentStep--;
            MapLocation location = steps.get(currentStep).toMapLocation();
            for(int c = 0; c < 3; c++) {
                navigation.faceLocation(location);
                navigation.yieldMoving();
                if(!controller.canMove(navigation.getDirection(controller.getLocation(), location))) {
                    navigation.checkBlockedUnitsAndWait(location);
                }
                navigation.moveOnceTowardsLocation(location);
                navigation.yieldMoving();
                if(controller.getLocation().equals(location)) {
                    break;
                }
            }
        }
    }

    public void goingToStepsGoal() {
        gotoGoal();
        try {
            navigation.yieldMoving();

            if(controller.canUnloadBlockToLocation(goalLocation.toMapLocation())) {
                //p("trying to unload");
                controller.unloadBlockToLocation(goalLocation.toMapLocation());
                setGoal(Goal.findBlock);
            } else if(controller.getLocation().isAdjacentTo(goalLocation.toMapLocation())) {
                checkBlockingUnit(goalLocation.location);
                //p("adjacent to steps");
                // I'm next to the location, but I can't drop it there!
                if(controller.getLocation().equals(goalLocation.toMapLocation())) {
                    //p("At location, but cannot drop");
                }
            } else {
                //p("checking blocking units");
                checkBlockingUnit(goalLocation.location);
                //checkBlockingUnit(currentStepLocation.location);
            }
            if(status == Status.fail) {
                //p("failed");
            }

        } catch(Exception e) {
            //could not load block onto location
            //System.out.println("reconfigure!");
        }
    }

    public void senseNewTiles() {
        sensing.senseDeltas(verticalDeltas, horizontalDeltas, diagonalDeltas);
    }

    /**
     * overridden from NovaPlayer, called when find blocks broadcast is received
     */
    public void findBlocks(MapLocation fluxLocation, Direction d) {
        stepDirection = searchDirection = d;
        this.fluxLocation = map.getOrCreate(fluxLocation);
        this.currentStepLocation = this.fluxLocation;
        createSteps();
        this.goalLocation = map.getOrCreate(fluxLocation.add(stepDirection));
        exploreDistance = 5;
        if(currentGoal == Goal.idle) {
            setGoal(Goal.findBlock);
        }
    }

    /**
     * 
     * returns a block in site
     */
    public MapData findBlock() {
        MapLocation[] nearbyBlocks = controller.senseNearbyBlocks();
        ArrayList<MapLocation> blocks = sortedByDistance(nearbyBlocks);
        stepsLoop:
        for(MapLocation block : blocks) {
            for(MapData step : steps) {
                if(step.toMapLocation().equals(block)) {
                    continue stepsLoop;
                }
            }
            for(MapData step : badBlocks) {
                if(step.toMapLocation().equals(block)) {
                    continue stepsLoop;
                }
            }

            int blockLocationHeight = map.getNotNull(block).height;
            int currentHeight = map.getOrCreate(controller.getLocation()).height;
            if(currentHeight - blockLocationHeight > 1 || blockLocationHeight - currentHeight > 1) {
                continue stepsLoop;
            }

            return map.getOrCreate(block);
        }
        return null;
    }

    public ArrayList<MapLocation> sortedByDistance(MapLocation[] locations) {
        ArrayList<MapLocation> retList = new ArrayList<MapLocation>();
        ArrayList<MapLocation> spots = new ArrayList<MapLocation>();
        spots.addAll(Arrays.asList(locations));
        int smallest, currDist;
        MapLocation small = null;

        MapLocation currLocation = controller.getLocation();
        while(!spots.isEmpty()) {
            smallest = Integer.MAX_VALUE;
            for(MapLocation m : spots) {
                if((currDist = currLocation.distanceSquaredTo(m)) < smallest) {
                    small = m;
                    smallest = currDist;
                }
            }
            retList.add(small);
            spots.remove(small);

        }
        return retList;
    }

    public int[] findGoalLocation() {
        senseSteps();
        sensing.senseTile(controller.getLocation());
        /*if(fluxLocation.blockHeight < 2)
        return fluxLocation;*/

        int step = getFirstForBlock(0);
        int stepClimbStart = isAccessible(step);

        if(stepClimbStart > -1 && step > -1) {
            return new int[] {step, stepClimbStart};
        }
        //return new MapData[] {steps.get(step), steps.get(stepClimbStart)};
        return null;
    }

    public int isAccessible(int currentStep) {
        if(currentStep >= 8) {
            return -1;
        }

        MapData current = steps.get(currentStep);
        MapData previous = steps.get(currentStep + 1);

        int myHeight = map.get(controller.getLocation()).height;
        //p("isAccessible currentStep: "+currentStep+"  me: "+myHeight+"  current: "+current.toStringFull()+"  previous: "+previous.toStringFull());

        if(current.height - myHeight <= 2) {
            if(currentStep < 8) {
                return currentStep + 1;
            } else {
                return currentStep;
            }
        }

        if(current.height - previous.height <= 2) {
            return isAccessible(currentStep + 1);
        } else {
            return -1;
        }
    }

    public int getFirstForBlock(int currentStep) {
        if(currentStep >= 8) {
            return currentStep;
        }

        MapData current = steps.get(currentStep);
        MapData previous = steps.get(currentStep + 1);

        //p("getFirstForBlock currentStep: "+currentStep+"  current: "+current.toStringFull()+"  previous: "+previous.toStringFull());
        // i can place a block on current from previous
        if(current.height - previous.height <= 1) {
            if(isAccessible(currentStep) > -1) {
                return currentStep;
            } else {
                return getFirstForBlock(currentStep + 1);
            }
        } else {
            return getFirstForBlock(currentStep + 1);
        }
    }

    public void searchForBlocks() {
        //need to track movement to make sure explore a big map area
        if(lastBlockLocation != null && !controller.getLocation().equals(blockLocation) && !controller.getLocation().isAdjacentTo(lastBlockLocation.toMapLocation())) {
            searchDirection = controller.getLocation().directionTo(lastBlockLocation.toMapLocation());
        }

        checkBlockingUnit(controller.getLocation().add(searchDirection));

        if(controller.canMove(searchDirection)) {
            status = navigation.moveOnce(searchDirection);
            if(status != Status.success || controller.getLocation().distanceSquaredTo(fluxLocation.toMapLocation()) > exploreDistance) {
                searchDirection = searchDirection.rotateRight();
                if(searchDirection.equals(stepDirection)) {
                    exploreDistance *= 3;
                }
            }
        } else {
            searchDirection = searchDirection.rotateRight();
            if(searchDirection.equals(stepDirection)) {
                exploreDistance *= 3;
            }
        }
    }

    public void senseSteps() {
        for(MapData m : steps) {
            try {
                if(controller.canSenseSquare(m.toMapLocation())) {
                    m.height = controller.senseHeightOfLocation(m.toMapLocation());
                }
            } catch(Exception e) {
                System.out.println("Caught Exception in senseSteps: " + e.toString());
            }
        }
    }

    public void createSteps() {
        steps.clear();
        steps.add(fluxLocation);

        // add the left one
        MapData tmp = map.getOrCreate(fluxLocation.toMapLocation().add(stepDirection));
        steps.add(tmp);

        // go up one
        Direction dir = stepDirection.rotateRight().rotateRight();
        tmp = map.getOrCreate(tmp.toMapLocation().add(dir));
        steps.add(tmp);

        //go right two, then down two, then left two
        for(int c = 0; c < 3; c++) {
            dir = dir.rotateRight().rotateRight();
            tmp = map.getOrCreate(tmp.toMapLocation().add(dir));
            steps.add(tmp);
            tmp = map.getOrCreate(tmp.toMapLocation().add(dir));
            steps.add(tmp);
        }
        //System.out.println(steps.toString());
    }
}
