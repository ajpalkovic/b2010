package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class SensationalSensing extends Base {

    public MapStore map;
    public MexicanMessaging messaging;
    public NaughtyNavigation navigation;
    public int airSensed = Integer.MIN_VALUE,
            groundSensed = Integer.MIN_VALUE,
            airInfoSensed = Integer.MIN_VALUE,
            groundInfoSensed = Integer.MIN_VALUE,
            enemyInfoSensed = Integer.MIN_VALUE,
            alliedInfoSensed = Integer.MIN_VALUE,
            enemyLocationSensed = Integer.MIN_VALUE,
            alliedTeleportersSensed = Integer.MIN_VALUE,
            alliedTowersSensed = Integer.MIN_VALUE,
            archonLocationsSensed = Integer.MIN_VALUE,
            nearestArchonSensed = Integer.MIN_VALUE,
            alliedTowerLocationsSensed = Integer.MIN_VALUE,
            dangerFactorSensed = Integer.MIN_VALUE;
    public Robot[] air, ground;
    public ArrayList<RobotInfo> airInfo, groundInfo, enemyRobots, alliedRobots, alliedTowerInfo;
    public ArrayList<MapLocation> enemyLocations, alliedTowerLocations;
    public HashMap<Integer, MapLocation> knownAlliedTowerLocations = new HashMap<Integer, MapLocation>();
    public HashMap<String, Integer> knownAlliedTowerIDs = new HashMap<String, Integer>();
    public MapLocation[] archonLocations;
    public MapLocation nearestArchon;
    public ArrayList<MapLocation> teleporterLocations = new ArrayList<MapLocation>();
    public int oldDataTolerance = 0; 
    public float dangerFactor=1;
    
    public SensationalSensing(NovaPlayer player) {
        super(player);
        map = player.map;
        messaging = player.messaging;
        navigation = player.navigation;
    }

    /**
     * Returns a 2D array with MapLocations or null objects based on the actual tiles this robot can sense.
     */
    public MapLocation[][] getSensibleTiles() {
        // return int locations of all tiles i can sense
        int radius = controller.getRobotType().sensorRadius();
        int size = radius * 2 + 1;
        MapLocation[][] tiles = new MapLocation[size][size];
        int currentX = controller.getLocation().getX();
        int currentY = controller.getLocation().getY();

        for(int x = -radius; x <= radius; x++) {
            for(int y = -radius; y <= radius; y++) {
                MapLocation location = new MapLocation(x + currentX, y + currentY);
                if(controller.canSenseSquare(location)) {
                    tiles[x + radius][y + radius] = location;
                } else {
                    tiles[x + radius][y + radius] = null;
                }
            }
        }

        return tiles;
    }
    public float getDangerFactor(){
    	if (dangerFactorSensed >= Clock.getRoundNum() - oldDataTolerance)
    		return dangerFactor;
    	
    	float badcount = 0;
    	
    	for (RobotInfo r : senseEnemyRobotInfoInSensorRange())
    		if (r.type.canAttackAir()){
    			badcount++;
    			if (r.location.isAdjacentTo(controller.getLocation()))
    				badcount++;
    		} else if (r.type.isAirborne()) {
    			badcount+=.5;
    		}
    		
    		
    	if (badcount <= 1)
    		dangerFactor = 1;
    	else if (badcount < 2)
    		dangerFactor = 1.5f;
    	else if (badcount < 3)
    		dangerFactor = 2;
    	else if (badcount < 4)
    		dangerFactor = 2.5f;
    	else if (badcount < 6)
    		dangerFactor = 3;
    	else dangerFactor = 4;
    	dangerFactorSensed = Clock.getRoundNum();
    	return dangerFactor;
    }
    /**
     * Iteratres through each of the tiles in sensor range of the robot.
     * This probably isn't needed anymore though.
     */
    public void senseAllTiles() {
        senseTiles(getSensibleTiles());
    }
   
    /**
     * This is used to sense the new tiles for robots with a 360 degree sensor angle.
     * Those robots pass in 3 int arrays representing the distance from the current robots location
     * of each new cell the robot can sense.  These distances are based on when the robot moves south,
     * west, and southwest respectively.
     *
     * Each of these is translated into a map location based on the direction the robot is facing.
     * ie: if the robot is moving east instead of west, the deltas are negated.
     *
     * The callback tileSensedCallback is called for each of the tiles.
     */
    public void senseDeltas(int[] verticalDeltas, int[] horizontalDeltas, int[] diagonalDeltas) {
        Direction dir = controller.getDirection();
        int currentX = controller.getLocation().getX(), currentY = controller.getLocation().getY();

        int xDelta, yDelta, x, y;
        int[] deltas;

        if(dir.dx == 0) {
            xDelta = 1;
            yDelta = dir.dy;
            deltas = verticalDeltas;
        } else if(dir.dy == 0) {
            xDelta = dir.dx;
            yDelta = 1;
            deltas = horizontalDeltas;
        } else {
            xDelta = dir.dx;
            yDelta = dir.dy;
            deltas = diagonalDeltas;
        }

        for(int c = 0; c < deltas.length; c += 2) {
            x = currentX + deltas[c] * xDelta;
            y = currentY + deltas[c + 1] * yDelta;

            senseTile(x, y);
        }
    }

    /**
     * Senses the flux at a radius of three to optimize the wout.
     */
    public void senseFlux(int[][] fluxDeltas) {
        int currentX = controller.getLocation().getX();
        int currentY = controller.getLocation().getY();
        MapLocation current;

        try {
            for(int c = 0; c < fluxDeltas.length; c++) {
                current = new MapLocation(currentX + fluxDeltas[c][0], currentY + fluxDeltas[c][1]);
                fluxDeltas[c][2] = controller.senseFluxAtLocation(current);
            }
        } catch (Exception e) {
            pa("----Caught exception in senseFlux "+e.toString());
        }
    }

    /**
     * This senses the terrain tile of an object, which only indicates the height of the tile
     * and if the tile is on the map or now.
     */
    public void senseTile(MapLocation location) {
        senseTile(location.getX(), location.getY());
    }

    public void senseTile(int x, int y) {
        MapLocation location = new MapLocation(x, y);

        if(map.sensed(x, y)) {
            return;
        }
        if(!controller.canSenseSquare(location)) {
            return;
        }
        map.set(x, y, controller.senseTerrainTile(location));
    }

    /**
     * Calls senseTile on each of the given tiles that are not null and then calls the callback
     * function tileSensedCallback for each of the resulting MapData objects.
     */
    public void senseTiles(MapLocation[][] tiles) {
        for(MapLocation[] row : tiles) {
            for(MapLocation tile : row) {
                if(tile != null) {
                    senseTile(tile);
                }
            }
        }
    }

    /**
     * This method is not done!
     * This method should be responsible for keeping a cache of all allied towers that are known to exist.
     * This way, if an archon is not in sensor range of a tower, it can still know the general direction in which to go to find one.
     */
    public ArrayList<MapLocation> senseKnownAlliedTowerLocations() {
        senseAlliedTowerLocations();        
        return new ArrayList<MapLocation>(knownAlliedTowerLocations.values());
    }

    /**
     * Returns an ArrayList of MapLocation objects for every allied teleporter tower.
     * The robot must be in range of one teleporter for this to work.
     * It will return a list of all teleporters regardless of sensor range.
     * The results are cached for two turns to save bytecodes.
     */
    public ArrayList<MapLocation> senseAlliedTeleporters() {
        if(alliedTeleportersSensed < player.cacheId) {
            try {
                List<MapLocation> loc = Arrays.asList(controller.senseAlliedTeleporters());
                if(!loc.isEmpty()) {
                	teleporterLocations.addAll(loc);
                }
                alliedTeleportersSensed = player.cacheId;
            } catch(Exception e) {
            }
        }
        return teleporterLocations;
    }

    /**
     * Returns an ArrayList of RobotInfo objects for each enemy in sensor range.
     * The results are cached for two turns to save bytecodes.
     */
    public ArrayList<RobotInfo> senseEnemyRobotInfoInSensorRange() {
        if(enemyInfoSensed >= player.cacheId) {
            return enemyRobots;
        }

        enemyRobots = new ArrayList<RobotInfo>();
        senseGroundRobotInfo();
        senseAirRobotInfo();

        for(RobotInfo robot : groundInfo) {
            if(!robot.team.equals(player.team)) {
                enemyRobots.add(robot);
            }
        }

        for(RobotInfo robot : airInfo) {
            if(!robot.team.equals(player.team)) {
                enemyRobots.add(robot);
            }
        }

        enemyInfoSensed = player.cacheId;
        return enemyRobots;
    }

    /**
     * Returns an arraylist of all of the allied towers that are in range.
     */
    public ArrayList<RobotInfo> senseAlliedTowers() {

        if(alliedTowersSensed >= player.cacheId) {
            return alliedTowerInfo;
        }

        alliedTowerInfo = new ArrayList<RobotInfo>();
        senseAlliedRobotInfoInSensorRange();

        for(RobotInfo robot : alliedRobots) {
            if(robot.type.isBuilding()) {
                alliedTowerInfo.add(robot);
            }
        }

        alliedTowersSensed = player.cacheId;
        return alliedTowerInfo;
    }

    /**
     * Returns an arraylist of all of the allied towers that are in range.
     */
    public ArrayList<MapLocation> senseAlliedTowerLocations() {
        if(alliedTowerLocationsSensed >= player.cacheId) {
            return alliedTowerLocations;
        }
        
        alliedTowerLocations = new ArrayList<MapLocation>();
        senseAlliedRobotInfoInSensorRange();
        for(RobotInfo robot : alliedRobots) 
            if(robot.type.isBuilding()) {
                alliedTowerLocations.add(robot.location);
                if (!knownAlliedTowerLocations.containsKey(robot.id)){
                	knownAlliedTowerLocations.put(robot.id, robot.location);
                	knownAlliedTowerIDs.put(robot.location.getX() + "," + robot.location.getY(), robot.id);
                }
            }        
        alliedTowerLocationsSensed = player.cacheId;
        return alliedTowerLocations;
    }

    /**
     * Returns an ArrayList of RobotInfo objects for each ally in sensor range.
     * The results are cached for two turns to save bytecodes.
     */
    public ArrayList<RobotInfo> senseAlliedRobotInfoInSensorRange() {
        if(alliedInfoSensed >= player.cacheId) {
            return alliedRobots;
        }

        alliedRobots = new ArrayList<RobotInfo>();
        senseGroundRobotInfo();

        for(RobotInfo robot : groundInfo) {
            if(robot.team.equals(player.team)) {
                alliedRobots.add(robot);
            }
        }

        alliedInfoSensed = player.cacheId;
        return alliedRobots;
    }

    /**
     * Returns an ArrayList of MapLocation objects for each enemy in sensor range.
     * The results are cached for two turns to save bytecodes.
     */
    public ArrayList<MapLocation> senseEnemyRobotLocations() {
        if(enemyLocationSensed >= player.cacheId) {
            return enemyLocations;
        }

        senseEnemyRobotInfoInSensorRange();
        enemyLocations = new ArrayList<MapLocation>();
        for(RobotInfo r : enemyRobots) {
            enemyLocations.add(r.location);
        }

        enemyLocationSensed = player.cacheId;
        return enemyLocations;
    }

    /**
     * Returns an array of all of the Archon MapLocations
     * The results are cached for two turns to save bytecodes.
     */
    public MapLocation[] senseArchonLocations() {
        if(archonLocationsSensed >= player.cacheId) {
            return archonLocations;
        }
        archonLocations = controller.senseAlliedArchons();
        archonLocationsSensed = player.cacheId;
        return archonLocations;
    }

    /**
     * Returns the MapLocation of the archon closest to the robot.
     * The results are cached for two turns to save bytecodes.
     */
    public MapLocation senseClosestArchon() {
        if(nearestArchonSensed >= player.cacheId) {
            return nearestArchon;
        }
        
        senseArchonLocations();
        nearestArchon = navigation.findClosest(archonLocations);
        nearestArchonSensed = player.cacheId;
        return nearestArchon;
    }

    /**
     * Returns an Array of simple Robot objects for each air robot in range.
     * Note: Both enemy and ally robots will be returned.
     * The results are cached for two turns to save bytecodes.
     */
    public Robot[] senseAirRobots() {
        if(airSensed >= player.cacheId) {
            return air;
        }

        try {
            air = controller.senseNearbyAirRobots();
            airSensed = player.cacheId;
        } catch(Exception e) {
            System.out.println("----Caught Exception in getAirRobots.  Exception: " + e.toString());
        }
        return air;
    }

    /**
     * Returns an Array of simple Robot objects for each ground robot in range.
     * Note: Both enemy and ally robots will be returned.
     * The results are cached for two turns to save bytecodes.
     */
    public Robot[] senseGroundRobots() {
        if(groundSensed >= player.cacheId) {
            return ground;
        }

        try {
            ground = controller.senseNearbyGroundRobots();
            groundSensed = player.cacheId;
        } catch(Exception e) {
            System.out.println("----Caught Exception in getGroundRobots.  Exception: " + e.toString());
        }
        return ground;
    }

    /**
     * Returns an ArrayList of RobotInfo objects for every air robot in range.
     * Note: Both enemy and ally robots will be returned.
     * The results are cached for two turns to save bytecodes.
     */
    public ArrayList<RobotInfo> senseAirRobotInfo() {
        if(airInfoSensed >= player.cacheId) {
            return airInfo;
        }

        senseAirRobots();
        airInfo = new ArrayList<RobotInfo>();
        for(Robot robot : air) {
            try {
                if(controller.canSenseObject(robot)) {
                    airInfo.add(controller.senseRobotInfo(robot));
                }
            } catch(Exception e) {
                System.out.println("----Caught Exception in getAirRobotInfo.  Exception: " + e.toString());
            }
        }
        airInfoSensed = player.cacheId;
        return airInfo;
    }

    /**
     * Returns an ArrayList of RobotInfo objects for every ground robot in range.
     * Note: Both enemy and ally robots will be returned.
     * The results are cached for two turns to save bytecodes.
     */
    public ArrayList<RobotInfo> senseGroundRobotInfo() {
        if(groundInfoSensed >= player.cacheId) {
            return groundInfo;
        }

        senseGroundRobots();
        groundInfo = new ArrayList<RobotInfo>();
        for(Robot robot : ground) {
            try {
                if(controller.canSenseObject(robot)) {
                    groundInfo.add(controller.senseRobotInfo(robot));
                }
            } catch(Exception e) {
                System.out.println("----Caught Exception in getGroundRobotInfo.  Exception: " + e.toString());
            }
        }
        groundInfoSensed = player.cacheId;
        return groundInfo;
    }
}
