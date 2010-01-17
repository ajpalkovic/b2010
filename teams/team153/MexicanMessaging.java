package team153;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class MexicanMessaging extends Base {

    public final int KEY1 = 439874345;
    public final int KEY2 = 198465730;
    public ArrayList<Integer> messageInts = new ArrayList<Integer>();
    public ArrayList<String> messageStrings = new ArrayList<String>();
    public ArrayList<MapLocation> messageLocations = new ArrayList<MapLocation>();
    public SensationalSensing sensing;

    public MexicanMessaging(NovaPlayer player) {
        super(player);
    }

    /**
     * Each of these methods represent the public interface for sending messages.
     * All they have to do is call addMessage and pass it some data.
     * The first two parameters are the message type (from BroadcastMessage.*) and the recipient id (BroadcastMessage.everyone for everyone)
     * Add message accepts an array of ints, strings, and MapLocations which will be sent along with the message.
     */
    public boolean sendMove(MapLocation location) {
        return addMessage(BroadcastMessage.move, BroadcastMessage.everyone, null, null, new MapLocation[] {location});
    }

    public boolean sendLowEnergon(MapLocation archonLocation, int amount) {
        int[] ints = new int[] {amount, player.isAirRobot ? 1 : 0};
        MapLocation[] locations = new MapLocation[] {controller.getLocation(), archonLocation};
        return addMessage(BroadcastMessage.lowEnergon, BroadcastMessage.everyone, ints, null, locations);
    }

    public boolean sendEnemyInSight(MapLocation location, int[] data, String robotType) {
        int[] ints = new int[] {data[0]};
        MapLocation[] locations = new MapLocation[] {controller.getLocation(), location};
        String[] strings = {robotType};
        return addMessage(BroadcastMessage.enemyInSight, BroadcastMessage.everyone, ints, strings, locations);
    }

    public boolean sendNewUnit() {
        MapLocation[] locations = new MapLocation[] {controller.getLocation()};
        String[] strings = {controller.getRobotType().toString()};
        return addMessage(BroadcastMessage.newUnit, BroadcastMessage.everyone, null, strings, locations);
    }

    public boolean sendFollowRequest(MapLocation archonLocation, int recipientRobotId) {
        MapLocation[] locations = new MapLocation[] {archonLocation};
        return addMessage(BroadcastMessage.followRequest, recipientRobotId, null, null, locations);
    }


    /**
     * This method sends enemyInSight message broadcast for all enemies in sight
     */
    public void sendMessageForEnemyRobots() {
        ArrayList<RobotInfo> enemies = sensing.senseEnemyRobotInfoInSensorRange();
        for(RobotInfo robot : enemies) {
            int[] data = {(int) robot.energonLevel, -1};
            String robotType = robot.type.toString();

            // this is really inefficient, we should fix it
            // it has to recopy the data from the arraylists to the arrays every time
            sendEnemyInSight(robot.location, data, robotType);
        }
        if(enemies.size() > 0) {
            sendMessage();
            player.enemyInSight(enemies);
        }
    }


    /**
     * This method processes all of the messages that the robot has.
     */
    public void parseMessages() {
        Message[] messages = controller.getAllMessages();
        for(Message message : messages) {
            processMessage(message);
        }
    }


    /**
     * This is the core method for processing a message.
     * Every message must update this method with two switch cases.
     *
     * A message consists of a series of ints, strings, and locations.
     * The first three ints of a message will always be our two keys and the sender's id.
     * Those are the same from all messages sent from a robot.
     *
     * Inside of a Message object, we can actually send multiple "messages", by just adding a second message
     * to the end of the ints/strings/locations.
     * So, the role of this method is to process each message within the Message object.
     *
     * Any message can have two intended recipients: everyone, or one specific robot.
     *
     * Regardless of for whom the message is intended, this method must update the index in the array of the ints, strings, and locations.
     * Even if the message is not intended for us, the data will still be there, so the indexes need to be updated.
     * Updating the indexes ensures that for the next message, the indexes will point to the start of its data.
     * Essentially, the amount each index needs to be updated is really just the same as the size of each of the arrays in the send method for that message.
     *
     * Finally, if the message is intended for us, it should call a callback function in NovaPlayer to process the message.
     * Putting it there allows us to override it later.
     * All this method should do is copy the data from the arrays into the callback function parameters.
     *
     **********************************************/
    private void processMessage(Message message) {
        if(message != null) {
            //is it ours?
            if(message.ints == null || message.ints.length < 3 || message.ints[0] != KEY1 || message.ints[1] != KEY2) {
                return;
            }

            int senderID = message.ints[2];
            int locationIndex = 0;
            int stringIndex = 0;
            int messageId = -1;
            int recipientId = -1;
            int myId = controller.getRobot().getID();

            for(int intIndex = 3; intIndex < message.ints.length; ) {
                messageId = message.ints[intIndex];
                recipientId = message.ints[intIndex + 1];

                intIndex += 2;

                //If this was not my message, we still need to update the location and string arrays because they will contain the data from a message that is not mine.
                if(recipientId != BroadcastMessage.everyone && recipientId != myId) {
                    switch(messageId) {
                        case BroadcastMessage.enemyInSight:
                            locationIndex += 2;
                            intIndex++;
                            stringIndex++;
                            break;
                        case BroadcastMessage.newUnit:
                            break;
                        case BroadcastMessage.lowEnergon:
                            intIndex += 2;
                            locationIndex += 2;
                            break;
                        case BroadcastMessage.move:
                            locationIndex++;
                            break;
                        case BroadcastMessage.followRequest:
                            locationIndex++;
                            break;
                        case BroadcastMessage.support:
                            break;
                    }
                } else {
                    // this message is ours
                    switch(messageId) {
                        case BroadcastMessage.enemyInSight:
                            player.enemyInSight(message.locations[locationIndex + 1], message.ints[intIndex], message.strings[stringIndex]);
                            intIndex++;
                            locationIndex += 2;
                            stringIndex++;
                            break;
                        case BroadcastMessage.newUnit:
                            player.newUnit(senderID, message.locations[locationIndex], message.strings[stringIndex]);
                            break;
                        case BroadcastMessage.lowEnergon:
                            player.lowEnergonMessageCallback(message.locations[locationIndex], message.locations[locationIndex + 1], message.ints[intIndex + 1], message.ints[intIndex]);
                            intIndex += 2;
                            locationIndex += 2;
                            break;
                        case BroadcastMessage.move:
                            player.moveMessageCallback(message.locations[locationIndex]);
                            locationIndex++;
                            break;
                        case BroadcastMessage.followRequest:
                            player.followRequestMessageCallback(message.locations[locationIndex], senderID);
                            locationIndex++;
                            break;
                        case BroadcastMessage.support:
                            break;
                    }
                }
            }
        }
    }

    /**
     * Copies the data into the arraylists.
     * If no message exists, then any previous message must have been sent, so all the data id old and can be removed.
     */
    private boolean addMessage(int messageType, int recipientId, int[] ints, String[] strings, MapLocation[] locations) {
        if(!controller.hasBroadcastMessage()) {
            clearMessages();
        }

        int i = 0;
        messageInts.add(messageType);
        messageInts.add(recipientId);

        if(ints != null) {
            for(i = 0; i < ints.length; i++) {
                messageInts.add(ints[i]);
            }
        }
        if(strings != null) {
            for(i = 0; strings != null && i < strings.length; i++) {
                messageStrings.add(strings[i]);
            }
        }
        if(locations != null) {
            for(i = 0; i < locations.length; i++) {
                messageLocations.add(locations[i]);
            }
        }

        sendMessage();
        return true;
    }

    /**
     * This method copies all of the data from the arraylist of ints,strings,locations and puts them into a message object.
     * @return
     */
    private boolean sendMessage() {
        Message message = new Message();
        message.ints = new int[messageInts.size() + 3];
        message.ints[0] = KEY1;
        message.ints[1] = KEY2;
        message.ints[2] = robot.getID();
        boolean good = true;

        for(int i = 3; i < messageInts.size() + 3; i++) {
            message.ints[i] = messageInts.get(i - 3);
        }

        message.locations = new MapLocation[messageLocations.size()];
        for(int i = 0; i < message.locations.length; i++) {
            message.locations[i] = messageLocations.get(i);
        }

        message.strings = new String[messageStrings.size()];
        for(int i = 0; i < message.strings.length; i++) {
            message.strings[i] = messageStrings.get(i);
        }

        try {
            if(controller.hasBroadcastMessage()) {
                controller.clearBroadcast();
            }

            controller.broadcast(message);

            good = true;
        } catch(Exception e) {
            e.printStackTrace();
            pr("----Caught Exception in sendMessage.  message: " + message.toString() + " Exception: " + e.toString());
            good = false;
        }
        return good;
    }

    /*
     * send message methods
     */
    public void clearMessages() {
        messageInts.clear();
        messageStrings.clear();
        messageLocations.clear();
    }
}
