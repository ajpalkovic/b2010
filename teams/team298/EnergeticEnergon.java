package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class EnergeticEnergon extends Base {

    public MapStore map;
    public MexicanMessaging messaging;
    public NaughtyNavigation navigation;
    public SensationalSensing sensing;
    public ArrayList<EnergonTransferRequest> requests;
    public double lowEnergonLevel, sortaLowEnergonLevel;

    public EnergeticEnergon(NovaPlayer player) {
        super(player);
        map = player.map;
        messaging = player.messaging;
        navigation = player.navigation;
        sensing = player.sensing;

        requests = new ArrayList<EnergonTransferRequest>();
        lowEnergonLevel = controller.getRobotType().maxEnergon() * .3;
        sortaLowEnergonLevel = controller.getRobotType().maxEnergon() * .5;
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
                        RobotInfo info = controller.senseRobotInfo(robot);
                        if(info.flux > flux) {
                            controller.transferFlux(flux, location, RobotLevel.IN_AIR);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            pa("----Caught exception in transferFluxBetweenArchons");
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

            if(location.distanceSquaredTo(controller.getLocation()) > 2) return;
            if(controller.canSenseSquare(location) && controller.senseAirRobotAtLocation(location) == null) return;
            
            double amount = player.controller.getFlux();
            player.controller.transferFlux(amount, location, RobotLevel.IN_AIR);
        } catch (Exception e) {
            pa("---Caught exception in transferFlux");
            e.printStackTrace();
        }
    }
    public void fluxUpWout(MapLocation woutLocation) {
    	
    	Robot robot;
        RobotInfo info;
    		try{
    			robot = player.controller.senseGroundRobotAtLocation(woutLocation);
    			info = player.controller.senseRobotInfo(robot);
    		} catch (Exception e) {return;}
    	if (robot == null) {
    		return;
    	} else { 
    		if (player.controller.getLocation().distanceSquaredTo(woutLocation) < 3){
    			try{
    			player.controller.transferFlux(3000, woutLocation, RobotLevel.ON_GROUND);
    			double transferamt = (RobotType.WOUT.maxEnergon() - info.energonLevel);
    			if (transferamt > player.controller.getEnergonLevel())
    				transferamt = player.controller.getEnergonLevel();
    			transferEnergon(transferamt, woutLocation, false);
    			} catch (Exception e) {return;}
    		}
    	}
    		
    }
	public void transferFluxToTower(MapLocation towerLocation) {
        try {
            Robot robot = player.controller.senseGroundRobotAtLocation(towerLocation);
            RobotInfo info = player.controller.senseRobotInfo(robot);
            if(robot == null) {
            	return;
            } else {
           		if (player.controller.getLocation().isAdjacentTo(towerLocation)) {
           			// Check to make sure we don't transfer more flux than the Wout has, and the tower can receive.
           			double maxTransfer = info.flux;
           			double available = player.controller.getFlux();
           			if (maxTransfer > available) {
           				maxTransfer = available;
           			}
           			if (maxTransfer > 10) {
           				player.controller.transferFlux(maxTransfer, towerLocation, RobotLevel.ON_GROUND);
           				//System.out.println("Transferred " + maxTransfer + " flux to a tower...");
           			}
           		} else {
           			// Wout was not adjacent to the tower, better luck next time.
           			return;
           		}
            }
        } catch (Exception e) {
            pa("---Caught exception in transferFluxToTower");
            e.printStackTrace();
        }    	
    }
    
    public void addRequest(MapLocation location, boolean isAirUnit, int amount) {
        if(location.distanceSquaredTo(controller.getLocation()) > 2) return;
        requests.add(new EnergonTransferRequest(location, isAirUnit, amount));
    }

    /**
     * Auto energon transfers
     */
    public void autoTransferEnergon() {
        ArrayList<RobotInfo> robots = sensing.senseAlliedRobotInfoInSensorRange();
        for(RobotInfo robot : robots) {
            if(robot.type.isBuilding()) continue;
            if(robot.location.distanceSquaredTo(controller.getLocation()) > 2) continue;
            int amount = calculateEnergonRequestAmount(robot);
            if(amount >= 1) {
                requests.add(new EnergonTransferRequest(robot.location, false, amount));
                //p("adding request: "+requests.get(requests.size()-1).toString());
            }
        }
    }

    /**
     * Auto energon transfers between units
     */
    public void autoTransferEnergonBetweenUnits() {
        if(player.isTower ||controller.getEnergonLevel() < controller.getRobotType().maxEnergon() / 2) {
            requests.clear();
            return;
        }

        if(player.isWout) {
            requests.clear();
        }
        processEnergonTransferRequests();
    }

    /**
     * Calculates the amount of energon needed to fill this robot so that neither the
     * energon reserve nor energon level overflows.  Returns -1 if less than 1 energon
     * is needed.
     */
    public int calculateEnergonRequestAmount(double currentLevel, double currentReserve, double maxLevel) {
        double maxReserve = GameConstants.ENERGON_RESERVE_SIZE;
        double eventualLevel = currentLevel + currentReserve;

        double transferAmount = maxReserve - currentReserve;
        eventualLevel += transferAmount;
        if(eventualLevel >= maxLevel) {
            transferAmount -= (eventualLevel - maxLevel);
        }

        int requestAmount = (int) Math.round(transferAmount);
        if(requestAmount <= 1) {
            return -1;
        }

        return requestAmount;
    }

    public int calculateEnergonRequestAmount() {
        return calculateEnergonRequestAmount(controller.getEnergonLevel(), controller.getEnergonReserve(), controller.getMaxEnergonLevel());
    }

    public int calculateEnergonRequestAmount(RobotInfo info) {
        return calculateEnergonRequestAmount(info.energonLevel, info.energonReserve, info.maxEnergon);
    }

    /**
     * Returns true if the energon level is 90% of max
     */
    public boolean isEnergonFull() {
        return controller.getEnergonLevel() + controller.getEnergonReserve() > controller.getRobotType().maxEnergon() * .9;
    }

    /**
     * Returns true if the flux level is > 300
     */
    public boolean isFluxFull() {
        int limit = Math.min(200+(int)(player.turnsSinceEnemiesSeen*player.turnsSinceEnemiesSeen*0.1), 2000);
        //p(limit+"");
        return controller.getFlux() > limit && controller.getFlux() < 3000;
    }

    /**
     * Returns true if the energon level plus energon reserve is less than the starting energon level
     */
    public boolean isEnergonLow() {
        double currentLevel = controller.getEnergonLevel(), currentReserve = controller.getEnergonReserve();
        return (currentReserve < 3 && currentLevel < lowEnergonLevel);
    }

    /**
     * Returns true if the energon level plus energon reserve is less than the starting energon level
     */
    public boolean isEnergonSortaLow() {
        double currentLevel = controller.getEnergonLevel(), currentReserve = controller.getEnergonReserve();
        return (currentReserve < 3 && currentLevel < sortaLowEnergonLevel);
    }

    /**
     * Processes all of the energon requests in the requests ArrayList.
     * If multiple requests were asked for, the method will only give each robot a percentage
     * of the requested energon so the archon is not killed.
     */
    public void processEnergonTransferRequests() {
        double amount = controller.getEnergonLevel();
        double maxToGive = amount - 10;
        if(maxToGive < 0) {
            requests.clear();
            return;
        }

        autoTransferEnergon();

        //p("processing requests");
        if(requests.size() > 0) {
            //p("multiple requests");
            double sum = 0;
            for(EnergonTransferRequest request : requests) {
                sum += request.amount;
            }

            double percent = 1;
            if(maxToGive < sum) {
                percent = maxToGive / sum;
            }

            //pr("sum: "+sum+" percent: "+percent+" amount: "+amount);
            for(EnergonTransferRequest request : requests) {
                //pr(request.amount+"  "+(request.amount * percent)+"  "+request.location+"  "+request.isAirUnit);
                int result = transferEnergon(request.amount * percent, request.location, request.isAirUnit);
            }

            requests.clear();
        }
    }

    /**
     * Performs all the steps to request an energon transfer.
     * First, it calculates an amount of energon to request, enough so that the neither
     * the reserve not level overflows.
     *
     * The robot then attempts to move adjacent to the closest archon.  However, if the
     * archon moves while the robot is moving, the robot will try 2 more times to get adjacent
     * to the robot.
     */
    public int requestEnergonTransfer() {
        int amount = calculateEnergonRequestAmount();
        if(amount == -1) {
            return Status.success;
        }

        int tries = 3;
        navigation.changeToArchonGoal(false);
        do {
            navigation.moveOnce(true);
            tries--;
        } while(tries > 0 && !navigation.goal.done());

        messaging.sendLowEnergon(calculateEnergonRequestAmount());
        controller.yield();

        navigation.popGoal();
        return Status.success;
    }

    /**
     * Transfers the specified amount of energon to the robot as long as the robot is adjacent to the
     * archon, and the amount of energon requested will not reduce the archon's energon level to
     * just 1 energon.
     */
    public int transferEnergon(double amount, MapLocation location, boolean isAirUnit) {
        if(location.distanceSquaredTo(controller.getLocation()) > 3) return Status.fail;
        if(controller.getEnergonLevel() - 1 < amount) {
            return Status.notEnoughEnergon;
        }

        if(amount < 0.1) {
            return Status.success;
        }

        //System.out.println("in transfer");
        /*if(!location.isAdjacentTo(controller.getLocation()))
        return Status.fail;*/

        RobotLevel level = isAirUnit ? RobotLevel.IN_AIR : RobotLevel.ON_GROUND;
        try {
            if(controller.canSenseSquare(location) && ((isAirUnit && controller.senseAirRobotAtLocation(location) == null) ||
                    (!isAirUnit && controller.senseGroundRobotAtLocation(location) == null))) {
                return Status.fail;
            }
            controller.transferUnitEnergon(amount, location, level);
        } catch(Exception e) {
            System.out.println("----Caught Exception in transferEnergon. amount: "+amount+
                    " location: "+location.toString()+" isAirUnit: "+isAirUnit+" level: "+
                    level.toString()+" Exception: "+e.toString());
            return Status.fail;
        }
        return Status.success;
    }
    
    /**
     * Represents a pending energon request.
     */
    class EnergonTransferRequest {

        public MapLocation location, archonLocation;
        public boolean isAirUnit;
        public double amount;

        public EnergonTransferRequest(MapLocation location, boolean isAirUnit, int amount) {
            this.location = location;
            this.isAirUnit = isAirUnit;
            this.amount = amount;
        }

        public EnergonTransferRequest(int amount, boolean isAirUnit) {
            this.isAirUnit = isAirUnit;
            this.amount = amount;
        }

        public String toString() {
            return "Location: " + location.toString() + " isAirUnit: " + isAirUnit + " Amount: " + amount;
        }
    }
}
