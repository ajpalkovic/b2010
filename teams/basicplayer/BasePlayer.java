package basicplayer;

import basicplayer.goals.Goal;
import basicplayer.message.MessageHandler;
import basicplayer.message.MessageSender;
import basicplayer.navigation.BugNavigation;
import basicplayer.navigation.Navigation;
import basicplayer.navigation.QueuedAction;
import basicplayer.util.FastList;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Message;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TerrainTile;
import battlecode.common.TerrainTile.TerrainType;
import java.io.PrintStream;

public abstract class BasePlayer
{
  public RobotController myRC;
  public Navigation myNav;
  public QueuedAction queued;
  public boolean moving = false;
  public boolean attacking = false;
  public static final double SACRIFICE_SELF_FOR_ARCHON = 10.0D;
  public static final double MAX_RESERVE = 2.0D;
  public static final double ARCHON_MAX_RESERVE = 5.0D;
  public static final double ARCHON_DEAD_ENERGON = -2.0D;
  public static final double SOLDIER_DEAD_ENERGON = -1.0D + RobotType.SOLDIER.energonUpkeep();
  public static final double TURRET_DEAD_ENERGON = -1.0D + RobotType.TURRET.energonUpkeep();
  public static final double WOUT_DEAD_ENERGON = -1.0D + RobotType.WOUT.energonUpkeep();
  public static final double CHAINER_DEAD_ENERGON = -1.0D + RobotType.CHAINER.energonUpkeep();
  public static final double TELEPORTER_DEAD_ENERGON = -1.0D + RobotType.TELEPORTER.energonUpkeep();
  public static final int ENEMY_PURSUE_TIME = 75;
  public static final int[] dx = { 0, 1, 1, 1, 0, -1, -1, -1, 0, 0 };
  public static final int[] dy = { -1, -1, 0, 1, 1, 1, 0, -1, 0, 0 };

