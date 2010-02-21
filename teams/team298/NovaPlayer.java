package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class NovaPlayer extends Base {

    public MapStore map;
    public int moveStraightDelay, moveDiagonalDelay;
    public ArrayList<Integer> oldEnemies;
    public ArrayList<MapLocation> oldLocations;
    public int trackingCount = 0;
    public int currentGoal = 0;
    public int followingArchonNumber = -1;
    public int archonLeader = -1;
    public boolean isLeader = false;
    public boolean hasReceivedUniqueMsg, ignoreFollowRequest = false;
    public int turnsSinceEnemiesSeen = 0;
    public int cacheId = 0;

    public MexicanMessaging messaging;
    public NaughtyNavigation navigation;
    public SensationalSensing sensing;
    public EnergeticEnergon energon;
    public FluxinFlux flux;
    public SporadicSpawning spawning;
    public TenaciousTowers towers;
    public int turnsSinceEnergonSent = -1, turnsSinceEnergonProcessed = -1;

    public Team team;

    public boolean isAirRobot;
    public boolean isArchon, isSoldier, isChainer, isTurret, isWout, isAuraTower, isCommTower, isTeleporterTower, isTower, isRobot;

    public static final boolean debug = false;

    public NovaPlayer(RobotController controller) {
        super(controller);
        map = new MapStore();
        moveStraightDelay = controller.getRobotType().moveDelayOrthogonal();
        moveDiagonalDelay = controller.getRobotType().moveDelayDiagonal();
        oldEnemies = new ArrayList<Integer>();
        oldLocations = new ArrayList<MapLocation>();
        isAirRobot = controller.getRobotType() == RobotType.ARCHON;
        hasReceivedUniqueMsg = false;

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

        messaging = new MexicanMessaging(this);
        navigation = new NaughtyNavigation(this);
        sensing = new SensationalSensing(this);
        energon = new EnergeticEnergon(this);
        flux = new FluxinFlux(this);

        messaging.sensing = sensing;
        navigation.sensing = sensing;
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

            beginTurn();

            int b = Clock.getBytecodeNum(), t = Clock.getRoundNum();
            messaging.parseMessages();
            if(debug) printBytecode(t, b, "Parse Messages: ");

            if(turnsSinceEnergonProcessed < 0) {
                if(isArchon) {
                    energon.processEnergonTransferRequests();
                } else if(!isTower) {
                    b = Clock.getBytecodeNum();
                    t = Clock.getRoundNum();
                    energon.autoTransferEnergonBetweenUnits();
                    if(debug) printBytecode(t, b, "Process energon: ");
                }
                turnsSinceEnergonProcessed = 0;
            }
            turnsSinceEnergonProcessed--;
            
            step();

            if(!isArchon && !isTower && turnsSinceEnergonSent < 0 && energon.isEnergonSortaLow()) {
                messaging.sendLowEnergon();
                turnsSinceEnergonSent = 1;
            }
            turnsSinceEnergonSent--;

            b = Clock.getBytecodeNum();
            t = Clock.getRoundNum();
            messaging.doSend();
            if(debug) printBytecode(t, b, "Process messages: ");

            if(startTurn == Clock.getRoundNum() || controller.hasActionSet()) {
                if(debug) p("yield");
                controller.yield();
            }

            cacheId++;
        }
    }

    public void beginTurn() {
        
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
        sensing.senseAllTiles();
    }

    /**
     * sets the players current goal
     */
    public void setGoal(int goal) {
        currentGoal = goal;
        controller.setIndicatorString(1, Goal.toString(goal));
        //p("Changin goal to: " + Goal.toString(goal));
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
    public void followRequestMessageCallback(MapLocation location, int idOfSendingArchon, int idOfRecipient) {
        if (idOfRecipient == robot.getID() || hasReceivedUniqueMsg) {
            if(archonLeader > 0 && idOfSendingArchon == archonLeader) {
                hasReceivedUniqueMsg = true;
                archonLeader = idOfSendingArchon;

                if(!ignoreFollowRequest) {
                    navigation.changeToFollowingArchonGoal(true);
                }

                navigation.followArchonGoal.updateArchonGoal(location, archonLeader);
            }
        }
    }

    public void moveMessageCallback(MapLocation location) {
        if(!controller.getRobotType().isAirborne() && controller.getLocation().equals(location)) {
            navigation.changeToDirectionGoal(navigation.getMoveableDirection(Direction.NORTH), false);
            navigation.moveOnce(true);
            navigation.popGoal();
        }
    }

    public void lowEnergonMessageCallback(MapLocation location1, int amount, int isAirUnit, int round) {
        energon.addRequest(location1, isAirUnit == 1, amount, round);
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
     * is called when received an enemyInSight message - to be overloaded
     * 
     **/
    public void enemyInSight(MapLocation[] locations, int[] ints, String[] strings, int locationStart, int intStart, int stringStart, int count) {
    }
    public void enemyInSight(MapLocation location, int energon, String type) {
        
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

    public void towerBuildLocationRequestCallback(int requesterID) {
    	
    }

    public void towerBuildLocationResponseCallback(MapLocation[] locations) {
        
    }
    public void towerPingLocationCallback(MapLocation location, int robotID) {
    	
    }
}
