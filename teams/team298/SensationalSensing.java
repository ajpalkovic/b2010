package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class SensationalSensing extends Base {

    public MapStore map;
    public Messaging messaging;
    public Navigation navigation;
    public RobotCache robotCache;

    public SensationalSensing(NovaPlayer player) {
        super(player);
        map = player.map;
        messaging = player.messaging;
        robotCache = new RobotCache();
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

    public boolean runSensorCallbacks(MapData data) {
        boolean ret = true;
        if(data != null) {
            ret = player.tileSensedCallback(data);
            if(data.airRobot != null || data.groundRobot != null) {
                ret = player.enemyInSightCallback(data) && ret;
            }
            if(data.deposit != null) {
                ret = player.fluxDepositInSightCallback(data) && ret;
            }
        }
        return ret;
    }

    /**
     * Returns true if the unit can sense an enemy at the given location
     */
    public boolean canSenseEnemy(MapLocation enemyLocation) {
        ArrayList<MapLocation> locations = senseEnemyRobotLocations();
        System.out.println("Can sense enemy!");
        for(MapLocation l : locations) {
            if(l.getX() == enemyLocation.getX() && l.getX() == enemyLocation.getY()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Iteratres through each of the tiles in sensor range of the
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
            runSensorCallbacks(senseTile(new MapLocation(x, y)));
        }
    }

    public ArrayList<RobotInfo> senseEnemyRobotInfoInSensorRange() {
        ArrayList<RobotInfo> ret = new ArrayList<RobotInfo>();
        ArrayList<RobotInfo> ground = robotCache.getGroundRobotInfo(), air = robotCache.getAirRobotInfo();

        for(RobotInfo robot : ground) {
            if(!robot.team.equals(player.team)) {
                ret.add(robot);
            }
        }

        for(RobotInfo robot : air) {
            if(!robot.team.equals(player.team)) {
                ret.add(robot);
            }
        }

        return ret;
    }

    public ArrayList<MapLocation> senseEnemyRobotLocations() {
        ArrayList<RobotInfo> enemyRobots = senseEnemyRobotInfoInSensorRange();
        ArrayList<MapLocation> returnList;
        returnList = new ArrayList<MapLocation>();
        for(RobotInfo r : enemyRobots) {
            returnList.add(r.location);
        }
        return returnList;
    }

    /**
     * Sense the 8 tiles around the robot.
     */
    public MapData[] senseSurroundingSquares() {
        return senseSurroundingSquares(controller.getLocation());
    }

    public MapData[] senseSurroundingSquares(MapLocation location) {
        MapData[] ret = new MapData[9];
        int x = location.getX(), y = location.getY();

        ret[0] = senseTile(new MapLocation(x - 1, y - 1));
        ret[4] = senseTile(new MapLocation(x, y - 1));
        ret[1] = senseTile(new MapLocation(x + 1, y - 1));

        ret[5] = senseTile(new MapLocation(x - 1, y));
        ret[8] = senseTile(new MapLocation(x, y));
        ret[6] = senseTile(new MapLocation(x + 1, y));

        ret[2] = senseTile(new MapLocation(x - 1, y + 1));
        ret[7] = senseTile(new MapLocation(x, y + 1));
        ret[3] = senseTile(new MapLocation(x + 1, y + 1));

        return ret;
    }

    /**
     * Sense the terrain and block height of a tile, and whether the tile has a robot or flux deposit on it.
     * If the location is offmap, a MapData object will still be returned, but it will not be saved in the
     * mapstore object.  Walls will be automatically updated to reflect the new locations.
     */
    public MapData senseTile(MapLocation location) {
        TerrainTile tile = null;

        if(!controller.canSenseSquare(location)) {
            return null;
        }

        try {
            tile = controller.senseTerrainTile(location);
        } catch(Exception e) {
            System.out.println("----Caught exception in senseTile1 tile: " + location.toString() + " Exception: " + e.toString());
        }

        // if the tile is off map, we do not want to store it in the database, cuz it will cause problems
        if(tile == null || tile.getType() == TerrainTile.TerrainType.OFF_MAP) {
            MapData data = new MapData(location);
            data.tile = tile;
            updateWalls(data);
            return data;
        }

        //grab the tile from the map store or create it if it doesn't exist because this tile is on the map
        try {
            MapData data = map.getOrCreate(location.getX(), location.getY());

            if(data.lastUpdate >= Clock.getRoundNum()) {
                return data;
            }

            boolean updateWalls = data.tile == null;
            int terrainHeight = tile.getHeight();
            int blockHeight = controller.senseNumBlocksAtLocation(location);

            data.tile = tile;
            if(terrainHeight != data.terrainHeight || blockHeight != data.blockHeight) {
                //the block has changed so update the heights and wall locations
                data.terrainHeight = terrainHeight;
                data.blockHeight = blockHeight;
                data.height = terrainHeight + blockHeight;
            }

            data.airRobot = controller.senseAirRobotAtLocation(location);
            data.groundRobot = controller.senseGroundRobotAtLocation(location);

            if(!data.isFluxDeposit) {
                data.deposit = controller.senseFluxDepositAtLocation(location);
                if(data.deposit != null) {
                    data.isFluxDeposit = true;
                }
            }

            data.airRobotInfo = null;
            data.groundRobotInfo = null;
            data.depositInfo = null;

            if(data.airRobot != null) {
                data.airRobotInfo = controller.senseRobotInfo(data.airRobot);
            }
            if(data.groundRobot != null) {
                data.groundRobotInfo = controller.senseRobotInfo(data.groundRobot);
            }
            if(data.isFluxDeposit) {
                data.depositInfo = controller.senseFluxDepositInfo(data.deposit);
            }

            if(updateWalls) {
                updateWalls(data);
            }

            data.lastUpdate = Clock.getRoundNum();
            return data;
        } catch(Exception e) {
            System.out.println("----Caught exception in senseTile2 tile: " + location.toString() + " Exception: " + e.toString());
        }
        return null;
    }

    /**
     * Calls senseTile on each of the given tiles that are not null and then calls the callback
     * function tileSensedCallback for each of the resulting MapData objects.
     */
    public void senseTiles(MapLocation[][] tiles) {
        for(MapLocation[] row : tiles) {
            for(MapLocation tile : row) {
                if(tile != null) {
                    if(!runSensorCallbacks(senseTile(tile))) {
                        return;
                    }
                }
            }
        }
    }

    /**
     * Updates the wall and wallBounds location with the new data.
     *
     * The walls represent the first location that is off map, not the last location this is on
     * the map.
     *
     * The bounds variables represent the last location that we have seen on the map.  This
     * preveents two walls from changing when only one is off map.  For instance, if the player
     * is at the bottom of the map and senses a tile below the map, without the bounds variables
     * it would change both the right and botto walls.  With the bounds variables, it recognizes
     * that there can't be a wall there because it already saw an on map tile further to the
     * right of it.
     */
    public void updateWalls(MapData data) {
        if(data.tile == null) {
            return;
        }

        if(data.tile.getType() == TerrainTile.TerrainType.OFF_MAP) {
            if(data.x > player.rightWallBounds && data.x < player.rightWall) {
                player.rightWall = data.x;
            }
            if(data.x < player.leftWallBounds && data.x > player.leftWall) {
                player.leftWall = data.x;
            }

            if(data.y < player.topWallBounds && data.y > player.topWall) {
                player.topWall = data.y;
            }
            if(data.y > player.bottomWallBounds && data.y < player.bottomWall) {
                player.bottomWall = data.y;
            }
        } else if(data.tile.getType() == TerrainTile.TerrainType.LAND) {
            if(data.x > player.rightWallBounds) {
                player.rightWallBounds = data.x;
                if(player.rightWall <= player.rightWallBounds) {
                    player.rightWall = player.rightWallBounds + 1;
                }
            }
            if(data.x < player.leftWallBounds) {
                player.leftWallBounds = data.x;
                if(player.leftWall >= player.leftWallBounds) {
                    player.leftWall = player.leftWallBounds - 1;
                }
            }

            if(data.y > player.bottomWallBounds) {
                player.bottomWallBounds = data.y;
                if(player.bottomWall <= player.bottomWallBounds) {
                    player.bottomWall = player.bottomWallBounds + 1;
                }
            }
            if(data.y < player.topWallBounds) {
                player.topWallBounds = data.y;
                if(player.topWall >= player.topWallBounds) {
                    player.topWall = player.topWallBounds - 1;
                }
            }
        }
    }

    class RobotCache {

        public int airSensed = Integer.MIN_VALUE, groundSensed = Integer.MIN_VALUE,
                airInfoSensed = Integer.MIN_VALUE, groundInfoSensed = Integer.MIN_VALUE;
        public Robot[] air, ground;
        public ArrayList<RobotInfo> airInfo, groundInfo;
        public int oldDataTolerance = 1;

        public RobotCache() {
        }

        public Robot[] getAirRobots() {
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

        public Robot[] getGroundRobots() {
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

        public ArrayList<RobotInfo> getAirRobotInfo() {
            if(airInfoSensed >= Clock.getRoundNum() - oldDataTolerance) {
                return airInfo;
            }

            getAirRobots();
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

        public ArrayList<RobotInfo> getGroundRobotInfo() {
            if(groundInfoSensed >= Clock.getRoundNum() - oldDataTolerance) {
                return groundInfo;
            }

            getGroundRobots();
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
}
