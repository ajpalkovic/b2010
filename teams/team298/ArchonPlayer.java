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
    public int minMoveTurns = 0, moveTurns = 0;
    public ArrayList<MapLocation> enemyLocations;
    public int turnsSinceLastSpawn = 0, turnsSinceMessageForEnemyRobotsSent = 30, turnsSinceTowerStuffDone = 0;
    boolean attacking;
    boolean attackingInitialized;
    
    
    public MapLocation closestEnemy;
    public MapLocation currentEnemy;
    public int closestEnemySeen=Integer.MIN_VALUE, closestEnemyTolerance = 10;

    public ArchonPlayer(RobotController controller) {
        super(controller);
        spawning = new SporadicSpawning(this);
        towers = new TenaciousTowers(this);
        
        minMoveTurns = RobotType.ARCHON.moveDelayDiagonal() + 3;
        enemyLocations = new ArrayList<MapLocation>();
    }

    public void step() {
    	if (sensing.getDangerFactor() >= 2)
    		setGoal(Goal.collectingFlux);
    	spawning.changeModeToAttacking();
        flux.transferFluxBetweenArchons();

        if (energon.isEnergonLow() && sensing.getDangerFactor() > 1)
            messaging.sendLowEnergon();
        attacking = sensing.senseEnemyRobotInfoInSensorRange().size() > 1 || closestEnemySeen+closestEnemyTolerance > Clock.getRoundNum();

        if(currentGoal == Goal.collectingFlux) {
            navigation.changeToArchonNavigationGoal(true);

           //add a small delay to archon movement so the other dudes can keep up
            if(attacking || navigation.archonNavigationGoal.distanceToLeader() > 25) {
                navigation.moveOnce(false);
            } else if((moveTurns >= minMoveTurns && controller.getRoundsUntilMovementIdle() == 0)) {
                int status = navigation.moveOnce(true);
                //p("Status: "+status);
                if(status == Status.success) moveTurns = 0;
            }
            moveTurns++;
        }

        if(turnsSinceLastSpawn > 2 && Clock.getRoundNum() > 100) {
            int status = spawning.spawnRobot();
            if(status == Status.cannotSupportUnit) turnsSinceLastSpawn = -1;
            else if(status == Status.success) {
                turnsSinceLastSpawn = -1;
                try {
                    messaging.sendFollowRequest(controller.senseGroundRobotAtLocation(spawning.spawnLocation).getID());
                } catch(Exception e) {
                    pa("----Exception Caught in sendFollowRequest()");
                }
            }
        }
        turnsSinceLastSpawn++;
        if (attacking) {
        	setGoal(Goal.collectingFlux);
        }
        switch(currentGoal) {
            case Goal.collectingFlux:
                if(turnsSinceTowerStuffDone < 0) {
                    //try to spawn a tower every turn
                    sensing.senseAlliedTeleporters();
                    if(spawning.canSupportTower(RobotType.TELEPORTER)) {
                        turnsSinceTowerStuffDone = 1;
                        towers.doTowerStuff(attacking);
                    } else {
                        turnsSinceTowerStuffDone = 5;
                    }
                }
                turnsSinceTowerStuffDone--;

                break;
            case Goal.askingForTowerLocation:
                towers.askForTowerLocation();
                break;
            case Goal.movingToPreviousTowerLocation:
                towers.moveToPreviousTowerLocation();
                break;
            case Goal.placingTeleporter:
                towers.placeTeleporter();
                break;
            case Goal.idle:
            case Goal.attackingEnemyArchons:
                setGoal(Goal.collectingFlux);
                break;
            case Goal.movingToTowerSpawnLocation:
                towers.moveToTowerSpawnLocation();
                break;
        }
    }

    public void followRequestMessageCallback(MapLocation location, int idOfSendingArchon, int idOfRecipient) {
        //p("followRequestCallback "+idOfSendingArchon+" "+location);
        if(idOfSendingArchon < archonLeader) {
            archonLeader = idOfSendingArchon;
            isLeader = false;
        }

        if(archonLeader == idOfSendingArchon) {
            //p("Updating");
            navigation.archonNavigationGoal.updateArchonGoal(location, archonLeader);
        }
    }

    public void enemyInSight(MapLocation[] locations, int[] ints, String[] strings, int locationStart, int intStart, int stringStart, int count) {
        closestEnemySeen = Clock.getRoundNum();
        closestEnemy = navigation.findClosest(locations, locationStart);
    }
    
    public void enemyInSight(MapLocation location, int energon, String type) {
        if(closestEnemy == null || controller.getLocation().distanceSquaredTo(location) < controller.getLocation().distanceSquaredTo(closestEnemy)) {
            closestEnemySeen = Clock.getRoundNum();
            closestEnemy = location;
        }
    }

    public void enemyInSight(ArrayList<RobotInfo> enemies) {
        if(enemies == null || enemies.size() == 0) return;
        RobotInfo closest = navigation.findClosest(enemies);
        if(closestEnemy == null || controller.getLocation().distanceSquaredTo(closest.location) < controller.getLocation().distanceSquaredTo(closestEnemy)) {
            closestEnemySeen = Clock.getRoundNum();
            closestEnemy = closest.location;
        }
    }

    public void towerBuildLocationResponseCallback(MapLocation[] locations) {
        towers.idealTowerSpawnLocations = locations;
    }

    public void towerPingLocationCallback(MapLocation location, int robotID) {
    	sensing.senseAlliedTowerLocations();
		if (!sensing.knownAlliedTowerLocations.containsKey(robotID)){
			sensing.knownAlliedTowerLocations.put(new Integer(robotID), location);
			sensing.knownAlliedTowerIDs.put(location.getX() + "," + location.getY(), robotID);
		}
    }

    public void newUnit(int senderID, MapLocation location, String robotType) {
    	if (RobotType.valueOf(robotType).isBuilding()){    	
    		if (sensing.knownAlliedTowerLocations == null)
    			sensing.senseAlliedTowerLocations();
    		
    		if (!sensing.knownAlliedTowerLocations.containsKey(senderID)){
    			sensing.knownAlliedTowerLocations.put(new Integer(senderID), location);
    			sensing.knownAlliedTowerIDs.put(location.getX() + "," + location.getY(), senderID);    			
    		}
    	}
    }
    public void boot() {
        archonLeader = controller.getRobot().getID();
        isLeader = true;
        
        sensing.senseAllTiles();
        team = controller.getTeam();
        senseArchonNumber();
        cacheId = archonNumber;

        //spread out so we dont group up
        MapLocation center = navigation.findAverage(sensing.senseArchonLocations());
        Direction dir = center.directionTo(controller.getLocation());
        navigation.changeToDirectionGoal(dir, false);
        navigation.moveOnce(true);
        navigation.popGoal();

        //p("Spread Out");

        int x = controller.getLocation().getX();
        int y = controller.getLocation().getY();
        
        setGoal(Goal.attackingEnemyArchons);
        if(archonNumber == 1) {
        	
            int dcounter = 6;
        	TerrainTile top = null;
            TerrainTile bottom = null;
            TerrainTile right = null;
            TerrainTile left = null;
            while (top == null && dcounter >= 0){
            	sensing.senseTile(x,y-dcounter);
            	top = controller.senseTerrainTile(new MapLocation(x, y-dcounter));                              
            	dcounter--;
            }
            dcounter = 6;
            while (bottom == null && dcounter >= 0){
            	sensing.senseTile(x,y-dcounter);
            	bottom = controller.senseTerrainTile(new MapLocation(x, y+dcounter));
            	dcounter--;
            }
            dcounter = 6;
            while (right == null && dcounter >= 0){
            	sensing.senseTile(x+dcounter,y);
            	right = controller.senseTerrainTile(new MapLocation(x+dcounter, y));
            	dcounter--;
            }
            dcounter = 6;
            while (left == null && dcounter >= 0){
            	sensing.senseTile(x-dcounter,y);
            	left = controller.senseTerrainTile(new MapLocation(x-dcounter, y));
            	dcounter--;
            }
            boolean t = top != null && top.getType() == TerrainType.OFF_MAP;
            boolean b = bottom != null && bottom.getType() == TerrainType.OFF_MAP;
            boolean l = left != null && left.getType() == TerrainType.OFF_MAP;
            boolean r = right != null && right.getType() == TerrainType.OFF_MAP;
            if (!t && !b && !l && !r)
            	dir = Direction.SOUTH_EAST;
            else if(b) {
                if(r) dir = Direction.NORTH_WEST;
                else if(l) dir = Direction.NORTH_EAST;
                else dir = Direction.NORTH;
            } else if(t) {
                if(r) dir = Direction.SOUTH_WEST;
                else if(l) dir = Direction.SOUTH_EAST;
                else dir = Direction.SOUTH;
            } 
            else{
                dir = r ? Direction.WEST : Direction.EAST;
            }
            pa(dir.toString());
            try{
            controller.setDirection(dir);
            } catch (Exception e){
            	pa("----Caught Exception in setting direction in boot. Exception: " + e.getMessage());
            }
            
            navigation.changeToDirectionGoal(dir, false);
            navigation.moveOnce(true);
            //p("Leader moved away");
        } else {
        }

        //if(archonNumber != 1) minMoveTurns -= 2;

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
        m.ints = new int[] {min, robot.getID()};
        try {
            controller.broadcast(m);
        } catch(Exception e) {
            System.out.println("----Caught Exception in senseArchonNumber.  Exception: " + e.toString());
        }
        hasReceivedUniqueMsg = true;
        System.out.println("Number: "+min+" "+archonLeader);
    }

    public void senseNewTiles() {
        sensing.senseDeltas(verticalDeltas, horizontalDeltas, diagonalDeltas);
    }

    public boolean pathStepTakenCallback() {
        senseNewTiles();
        messaging.sendFollowRequest(BroadcastMessage.everyone);
        return true;
    }

    public void lowEnergonMessageCallback(MapLocation location1, int amount, int isAirUnit, int round) {
    }
}
