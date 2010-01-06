package team298;

import java.util.*;
import battlecode.common.*;

public class MapData {

    public int x, y, terrainHeight, blockHeight, height, pathCost = 0;
    public boolean isFluxDeposit;
    public int lastUpdate = -1;
    public MapLocation location = null;
    public FluxDeposit deposit = null;
    public FluxDepositInfo depositInfo = null;
    public Robot groundRobot = null, airRobot = null;
    public RobotInfo groundRobotInfo = null, airRobotInfo = null;
    public TerrainTile tile = null;

    public String toString() {
        return "(" + x + " " + y + ")";
    }

    public String toStringFull() {
        String ret = "(" + x + " " + y + ")";
        ret += " height: " + height;
        ret += " terrainHeight: " + terrainHeight;
        ret += " blockHeight: " + blockHeight;
        ret += " isFluxDeposit: " + isFluxDeposit;
        if(deposit != null) {
            ret += " deposit: " + deposit;
        }
        if(groundRobot != null) {
            ret += " groundRobot: " + groundRobot;
        }
        if(airRobot != null) {
            ret += " airRobot: " + airRobot;
        }
        if(tile != null) {
            ret += " tile: " + tile;
        }
        ret += " lastUpdate: " + lastUpdate;

        return ret;
    }

    public boolean equals(Object other) {
        MapData data = (MapData) other;
        return data.x == x && data.y == y;
    }

    public MapData(MapLocation location) {
        this.x = location.getX();
        this.y = location.getY();
        terrainHeight = -1;
        blockHeight = -1;
        height = -1;
        isFluxDeposit = false;
    }

    public MapData(int x, int y) {
        this.x = x;
        this.y = y;
        terrainHeight = -1;
        blockHeight = -1;
        height = -1;
        isFluxDeposit = false;
    }

    public MapData(int x, int y, int height) {
        this.x = x;
        this.y = y;
        this.height = height;
        terrainHeight = -1;
        blockHeight = -1;
        isFluxDeposit = false;
    }

    public MapData(int x, int y, int terrainHeight, int blockHeight, boolean isFluxDeposit) {
        this.x = x;
        this.y = y;
        this.terrainHeight = terrainHeight;
        this.blockHeight = blockHeight;
        this.isFluxDeposit = isFluxDeposit;
        height = terrainHeight + blockHeight;
    }

    public MapLocation toMapLocation() {
        if(location == null) {
            location = new MapLocation(x, y);
        }
        return location;
    }
}
