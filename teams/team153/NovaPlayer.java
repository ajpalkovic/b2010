package team153;

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

    public MexicanMessaging messaging;
    public NaughtyNavigation navigation;
    public SensationalSensing sensing;
    public EnergeticEnergon energon;

    public Team team;

    public boolean isAirRobot;
    public boolean isArchon, isSoldier, isChainer, isTurret, isWout, isAuraTower, isCommTower, isTeleporterTower, isTower, isRobot;

    public NovaPlayer(RobotController controller) {
        super(controller);
        map = new MapStore();
        moveStraightDelay = controller.getRobotType().moveDelayOrthogonal();
        moveDiagonalDelay = controller.getRobotType().moveDelayDiagonal();
        oldEnemies = new ArrayList<Integer>();
        oldLocations = new ArrayList<MapLocation>();
        isAirRobot = controller.getRobotType() == RobotType.ARCHON;

        messaging = new MexicanMessaging(this);
        navigation = new NaughtyNavigation(this);
        sensing = new SensationalSensing(this);
        energon = new EnergeticEnergon(this);

        messaging.sensing = sensing;
        navigation.sensing = sensing;

        isArchon = isArchon(controller.getRobotType());
        isSoldier = isSoldier(controller.getRobotType());
        isChainer = isChainer(controller.getRobotType());
        isTurret = isTurret(controller.getRobotType());
        isWout = isWout(controller.getRobotType());
        isAuraTower = isAuraTower(controller.getRobotType());
        isCommTower = isCommTower(controller.getRobotType());
        isTeleporterTower = isTeleporterTower(controller.getRobotType());

        isTower = isAuraTower || isCommTower || isTeleporterTower;
        isRobot = !isTower;
    }

    public boolean isArchon(RobotType type) {
        return type == RobotType.ARCHON;
    }
    public boolean isSoldier(RobotType type) {
        return type == RobotType.SOLDIER;
    }
    public boolean isAuraTower(RobotType type) {
        return type == RobotType.AURA;
    }
    public boolean isChainer(RobotType type) {
        return type == RobotType.CHAINER;
    }
    public boolean isCommTower(RobotType type) {
        return type == RobotType.COMM;
    }
    public boolean isTurret(RobotType type) {
        return type == RobotType.TURRET;
    }
    public boolean isTeleporterTower(RobotType type) {
        return type == RobotType.TELEPORTER;
    }
    public boolean isWout(RobotType type) {
        return type == RobotType.WOUT;
    }

    public void run() throws Exception {
        team = controller.getTeam();
        boot();
        while(true) {
            int startTurn = Clock.getRoundNum();
            controller.setIndicatorString(0, controller.getLocation().toString());
            messaging.parseMessages();

            if(isArchon) {
                energon.processEnergonTransferRequests();
            } else {
                energon.autoTransferEnergonBetweenUnits();
            }
            step();

            if(startTurn == Clock.getRoundNum() || controller.hasActionSet()) {
                controller.yield();
            }
        }
    }

    /**
     * Called once each round.
     */
    public void step() { }

    /**
     * Called once when the robot object is created.
     */
    public void boot() {
        messaging.sendNewUnit();
    }

    /**
     * sets the players current goal
     */
    public void setGoal(int goal) {
        currentGoal = goal;
        controller.setIndicatorString(1, Goal.toString(goal));
        pr("Changin goal to: " + Goal.toString(goal));
    }

    /**
     * Returns the number of turns to move between two tiles of the corresponding height.
     * For ground units, this takes into consideration the difference in tile heights.
     * For air units, this is overriden in their classes to only return the base movement delay.
     */
    public int calculateMovementDelay(boolean diagonal) {
        return diagonal ? player.moveDiagonalDelay : player.moveStraightDelay;
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
            navigation.go(new NovaMapData(location));
            followingArchonNumber = senderID;
        }
    }

    /**
     * Callback right before the moveForward method is called so a robot can check
     * if the tile is free to reevaluate its goals.
     */
    public boolean beforeMovementCallback(NovaMapData location) {
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
    public boolean enemyInSightCallback(NovaMapData location) {
        return true;
    }

    /**
     * Callback in the go method when a path is calculated.  The robot can override this to,
     * for exaple, make sure it has enough energon to get back to the archon.
     * If the callback returns false, the go method returns.
     */
    public boolean pathCalculatedCallback(LinkedList<NovaMapData> path) {
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
    public boolean tileSensedCallback(NovaMapData tile) {
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
     * callback for an enemy in sight to be overridden
     */
    public void enemyInSight(ArrayList<RobotInfo> enemies) {
    }
}