  public static final Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST, Direction.NONE };

  public static final RobotType[] robotTypes = { RobotType.ARCHON, RobotType.WOUT, RobotType.CHAINER, RobotType.SOLDIER, RobotType.TURRET };

  public FastList alliedArchons = new FastList(8);
  public FastList alliedWouts = new FastList(113);
  public FastList alliedChainers = new FastList(113);
  public FastList alliedSoldiers = new FastList(113);
  public FastList alliedTurrets = new FastList(113);
  public FastList alliedTeleporters = new FastList(113);
  public FastList alliedComms = new FastList(113);
  public FastList alliedAuras = new FastList(113);

  public RobotInfo[] alliedArchonInfos = this.alliedArchons.robotInfos;
  public RobotInfo[] alliedWoutInfos = this.alliedWouts.robotInfos;
  public RobotInfo[] alliedChainerInfos = this.alliedChainers.robotInfos;
  public RobotInfo[] alliedSoldierInfos = this.alliedSoldiers.robotInfos;
  public RobotInfo[] alliedTurretInfos = this.alliedTurrets.robotInfos;
  public RobotInfo[] alliedTeleporterInfos = this.alliedTeleporters.robotInfos;
  public RobotInfo[] alliedCommInfos = this.alliedComms.robotInfos;
  public RobotInfo[] alliedAuraInfos = this.alliedAuras.robotInfos;

  public Robot[] alliedArchonRobots = this.alliedArchons.robots;
  public Robot[] alliedWoutRobots = this.alliedWouts.robots;
  public Robot[] alliedChainerRobots = this.alliedChainers.robots;
  public Robot[] alliedSoldierRobots = this.alliedSoldiers.robots;
  public Robot[] alliedTurretRobots = this.alliedTurrets.robots;
  public Robot[] alliedTeleporterRobots = this.alliedTeleporters.robots;
  public Robot[] alliedCommRobots = this.alliedComms.robots;
  public Robot[] alliedAuraRobots = this.alliedAuras.robots;

  public FastList[] alliedUnits = { this.alliedArchons, this.alliedWouts, this.alliedChainers, this.alliedSoldiers, this.alliedTurrets, this.alliedComms, this.alliedTeleporters, this.alliedAuras };

  public FastList enemyArchons = new FastList(8);
  public FastList enemyWouts = new FastList(113);
  public FastList enemyChainers = new FastList(113);
  public FastList enemySoldiers = new FastList(113);
  public FastList enemyTurrets = new FastList(113);
  public FastList enemyTeleporters = new FastList(113);
  public FastList enemyComms = new FastList(113);
  public FastList enemyAuras = new FastList(113);

  public RobotInfo[] enemyArchonInfos = this.enemyArchons.robotInfos;
  public RobotInfo[] enemyWoutInfos = this.enemyWouts.robotInfos;
  public RobotInfo[] enemyChainerInfos = this.enemyChainers.robotInfos;
  public RobotInfo[] enemySoldierInfos = this.enemySoldiers.robotInfos;
  public RobotInfo[] enemyTurretInfos = this.enemyTurrets.robotInfos;
  public RobotInfo[] enemyTeleporterInfos = this.enemyTeleporters.robotInfos;
  public RobotInfo[] enemyCommInfos = this.enemyComms.robotInfos;
  public RobotInfo[] enemyAuraInfos = this.enemyAuras.robotInfos;

  public Robot[] enemyArchonRobots = this.enemyArchons.robots;
  public Robot[] enemyWoutRobots = this.enemyWouts.robots;
  public Robot[] enemyChainerRobots = this.enemyChainers.robots;
  public Robot[] enemySoldierRobots = this.enemySoldiers.robots;
  public Robot[] enemyTurretRobots = this.enemyTurrets.robots;
  public Robot[] enemyTeleporterRobots = this.enemyTeleporters.robots;
  public Robot[] enemyCommRobots = this.enemyComms.robots;
  public Robot[] enemyAuraRobots = this.enemyAuras.robots;

  public FastList[] enemyUnits = { this.enemyArchons, this.enemyWouts, this.enemyChainers, this.enemySoldiers, this.enemyTurrets, this.enemyComms, this.enemyTeleporters, this.enemyAuras };
  public FastList[][] allUnits;
  public Goal[] movementGoals;
  public Goal[] broadcastGoals;
  public Goal lastGoal;
  public Team myTeam;
  public RobotType myType;
  public Robot myRobot;
  public int myID;
  public int myIDMod1024;
  public MessageSender mySender;
  public MessageHandler[] handlers = new MessageHandler[117];
  public MapLocation myLoc;
  public int lastKnownEnemyTime = -10000;
  int timer;
  public static final int AT_WAR_TIME = 75;
  public boolean atWar;

  public BasePlayer(RobotController RC)
  {
    this.myRC = RC;
    this.myNav = new BugNavigation(this);
    this.myTeam = this.myRC.getTeam();
    this.myType = this.myRC.getRobotType();
    this.myRobot = this.myRC.getRobot();
    this.myID = this.myRobot.getID();
    if (this.myTeam == Team.A) {
      this.allUnits = new FastList[][] { this.alliedUnits, this.enemyUnits };
    }
    else {
      this.allUnits = new FastList[][] { this.enemyUnits, this.alliedUnits };
    }
    this.mySender = new MessageSender(this);
  }

  public abstract void run();

  public void checkIfActive()
  {
    this.attacking = this.myRC.isAttackActive();
    this.moving = this.myRC.isMovementActive();
  }

  public static void debug_stackTrace(Exception e)
  {
    //System.out.println("CAUGHT EXCEPTION:");
    //e.printStackTrace();
  }

  public static void debug_println(String s) {
    //System.out.println(s);
  }

  public static void debug_printObject(Object o) {
    //System.out.println(o.toString());
  }

  public static void debug_printInt(int i) {
    //System.out.println(Integer.toString(i));
  }

  public void debug_startTiming() {
    this.timer = (6000 * Clock.getRoundNum() + Clock.getBytecodeNum());
  }

  public void debug_stopTiming() {
    //System.out.println(6000 * Clock.getRoundNum() + Clock.getBytecodeNum() - this.timer);
  }

  public void debug_setIndicatorString(int n, String s) {
    this.myRC.setIndicatorString(n, s);
  }

  public void debug_setIndicatorStringObject(int n, Object o) {
    if (o != null)
      this.myRC.setIndicatorString(n, o.toString());
    else
      this.myRC.setIndicatorString(n, null);
  }

  public static MapLocation multipleAddDirection(MapLocation orig, Direction dir, int n) {
    return new MapLocation(orig.getX() + n * dx[dir.ordinal()], orig.getY() + n * dy[dir.ordinal()]);
  }

  public void setQueued(QueuedAction a) {
    if (this.queued != null) {
      debug_println("Warning: action already queued");
    }
    this.queued = a;
  }

  public boolean canSpawnGround(Direction d) throws GameActionException {
    MapLocation loc = this.myRC.getLocation().add(d);
    return ((this.myRC.senseTerrainTile(loc).getType() == TerrainTile.TerrainType.LAND) && (this.myRC.senseGroundRobotAtLocation(loc) == null));
  }

  public boolean enoughEnergonToTransform(RobotType type) {
    return (this.myRC.getEnergonLevel() >= type.spawnCost() / 2.0D + 5.0D * type.energonUpkeep());
  }

  public void senseNearbyRobots() {
    this.alliedArchons.size = 0;
    this.alliedWouts.size = 0;
    this.alliedChainers.size = 0;
    this.alliedSoldiers.size = 0;
    this.alliedTurrets.size = 0;
    this.alliedComms.size = 0;
    this.alliedTeleporters.size = 0;
    this.alliedAuras.size = 0;
    this.enemyArchons.size = 0;
    this.enemyWouts.size = 0;
    this.enemyChainers.size = 0;
    this.enemySoldiers.size = 0;
    this.enemyTurrets.size = 0;
    this.enemyComms.size = 0;
    this.enemyTeleporters.size = 0;
    this.enemyAuras.size = 0;

    FastList[][] allUnits = this.allUnits;
    RobotController myRC = this.myRC;
    try {
      Robot[] robots = myRC.senseNearbyAirRobots();
      RobotInfo info;
      FastList fl;
      for (int i = robots.length - 1; i >= 0; --i) {
        info = myRC.senseRobotInfo(robots[i]);
        fl = allUnits[info.team.ordinal()][info.type.ordinal()];
        fl.robots[fl.size] = robots[i];
        fl.robotInfos[(fl.size++)] = info;
      }
      robots = myRC.senseNearbyGroundRobots();
      int i;
      for (i = robots.length - 1; i >= 0; --i) {
        info = myRC.senseRobotInfo(robots[i]);
        fl = allUnits[info.team.ordinal()][info.type.ordinal()];
        fl.robots[fl.size] = robots[i];
        fl.robotInfos[(fl.size++)] = info;
      }
    } catch (Exception e) {
      debug_stackTrace(e);
    }
  }

  public MapLocation nearestAlliedArchon() {
    int dmin = 99999;
    MapLocation[] archons = this.myRC.senseAlliedArchons();
    MapLocation best = null;

    for (int i = archons.length - 1; i >= 0; --i) {
      int d = this.myLoc.distanceSquaredTo(archons[i]);
      if (d < dmin) {
        best = archons[i];
        dmin = d;
      }
    }
    return best;
  }

  public RobotInfo nearestOneOf(FastList l) {
    int dmin = 99999;

    RobotInfo best = null;
    RobotInfo[] infos = l.robotInfos;
    for (int i = l.size - 1; i >= 0; --i) {
      int d = this.myLoc.distanceSquaredTo(infos[i].location);
      if (d < dmin) {
        best = infos[i];
        dmin = d;
      }
    }
    return best;
  }

  public MapLocation nearestAlliedArchonTo(MapLocation loc) {
    int dmin = 99999;
    MapLocation[] archons = this.myRC.senseAlliedArchons();
    MapLocation best = null;

    for (int i = archons.length - 1; i >= 0; --i) {
      int d = loc.distanceSquaredTo(archons[i]);
      if (d < dmin) {
        best = archons[i];
        dmin = d;
      }
    }
    return best;
  }

  public MapLocation nearestAlliedArchonAtLeastDist(int mind) {
    int dmin = 99999;
    MapLocation[] archons = this.myRC.senseAlliedArchons();
    MapLocation best = null;

    for (int i = archons.length - 1; i >= 0; --i) {
      int d = this.myLoc.distanceSquaredTo(archons[i]);
      if ((d >= mind) && (d < dmin)) {
        best = archons[i];
        dmin = d;
      }
    }
    return best;
  }

  public boolean isThereAnArchonWithin(int dist)
  {
    MapLocation[] archons = this.myRC.senseAlliedArchons();
    for (int i = archons.length - 1; i >= 0; --i) {
      if (this.myLoc.distanceSquaredTo(archons[i]) <= dist)
        return true;
    }
    return false;
  }

  public boolean isThereAnArchonNear(MapLocation loc, int dist)
  {
    MapLocation[] archons = this.myRC.senseAlliedArchons();
    for (int i = archons.length - 1; i >= 0; --i) {
      if (loc.distanceSquaredTo(archons[i]) <= dist)
        return true;
    }
    return false;
  }

  public static MapLocation awayFrom(MapLocation loc1, MapLocation loc2) {
    return new MapLocation(2 * loc1.getX() - loc2.getX(), 2 * loc1.getY() - loc2.getY());
  }

  public void tryBestGoal(Goal[] goals)
  {
    int best = 0;

    for (int i = goals.length - 1; i >= 0; --i) {
      Goal g = goals[i];
      if (g.getMaxPriority() <= best)
        break;
      int p = g.getPriority();
      if (p >= best) {
        best = p;
        this.lastGoal = g;
      }
    }
    if (best != 0)
      this.lastGoal.tryToAccomplish();
  }

  public void tryBestGoalNotSorted(Goal[] goals)
  {
    int best = 0;

    for (int i = goals.length - 1; i >= 0; --i) {
      Goal g = goals[i];
      if (g.getMaxPriority() <= best)
        continue;
      int p = g.getPriority();
      if (p >= best) {
        best = p;
        this.lastGoal = g;
      }
    }
    if (best != 0)
      this.lastGoal.tryToAccomplish();
  }

  public void sortMessages()
  {
    this.mySender.updateRoundNum();
    Message[] newMessages = this.myRC.getAllMessages();

    for (int i = 0; i < newMessages.length; ++i) {
      int[] ints = newMessages[i].ints;
      int type;
      if ((ints == null) || (ints.length < 3) || ((type = ints[0]) < 0) || (type >= 117) || (this.handlers[type] == null)) continue; if (!(this.mySender.isValid(newMessages[i])))
      {
        continue;
      }

      this.handlers[type].receivedMessage(newMessages[i]);
    }
  }

  public void transferEnergon()
  {
    double myEnergon = this.myRC.getEnergonLevel();
    try
    {
      RobotInfo info;
      double transferAmount;
      for (int i = this.alliedArchons.size - 1; i >= 0; --i) {
        info = this.alliedArchonInfos[i];
        if (this.myLoc.distanceSquaredTo(info.location) > 2) continue; if (info.energonLevel >= -3.0D)
          if (info.eventualEnergon < 41.0D) {
            transferAmount = 2.0D - info.energonReserve;
            if (transferAmount > 0.0D)
              if (transferAmount >= myEnergon) {
                if (info.energonLevel < 10.0D)
                  this.myRC.transferUnitEnergon(this.myRC.getEnergonLevel(), info.location, RobotLevel.IN_AIR);
              }
              else
              {
                this.myRC.transferUnitEnergon(transferAmount, info.location, RobotLevel.IN_AIR);
                myEnergon -= transferAmount;
              }
          }
      }
      if (myEnergon < 2.0D) return;
      int i;
      for (i = this.alliedAuras.size - 1; i >= 0; --i) {
        info = this.alliedAuraInfos[i];
        if (!(this.myLoc.isAdjacentTo(info.location))) continue; if (info.energonLevel >= TURRET_DEAD_ENERGON) {
          transferAmount = 2.0D - info.energonReserve;
          if (transferAmount > 0.0D)
            if (myEnergon > info.eventualEnergon + 2.0D) {
              this.myRC.transferUnitEnergon(transferAmount, info.location, RobotLevel.ON_GROUND);
              myEnergon -= transferAmount;
              if (myEnergon < 2.0D) return;
            }
        }
      }
      for (i = this.alliedTurrets.size - 1; i >= 0; --i) {
        info = this.alliedTurretInfos[i];
        if (!(this.myLoc.isAdjacentTo(info.location))) continue; if (info.energonLevel >= TURRET_DEAD_ENERGON) {
          transferAmount = 2.0D - info.energonReserve;
          if (transferAmount > 0.0D)
            if (myEnergon > info.eventualEnergon + 2.0D) {
              this.myRC.transferUnitEnergon(transferAmount, info.location, RobotLevel.ON_GROUND);
              myEnergon -= transferAmount;
              if (myEnergon < 2.0D) return;
            }
        }
      }
      for (i = this.alliedSoldiers.size - 1; i >= 0; --i) {
        info = this.alliedSoldierInfos[i];
        if (!(this.myLoc.isAdjacentTo(info.location))) continue; if (info.energonLevel >= SOLDIER_DEAD_ENERGON) {
          transferAmount = 2.0D - info.energonReserve;
          if (transferAmount > 0.0D)
            if (myEnergon > info.eventualEnergon + 2.0D) {
              this.myRC.transferUnitEnergon(transferAmount, info.location, RobotLevel.ON_GROUND);
              myEnergon -= transferAmount;
              if (myEnergon < 2.0D) return;
            }
        }
      }
      for (i = this.alliedChainers.size - 1; i >= 0; --i) {
        info = this.alliedChainerInfos[i];
        if (!(this.myLoc.isAdjacentTo(info.location))) continue; if (info.energonLevel >= CHAINER_DEAD_ENERGON) {
          transferAmount = 2.0D - info.energonReserve;
          if (transferAmount > 0.0D)
            if (myEnergon > info.eventualEnergon + 2.0D) {
              this.myRC.transferUnitEnergon(transferAmount, info.location, RobotLevel.ON_GROUND);
              myEnergon -= transferAmount;
              if (myEnergon < 2.0D) return;
            }
        }
      }
    } catch (Exception e) {
      debug_stackTrace(e);
    }
  }
}