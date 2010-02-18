package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class FluxinFlux extends Base {

    public MapStore map;
    public MexicanMessaging messaging;
    public NaughtyNavigation navigation;
    public SensationalSensing sensing;
    public EnergeticEnergon energon;
    public double towerFluxTransferMax = 200;

    public FluxinFlux(NovaPlayer player) {
        super(player);
        map = player.map;
        messaging = player.messaging;
        navigation = player.navigation;
        sensing = player.sensing;
        energon = player.energon;
    }

    public void transferFluxBetweenArchons() {
        double flux = controller.getFlux();
        try {
            if(flux > 0) {
                MapLocation[] locations = sensing.senseArchonLocations();
                for(MapLocation location : locations) {
                    int distance = location.distanceSquaredTo(controller.getLocation());
                    if(distance > 0 && distance < 3) {
                        Robot robot = controller.senseAirRobotAtLocation(location);
                        if(robot == null) continue;
                        RobotInfo info = controller.senseRobotInfo(robot);
                        if(info.flux > flux) {
                            controller.transferFlux(flux, location, RobotLevel.IN_AIR);
                            break;
                        }
                    }
                }
            }
        } catch(Exception e) {
            pa("----Caught exception in transferFluxBetweenArchons " + e);
        }
    }

    public void transferFlux(MapLocation location) {
        try {
            Robot robot = player.controller.senseAirRobotAtLocation(location);
            if(robot == null) {
                location = sensing.senseClosestArchon();
                if(location.distanceSquaredTo(player.controller.getLocation()) > 2) {
                    return;
                }
            }

            if(location.distanceSquaredTo(controller.getLocation()) > 2) {
                return;
            }
            if(controller.canSenseSquare(location) && controller.senseAirRobotAtLocation(location) == null) {
                return;
            }

            double amount = player.controller.getFlux();
            player.controller.transferFlux(amount, location, RobotLevel.IN_AIR);
        } catch(Exception e) {
            pa("---Caught exception in transferFlux " + e);
        }
    }

    public void fluxUpWout(MapLocation woutLocation) {
        Robot robot = null;
        RobotInfo info = null;
        try {
            robot = player.controller.senseGroundRobotAtLocation(woutLocation);
            if(robot == null) return;
            info = player.controller.senseRobotInfo(robot);
        } catch(Exception e) {
            pa("----Caught exception in fluxUpWout " + e);
            return;
        }
        if(robot == null) {
            return;
        } else {
            if(player.controller.getLocation().distanceSquaredTo(woutLocation) < 3) {
                try {
                    player.controller.transferFlux(3000, woutLocation, RobotLevel.ON_GROUND);
                    double transferamt = (RobotType.WOUT.maxEnergon() - info.energonLevel);
                    if(transferamt > player.controller.getEnergonLevel()) {
                        transferamt = player.controller.getEnergonLevel();
                    }
                    energon.transferEnergon(transferamt, woutLocation, false);
                } catch(Exception e) {
                    return;
                }
            }
        }

    }

    /**
     * Attempts to transfer flux to nearby wouts and towers.
     * If a tower is close that needs flux, the wout will attempt to go there.
     * It does that by returning the RobotInfo object of that tower.
     */
    public RobotInfo autoTransferFlux() {
        RobotInfo robot = null;
        ArrayList<RobotInfo> allied = sensing.senseAlliedRobotInfoInSensorRange();
        for(RobotInfo robotInfo : allied) {
            if(robotInfo.type.isBuilding()) {
                if(isFluxLow(robotInfo)) {
                    if(controller.getLocation().isAdjacentTo(robotInfo.location)) {
                        robot = robotInfo;
                        break;
                    }
                    return robotInfo;
                }
            } else if(robotInfo.type == RobotType.WOUT) {
                if(robotInfo.flux > controller.getFlux() && controller.getLocation().isAdjacentTo(robotInfo.location)) {
                    robot = robotInfo;
                }
            }
        }

        if(robot == null) {
            return null;
        }

        try {
            if(player.controller.getLocation().isAdjacentTo(robot.location)) {
                // Check to make sure we don't transfer more flux than the Wout has, and the tower can receive.
                double maxTransfer = 0;
                if(robot.type == RobotType.WOUT) {
                    maxTransfer = Math.min(controller.getFlux(), robot.type.maxFlux() - robot.flux);
                } else {
                    maxTransfer = Math.min(towerFluxTransferMax, Math.min(player.controller.getFlux(), robot.type.maxFlux() - robot.flux));
                }
                if(maxTransfer > 10) {
                    if(controller.senseGroundRobotAtLocation(robot.location) == null) {
                        return null;
                    }
                    controller.transferFlux(maxTransfer, robot.location, RobotLevel.ON_GROUND);
                    //System.out.println("Transferred " + maxTransfer + " flux to a "+robot.type+"...");
                }
            }
        } catch(Exception e) {
            pa("---Caught exception in transferFluxToTower " + e);
        }

        return null;
    }

    /**
     * Returns true if a tower needs to be fluxed up.
     */
    public boolean isFluxLow(RobotInfo info) {
        return info.flux < (towerFluxTransferMax / 2);
    }

    /**
     * Returns true if the flux level is > 300
     */
    public boolean isFluxFull() {
        int limit = Math.min(200 + (int) (player.turnsSinceEnemiesSeen * player.turnsSinceEnemiesSeen * 0.1), 2000);
        //p(limit+"");
        return controller.getFlux() > limit && controller.getFlux() < 3000;
    }
}
