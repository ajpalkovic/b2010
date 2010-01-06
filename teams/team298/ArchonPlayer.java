package team298;

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
    public Party scoutParty = new Party(3);
    public MapData spottedFlux = null;
    public Direction exploreDirection;
    public SporadicSpawning spawning;

    public ArchonPlayer(RobotController controller) {
        super(controller);
        spawning = new SporadicSpawning(this);
    }

    public void run() {
        boot();
        while(true) {
            int startTurn = Clock.getRoundNum();
            messaging.parseMessages();
            energon.processEnergonTransferRequests();

            // reevaluate goal here?
            switch(currentGoal) {
                case Goal.followingArchon:
                    messaging.sendMessageForEnemyRobots();
                    for(Robot r : controller.senseNearbyAirRobots()) {
                        if(r.getID() == followingArchonNumber) {
                            try {
                                MapData loc = map.getNotNull(controller.senseRobotInfo(r).location);
                                navigation.goByBugging(loc);


                            } catch(Exception e) {
                                p("----------------cannot sense robot info in following archon");
                            }
                        }
                    }

                    break;
                case Goal.goingTowardsFlux:
                    goTowardsFlux();
                    break;
                case Goal.exploringForFlux:
                    explore();
                    break;
                case Goal.goingDirectlyToFlux:
                    navigation.goByBugging(spottedFlux);
                    if(!updateFluxGoal(controller.getLocation())) {
                        spawnParty();
                        setGoal(Goal.scouting);
                    }
                    break;
                case Goal.collectingFlux:
                    spottedFlux = null;
                    messaging.sendMessageForEnemyRobots();
                    spawning.spawnFluxUnits();
                    break;
                case Goal.gettingCloseToFlux:
                    getCloseToFlux();
                    break;
                case Goal.supporttingFluxDeposit:
                    messaging.sendMessageForEnemyRobots();
                    spawning.spawnFluxUnits();
                    break;
                case Goal.idle:
                    // reevaluate the goal
                    ;
                    break;
                case Goal.scouting:
                    //
                    messaging.sendMessageForEnemyRobots();
                    trackingCount++;
                    trackingCount %= 10;
                    scoutParty.partyUp();
                    if(trackingCount == 0) {
                        explore();
                    }
                    scout();

                    break;
                case Goal.fight:
                    spawnParty();
                    break;
            }

            if(startTurn == Clock.getRoundNum() || controller.hasActionSet()) {
                controller.yield();
            }
        }
    }

    public void scout() {
        navigation.moveOnce(exploreDirection);
    }

    public void goTowardsFlux() {
        Direction dir = controller.senseDirectionToUnownedFluxDeposit();
        if(!controller.canMove(dir)) {
            Direction leftTemp = dir, rightTemp = dir;
            for(int c = 0; c < 4; c++) {
                leftTemp = leftTemp.rotateLeft();
                if(controller.canMove(leftTemp)) {
                    dir = leftTemp;
                    break;
                }

                rightTemp = rightTemp.rotateRight();
                if(controller.canMove(rightTemp)) {
                    dir = rightTemp;
                    break;
                }
            }
        }
        navigation.moveOnce(dir);
    }

    public void boot() {
        team = controller.getTeam();
        senseArchonNumber();
        sensing.senseAllTiles();
        if(archonNumber < 3) {
            setGoal(Goal.goingTowardsFlux);
            archonGroup = 1;
            sensing.senseAllTiles();

        } else if(archonNumber < 5) {
            setGoal(Goal.exploringForFlux);
            archonGroup = 2;
            exploreDirection = controller.senseDirectionToUnownedFluxDeposit().rotateLeft();
        } else {
            setGoal(Goal.exploringForFlux);
            archonGroup = 2;
            exploreDirection = controller.senseDirectionToUnownedFluxDeposit().rotateRight();
        }
        if(archonNumber % 2 == 0) {
            //message to other archon
        }

    }

    public void enemyInSight() {
        //spawnParty();
    }

    public void spawnParty() {
        int turnsToWait = new Random().nextInt(8) + Clock.getRoundNum() + 1;
        while(Clock.getRoundNum() < turnsToWait) {
            messaging.parseMessages();
            energon.processEnergonTransferRequests();
            controller.yield();
        }

        if(spawning.canSupportUnit(RobotType.CANNON) && scoutParty.partySize < 2) {
            scoutParty.waitForNewUnit(RobotType.CANNON);
            spawning.spawnRobot(RobotType.CANNON);
        } else if(controller.getEnergonLevel() / controller.getMaxEnergonLevel() > .5) {
            ArrayList<MapLocation> enemies = sensing.senseEnemyRobotLocations();
            if(enemies.size() <= 3) {
                goDirection(controller.getLocation().directionTo(enemies.get(0)));
            }
            if(enemies.size() == 0) {
                setGoal(Goal.scouting);
                explore();
            }
        } else {
            ArrayList<MapLocation> enemies = sensing.senseEnemyRobotLocations();
            if(enemies.size() > 0) {
                goDirection(controller.getLocation().directionTo(enemies.get(0)).opposite());
            }

        }
    }

    public void newUnit(int robotID, MapLocation location, String robotType) {

        if(scoutParty.isWaitingForNewRobot() && scoutParty.expectedRobotType.toString().equals(robotType)) {
            ArrayList<Robot> nearbyRobots = new ArrayList<Robot>();
            nearbyRobots.addAll(Arrays.asList(controller.senseNearbyAirRobots()));
            nearbyRobots.addAll(Arrays.asList(controller.senseNearbyGroundRobots()));
            for(Robot r : nearbyRobots) {
                if(r.getID() == robotID) {
                    //this guy is near us, and he is probably the one we were waiting for in our party
                    try {
                        scoutParty.addPartyMember(r, controller.senseRobotInfo(r));
                    } catch(Exception e) {
                        p("------------Cannot Sense Robot Info in newUnit in ArchonPlayer");
                    }
                }
            }
        }
        if(RobotType.valueOf(robotType).equals(RobotType.WORKER) && (currentGoal == Goal.supporttingFluxDeposit || currentGoal == Goal.collectingFlux)) {
            messaging.sendFindBlocks(currentGoal == Goal.supporttingFluxDeposit ? spottedFlux.location : controller.getLocation(), findStepDirection(), robotID);
        }

    }

    public Direction findStepDirection() {
        int[] numLocations = {0, 0, 0, 0};
        int[] avgChangeHeight = {0, 0, 0, 0};
        Direction retDirection;
        MapLocation fluxDeposit = ((currentGoal == Goal.supporttingFluxDeposit) ? spottedFlux.location : controller.getLocation());

        retDirection = Direction.NORTH;
        MapData checkLoc;
        int prevHeight, currHeight;
        for(int j = 0; j < 4; j++) {
            checkLoc = map.getNotNull(fluxDeposit);
            currHeight = checkLoc.height;
            for(int i = 0; i <= 6; i++) {
                prevHeight = currHeight;
                checkLoc = map.getNotNull(checkLoc.toMapLocation().add(retDirection));
                currHeight = checkLoc.height;
                prevHeight -= currHeight;
                if(prevHeight < -2 || prevHeight > 2) {
                    break;
                }
                if(checkLoc.tile != null && checkLoc.tile.getType() == TerrainType.LAND) {
                    numLocations[j]++;
                } else {
                    break;
                }
            }
            retDirection = retDirection.rotateRight().rotateRight();

        }
        int maxIndex = -1, maxNum = -1, currNum;

        for(int i = 0; i < 4; i++) {
            if((currNum = numLocations[i]) > maxNum) {
                maxIndex = i;
                maxNum = currNum;
            }
        }

        retDirection = Direction.NORTH;
        for(int i = 0; i < maxIndex; i++) {
            retDirection = retDirection.rotateRight().rotateRight();
        }

        return retDirection;
    }

    public boolean updateFluxGoal(MapLocation location) {
        MapData updatedData = sensing.senseTile(spottedFlux.toMapLocation());

        if(updatedData.toMapLocation().equals(controller.getLocation())) {
            controller.setIndicatorString(1, "collecting flux");
            scoutParty.setPartyGoal(Goal.supporttingFluxDeposit);
            setGoal(Goal.collectingFlux);
            return true;
        }

        if(updatedData.airRobot == null) {
            return true;
        }

        if(updatedData.airRobotInfo.team != team) {
            //uhoh, its the enemy
            setGoal(Goal.scouting);
            return false;
        } else {
            // a robot is there already, let's help them out
            // get to within 2 tiles of it first though
            setGoal(Goal.scouting);
            return true;
        }
        //return true;
    }

    public void getCloseToFlux() {

        if(true) {
            return;
        }

        while(true) {
            MapData updatedData = sensing.senseTile(spottedFlux.toMapLocation());
            int distance = Math.abs(updatedData.x - controller.getLocation().getX()) + Math.abs(updatedData.y - controller.getLocation().getY());
            p("Distance to flux: " + distance);
            if(distance <= 2) {
                break;
            }

            MapData[] fluxSquares = sensing.senseSurroundingSquares(spottedFlux.toMapLocation());
            MapData goal = null;
            int count = 0, offset = archonNumber % 3;
            if(fluxSquares == null) {
                return;
            }
            for(MapData square : fluxSquares) {
                if(square.airRobot == null) {
                    goal = square;
                    if(count >= offset) {
                        break;
                    }
                    count++;
                } else {
                    count++;
                }
            }

            navigation.go(goal);
        }
        setGoal(Goal.supporttingFluxDeposit);
        trackingCount = 0;
    }

    public boolean beforeMovementCallback(MapData data) {
        switch(currentGoal) {
            case Goal.goingDirectlyToFlux:
                return updateFluxGoal(data.toMapLocation());
        }
        return true;
    }

    public boolean fluxDepositInSightCallback(MapData data) {
        p("flux spotted");
        if(currentGoal == Goal.exploringForFlux || currentGoal == Goal.goingTowardsFlux) {
            setGoal(Goal.goingDirectlyToFlux);
            spottedFlux = data;
        } else if(currentGoal == Goal.scouting) {
            boolean good = true;
            for(MapLocation m : controller.senseAlliedArchons()) {
                if(m.equals(data.location)) {
                    good = false;
                    break;
                }
            }
            ArrayList<RobotInfo> enemies;
            if((enemies = sensing.senseEnemyRobotInfoInSensorRange()).size() > 0) {
                spawnParty();
                setGoal(Goal.fight);//FIGHT!
            } else if(good) {
                setGoal(Goal.goingDirectlyToFlux);
                spottedFlux = data;
            }
        }

        return true;
    }

    /**
     * so archon teams will stay together
     */
    public void followRequest(int archonNumber, int id) {
        if(archonNumber % 2 == 1 && this.archonNumber == archonNumber + 1) {
            setGoal(Goal.followingArchon);
            followingArchonNumber = id;
        }
    }

    /**
     * Will cause the archon to explore the map in one direction and broadcast a
     * message with its location and next direction each time.
     */
    public void explore() {
        setGoal(Goal.scouting);
        //MapLocation[] archonLocations = controller.senseAlliedArchons();
        //Direction dir = controller.senseDirectionToUnownedFluxDeposit();
    	/*for (MapLocation archon: archonLocations)
        if (controller.getLocation().directionTo(archon) == dir) {
        exploreDirection = dir.opposite();
        return;
        }*/

        if(exploreDirection != null && !controller.canMove(exploreDirection)) {
            exploreDirection = controller.senseDirectionToUnownedFluxDeposit();
        }
    }

    public void goDirection(Direction d) {
        int[] delta = navigation.getDirectionDelta(d);
        int x = controller.getLocation().getX() + delta[0] * 10;
        int y = controller.getLocation().getY() + delta[1] * 10;
        MapLocation destination = new MapLocation(x, y);
        navigation.go(new MapData(destination));
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

    public int calculateMovementDelay(int heightFrom, int heightTo, boolean diagonal) {
        return diagonal ? moveDiagonalDelay : moveStraightDelay;
    }

    public void senseNewTiles() {
        sensing.senseDeltas(verticalDeltas, horizontalDeltas, diagonalDeltas);
    }

    private class Party {

        ArrayList<Robot> partyRobots;
        ArrayList<RobotInfo> partyRobotInfo;
        int partyGoal = -1;
        int partyMaxSize = 3;
        int partySize;
        boolean waitingForNewRobot;
        RobotType expectedRobotType;

        public Party(int size) {
            partyMaxSize = size;
            init();
        }

        public Party() {
            init();
        }

        public void init() {
            partySize = 0;
            partyRobots = new ArrayList<Robot>();
            partyRobotInfo = new ArrayList<RobotInfo>();
            waitingForNewRobot = false;
        }

        public int getPartySize() {
            return partySize;
        }

        public void partyUp() {
            MapLocation ret = null;
            int turn = 0;
            ArrayList<Robot> missingParty = (ArrayList<Robot>) partyRobots.clone();
            ArrayList<Robot> nearbyRobots = new ArrayList<Robot>();
            nearbyRobots.addAll(Arrays.asList(controller.senseNearbyAirRobots()));
            nearbyRobots.addAll(Arrays.asList(controller.senseNearbyGroundRobots()));
            while(turn < 15 && missingParty.size() > 0) {
                for(Robot r : partyRobots) {
                    for(Robot ro : nearbyRobots) {
                        if(r.getID() == ro.getID()) {
                            missingParty.remove(r);
                        }
                    }
                }
                turn++;
                controller.yield();
            }
        }

        public void setPartyGoal(int partyGoal) {
            if(this.partyGoal == partyGoal) {
                return;
            }
            this.partyGoal = partyGoal;
            switch(partyGoal) {
                case Goal.followingArchon:
                    System.out.println("Group is Following Archon");
                    for(Robot robot : partyRobots) {
                        messaging.sendFollowRequest(controller.getLocation(), archonNumber, robot.getID());
                    }
                    break;
                case Goal.supporttingFluxDeposit:

                    messaging.sendFollowRequest(controller.getLocation(), -1, -1);
                    break;
            }
        }

        public void addPartyMember(Robot robot, RobotInfo robotInfo) {
            if(partySize < partyMaxSize) {
                partyRobots.add(robot);
                partyRobotInfo.add(robotInfo);
                partySize++;
            }
            if(waitingForNewRobot && expectedRobotType == robotInfo.type) {
                waitingForNewRobot = false;
            }
        }

        public boolean isWaitingForNewRobot() {
            return waitingForNewRobot;
        }

        public void waitForNewUnit(RobotType robotType) {
            expectedRobotType = robotType;
            waitingForNewRobot = true;
        }
    }
}
