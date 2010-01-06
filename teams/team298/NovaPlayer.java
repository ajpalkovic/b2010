package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class NovaPlayer extends Base {

    public MapStore map;
    public int moveStraightDelay, moveDiagonalDelay;
    public int leftWall = Integer.MIN_VALUE, rightWall = Integer.MAX_VALUE, topWall = Integer.MIN_VALUE, bottomWall = Integer.MAX_VALUE;
    public int leftWallBounds = Integer.MAX_VALUE, rightWallBounds = Integer.MIN_VALUE,
            topWallBounds = Integer.MAX_VALUE, bottomWallBounds = Integer.MIN_VALUE;
    public ArrayList<Integer> oldEnemies;
    public ArrayList<MapLocation> oldLocations;
    public int trackingCount = 0;
    public int currentGoal = 0;
    public int followingArchonNumber = -1;

    public Messaging messaging;
    public Navigation navigation;
    public SensationalSensing sensing;
    public EnergeticEnergon energon;

    public Team team;

    public boolean isAirRobot;
    public boolean isArchon, isWorker, isScout, isCannon, isSoldier, isChanneler;

    public NovaPlayer(RobotController controller) {
        super(controller);
        map = new MapStore();
        moveStraightDelay = controller.getRobotType().moveDelayOrthogonal();
        moveDiagonalDelay = controller.getRobotType().moveDelayDiagonal();
        oldEnemies = new ArrayList<Integer>();
        oldLocations = new ArrayList<MapLocation>();
        isAirRobot = controller.getRobotType() == RobotType.ARCHON || controller.getRobotType() == RobotType.SCOUT;

        messaging = new Messaging(this);
        navigation = new Navigation(this);
        sensing = new SensationalSensing(this);
        energon = new EnergeticEnergon(this);

        messaging.sensing = sensing;
        navigation.sensing = sensing;

        isArchon = isArchon(controller.getRobotType());
        isWorker = isWorker(controller.getRobotType());
        isScout = isScout(controller.getRobotType());
        isCannon = isCannon(controller.getRobotType());
        isSoldier = isSoldier(controller.getRobotType());
        isChanneler = isChanneler(controller.getRobotType());
    }

    public boolean isArchon(RobotType type) {
        return type == RobotType.ARCHON;
    }

    public boolean isWorker(RobotType type) {
        return type == RobotType.WORKER;
    }

    public boolean isScout(RobotType type) {
        return type == RobotType.SCOUT;
    }

    public boolean isCannon(RobotType type) {
        return type == RobotType.CANNON;
    }

    public boolean isSoldier(RobotType type) {
        return type == RobotType.SOLDIER;
    }

    public boolean isChanneler(RobotType type) {
        return type == RobotType.CHANNELER;
    }

    public void run() throws Exception {
        team = controller.getTeam();
        while(true) {
            if(energon.isEnergonLow()) {
                energon.requestEnergonTransfer();
            }
        }
    }

    /**
     * sets the players current goal
     */
    public void setGoal(int goal) {
        currentGoal = goal;
        controller.setIndicatorString(1, Goal.toString(goal));
        p("Changin goal to: " + Goal.toString(goal));
    }

    /**
     * Returns the number of turns to move between two tiles of the corresponding height.
     * For ground units, this takes into consideration the difference in tile heights.
     * For air units, this is overriden in their classes to only return the base movement delay.
     */
    public int calculateMovementDelay(int heightFrom, int heightTo, boolean diagonal) {
        int cost = diagonal ? player.moveDiagonalDelay : player.moveStraightDelay;
        int delta = heightFrom - heightTo;
        if(Math.abs(delta) <= 1) {
            return cost;
        }
        if(delta > 0) {
            return cost + GameConstants.FALLING_PENALTY_RATE * delta;
        } else {
            return cost + GameConstants.CLIMBING_PENALTY_RATE * delta * delta;
        }
    }

    /**
     * Default method to update the map each time the robot moves.
     */
    public void senseNewTiles() {
        sensing.senseAllTiles();
    }

    /***************************************************************************
     * CALLBACKS
     **************************************************************************/
    public void moveMessageCallback(MapLocation location) {
        if(!controller.getRobotType().isAirborne() && controller.getLocation().equals(location)) {
            navigation.moveOnce(navigation.getMoveableDirection(Direction.NORTH));
        }
    }

    public void lowEnergonMessageCallback(MapLocation location1, MapLocation location2, int amount, int isAirUnit) {
        if(location2.equals(controller.getLocation())) {
            energon.addRequest(location1, isAirUnit == 1, amount);
        }
    }

    public void lowAlliedUnitMessageCallback() {
        if(controller.getRobotType() != RobotType.ARCHON) {
            energon.lowAllyRequests.clear();
        }
        energon.lowAllyRequestsTurn = Clock.getRoundNum();
    }

    public void lowAlliedUnitMessageCallback(MapLocation location, int level, int reserve, int max) {
        energon.addLowAllyRequest(location, level, reserve, max);
    }

    public void followRequestMessageCallback(MapLocation location, int i, int senderID, int recipientID) {
        if(controller.getRobotType() == RobotType.ARCHON) {
            followRequest(i, senderID);
        } else if(recipientID == robot.getID() && currentGoal != Goal.followingArchon) {
            setGoal(Goal.followingArchon);
            navigation.go(new MapData(location));
            followingArchonNumber = senderID;
        }
    }

    /**
     * Callback right before the moveForward method is called so a robot can check
     * if the tile is free to reevaluate its goals.
     */
    public boolean beforeMovementCallback(MapData location) {
        return true;
    }

    /**
     * Called when the direction in moveOnceTowardsLocation is calculated.
     */
    public boolean directionCalculatedCallback(Direction dir) {
        return true;
    }

    /**
     * Callback when an enemy is spotted in the sense methods.
     * The MapData object contains the robots.
     * Return false to return from the calling method.
     */
    public boolean enemyInSightCallback(MapData location) {
        return true;
    }

    /**
     * Callback for when a flux deposit is spotted.
     */
    public boolean fluxDepositInSightCallback(MapData location) {
        return true;
    }

    /**
     * Callback in the go method when a path is calculated.  The robot can override this to,
     * for exaple, make sure it has enough energon to get back to the archon.
     * If the callback returns false, the go method returns.
     */
    public boolean pathCalculatedCallback(LinkedList<MapData> path) {
        return true;
    }

    /**
     * Callback in the go method each time the robot takes a step.  One use would be to
     * check if an enemy is in sight, each time you move.
     * If the callback returns false, the go method returns.
     */
    public boolean pathStepTakenCallback() {
        senseNewTiles();
        return true;
    }

    /**
     * Callback for when a tile is sensed.  Return false to stop sensing.
     */
    public boolean tileSensedCallback(MapData tile) {
        return true;
    }

    public void followRequest(int archonNum, int id) {
    }

    /**
     * is called when received an enemyInSight message - to be overloaded
     * 
     **/
    public void enemyInSight(MapLocation enemyLocation, int enemyID, String enemyType) {
    }

    /**
     *  is called when received a newUnit message - to be overloaded;
     */
    public void newUnit(int senderID, MapLocation location, String robotType) {
    }

    /**
     *  is called when Find Blocks message is receive to be overloaded;
     */
    public void findBlocks(MapLocation fluxLocation, Direction d) {
    }

    /**
     * callback for an enemy in sight to be overridden
     */
    public void enemyInSight(ArrayList<RobotInfo> enemies) {
    }
}
