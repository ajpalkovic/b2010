package team153;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

import java.lang.reflect.Array;
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
            alliedTowerLocationsSensed = Integer.MIN_VALUE;
    public Robot[] air, ground;
    public ArrayList<RobotInfo> airInfo, groundInfo, enemyRobots, alliedRobots, alliedTowerInfo;
    public ArrayList<MapLocation> enemyLocations, alliedTowerLocations;
    public MapLocation[] archonLocations;
    public MapLocation nearestArchon;
    public ArrayList<MapLocation> teleporterLocations = new ArrayList<MapLocation>();
    public int oldDataTolerance = 1;

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

    public boolean runSensorCallbacks(MapLocation data) {
        boolean ret = true;
        if(data != null) {
            /*
            TODO:: This breaks AttackPlayer badly.
            if(data.airRobot != null || data.groundRobot != null) {
            ret = player.enemyInSightCallback(data) && ret;
            }*/
        }
        return ret;
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
        int[] directionDelta = navigation.getDirectionDelta(dir);
        int currentX = controller.getLocation().getX(), currentY = controller.getLocation().getY();

        int xDelta, yDelta, x, y;
        int[] deltas;

        if(directionDelta[0] == 0) {
            xDelta = 1;
            yDelta = directionDelta[1];
            deltas = verticalDeltas;
        } else if(directionDelta[1] == 0) {
            xDelta = directionDelta[0];
            yDelta = 1;
            deltas = horizontalDeltas;
        } else {
            xDelta = directionDelta[0];
            yDelta = directionDelta[1];
            deltas = diagonalDeltas;
        }

        for(int c = 0; c < deltas.length; c += 2) {
            x = currentX + deltas[c] * xDelta;
            y = currentY + deltas[c + 1] * yDelta;

            senseTile(x, y);
            runSensorCallbacks(new MapLocation(x, y));
        }
    }

    /**
     * Sense the 8 tiles around the robot.
     */
    public void senseSurroundingSquares() {
        senseSurroundingSquares(controller.getLocation());
    }

    /**
     * Sense the 8 tiles around the robot.
     */
    public void senseSurroundingSquares(MapLocation location) {
        int x = location.getX(), y = location.getY();

        senseTile(x - 1, y - 1);
        senseTile(x, y - 1);
        senseTile(x + 1, y - 1);

        senseTile(x - 1, y);
        senseTile(x, y);
        senseTile(x + 1, y);

        senseTile(x - 1, y + 1);
        senseTile(x, y + 1);
        senseTile(x + 1, y + 1);
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
                    if(!runSensorCallbacks(tile)) {
                        return;
                    }
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
        return alliedTowerLocations;
    }

    /**
     * Returns an ArrayList of MapLocation objects for every allied teleporter tower.
     * The robot must be in range of one teleporter for this to work.
     * It will return a list of all teleporters regardless of sensor range.
     * The results are cached for two turns to save bytecodes.
     */
    public ArrayList<MapLocation> senseAlliedTeleporters() {
        if(alliedTeleportersSensed < Clock.getRoundNum() - oldDataTolerance) {
            try {
                List<MapLocation> loc = Arrays.asList(controller.senseAlliedTeleporters());
                if(!loc.isEmpty()) {
                    teleporterLocations.addAll(loc);
                }
                alliedTeleportersSensed = Clock.getRoundNum();
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
        if(enemyInfoSensed >= Clock.getRoundNum() - oldDataTolerance) {
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

        enemyInfoSensed = Clock.getRoundNum();
        return enemyRobots;
    }

    /**
     * Returns an arraylist of all of the allied towers that are in range.
     */
    public ArrayList<RobotInfo> senseAlliedTowers() {
        if(alliedTowersSensed >= Clock.getRoundNum() - oldDataTolerance) {
            return alliedTowerInfo;
        }

        alliedTowerInfo = new ArrayList<RobotInfo>();
        senseAlliedRobotInfoInSensorRange();

        for(RobotInfo robot : alliedRobots) {
            if(robot.type.isBuilding()) {
                alliedTowerInfo.add(robot);
            }
        }

        alliedTowersSensed = Clock.getRoundNum();
        return alliedTowerInfo;
    }

    /**
     * Returns an arraylist of all of the allied towers that are in range.
     */
    public ArrayList<MapLocation> senseAlliedTowerLocations() {
        if(alliedTowerLocationsSensed >= Clock.getRoundNum() - oldDataTolerance) {
            return alliedTowerLocations;
        }

        alliedTowerLocations = new ArrayList<MapLocation>();
        senseAlliedRobotInfoInSensorRange();
        for(RobotInfo robot : alliedRobots) {
            if(robot.type.isBuilding()) {
                alliedTowerLocations.add(robot.location);
            }
        }

        alliedTowerLocationsSensed = Clock.getRoundNum();
        return alliedTowerLocations;
    }

    /**
     * Returns an ArrayList of RobotInfo objects for each ally in sensor range.
     * The results are cached for two turns to save bytecodes.
     */
    public ArrayList<RobotInfo> senseAlliedRobotInfoInSensorRange() {
        if(alliedInfoSensed >= Clock.getRoundNum() - oldDataTolerance) {
            return alliedRobots;
        }

        alliedRobots = new ArrayList<RobotInfo>();
        senseGroundRobotInfo();

        for(RobotInfo robot : groundInfo) {
            if(robot.team.equals(player.team)) {
                alliedRobots.add(robot);
            }
        }

        alliedInfoSensed = Clock.getRoundNum();
        return alliedRobots;
    }

    /**
     * Returns an ArrayList of MapLocation objects for each enemy in sensor range.
     * The results are cached for two turns to save bytecodes.
     */
    public ArrayList<MapLocation> senseEnemyRobotLocations() {
        if(enemyLocationSensed >= Clock.getRoundNum() - oldDataTolerance) {
            return enemyLocations;
        }

        senseEnemyRobotInfoInSensorRange();
        enemyLocations = new ArrayList<MapLocation>();
        for(RobotInfo r : enemyRobots) {
            enemyLocations.add(r.location);
        }

        enemyLocationSensed = Clock.getRoundNum();
        return enemyLocations;
    }

    /**
     * Returns an array of all of the Archon MapLocations
     * The results are cached for two turns to save bytecodes.
     */
    public MapLocation[] senseArchonLocations() {
        if(archonLocationsSensed >= Clock.getRoundNum() - oldDataTolerance) {
            return archonLocations;
        }
        archonLocations = controller.senseAlliedArchons();
        archonLocationsSensed = Clock.getRoundNum();
        return archonLocations;
    }

    /**
     * Returns the MapLocation of the archon closest to the robot.
     * The results are cached for two turns to save bytecodes.
     */
    public MapLocation senseClosestArchon() {
        if(nearestArchonSensed >= Clock.getRoundNum() - oldDataTolerance) {
            return nearestArchon;
        }
        
        senseArchonLocations();
        nearestArchon = navigation.findClosest(archonLocations);
        nearestArchonSensed = Clock.getRoundNum();
        return nearestArchon;
    }

    /**
     * Returns an Array of simple Robot objects for each air robot in range.
     * Note: Both enemy and ally robots will be returned.
     * The results are cached for two turns to save bytecodes.
     */
    public Robot[] senseAirRobots() {
        if(airSensed >= Clock.getRoundNum() - oldDataTolerance) {
            return air;
        }

        try {
            air = controller.senseNearbyAirRobots();
            airSensed = Clock.getRoundNum();
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
        if(groundSensed >= Clock.getRoundNum() - oldDataTolerance) {
            return ground;
        }

        try {
            ground = controller.senseNearbyGroundRobots();
            groundSensed = Clock.getRoundNum();
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
        if(airInfoSensed >= Clock.getRoundNum() - oldDataTolerance) {
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
        airInfoSensed = Clock.getRoundNum();
        return airInfo;
    }

    /**
     * Returns an ArrayList of RobotInfo objects for every ground robot in range.
     * Note: Both enemy and ally robots will be returned.
     * The results are cached for two turns to save bytecodes.
     */
    public ArrayList<RobotInfo> senseGroundRobotInfo() {
        if(groundInfoSensed >= Clock.getRoundNum() - oldDataTolerance) {
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
        groundInfoSensed = Clock.getRoundNum();
        return groundInfo;
    }
}
