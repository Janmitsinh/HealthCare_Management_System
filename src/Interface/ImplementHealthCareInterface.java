package Interface;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static Variables.DeclareVariable.*;
import java.io.IOException;
import Model.AppointmentDetailsModel;
import Model.UserModel;
import Log.Log;

public class ImplementHealthCareInterface extends UnicastRemoteObject implements HealthCareInterface {
    private String serverID;
    private String serverName;
    private static final int defaultPort = 1;
    // Below hashmap helps in managing and accessing the various data elements.
    private Map<String, Map<String, AppointmentDetailsModel>> everyAppointment;         // HashMap <AppointmentType, Map<appointmentID, AppointmentDetailsModel>>
    private Map<String, Map<String, List<String>>> usersAppointments;                   // HashMap <PatientID, Map<appointmentType, AppointmentIDs>>
    private Map<String, UserModel> serverUsers;                                         // HashMap <patientID, UserModel>
    private static final Map<String, Integer> cityPortMapping;                          // HashMap <serverName, serverPort>

    // Constructor
    public ImplementHealthCareInterface(String serverID, String serverName) throws RemoteException {
        super();
        this.serverID = serverID;
        this.serverName = serverName;
        everyAppointment = new ConcurrentHashMap<>(); // allows multiple threads to access and modify the map concurrently
        everyAppointment.put(APPOINTMENT_TYPE_PHYSICIAN, new ConcurrentHashMap<>());
        everyAppointment.put(APPOINTMENT_TYPE_SURGEON, new ConcurrentHashMap<>());
        everyAppointment.put(APPOINTMENT_TYPE_DENTAL, new ConcurrentHashMap<>());
        usersAppointments = new ConcurrentHashMap<>();
        serverUsers = new ConcurrentHashMap<>();
    }

    static {
        cityPortMapping= new ConcurrentHashMap<>();
        cityPortMapping.put("MTL", SERVER_PORT_MONTREAL);
        cityPortMapping.put("SHE", SERVER_PORT_SHERBROOKE);
        cityPortMapping.put("QUE", SERVER_PORT_QUEBEC);
    }

    public static int getServerPort(String branchAcronym) {
        return cityPortMapping.getOrDefault(branchAcronym.toUpperCase(), defaultPort);
    }

    // add appointment method
    @Override
    public String addAppointment(String appointmentID, String appointmentType, int capacity) throws RemoteException {
        if (appointmentID == null || appointmentType == null || appointmentID.isEmpty() || appointmentType.isEmpty()) {
            return "Failed: Something wrong with AppointmentID or AppointmentType. Please check again!";
        }

        try {
            AppointmentDetailsModel existingAppointment = getAppointment(appointmentID, appointmentType);               // contains all existing appointment data
            if (existingAppointment != null) {
                return "Failed: Appointment already exists. Cannot add a duplicate appointment.";
            }
            return updateAppointment(appointmentID, appointmentType, capacity);
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed: Not able to Login";
        }
    }

    // remove appointment method
    @Override
    public String removeAppointment(String appointmentID, String appointmentType) throws RemoteException {
        try {
            return processAppointmentRemoval(appointmentID, appointmentType);
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed: Not able to Login";
        }
    }

    // give list of available appointment
    @Override
    public String listAppointmentAvailability(String appointmentType) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append(serverName).append(" Server ").append(appointmentType).append(":\n");

        Map<String, AppointmentDetailsModel> appointments = everyAppointment.get(appointmentType);

        if (appointments.isEmpty()) {
            builder.append("No appointments of Type ").append(appointmentType);
        } else {
            for (AppointmentDetailsModel appointment : appointments.values()) {
                builder.append(appointment.toString()).append("\n");
            }
            builder.append("\n \n");
        }

        String otherServer1 = sendUDPMessage(getOtherServerPortI(), "listAppointmentAvailability", "null", appointmentType, "null");
        String otherServer2 = sendUDPMessage(getOtherServerPortII(), "listAppointmentAvailability", "null", appointmentType, "null");

        builder.append(otherServer1).append(otherServer2);

        String response = builder.toString();

        try {
            Log.serverLog(serverID, "null", " RMI listAppointmentAvailability ", " appointmentType: " + appointmentType + " ", response);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }

    private int getOtherServerPortI() {
        return switch (serverID) {
            case "MTL" -> SERVER_PORT_SHERBROOKE;
            case "SHE" -> SERVER_PORT_QUEBEC;
            default -> SERVER_PORT_MONTREAL;
        };
    }

    private int getOtherServerPortII() {
        return switch (serverID) {
            case "MTL" -> SERVER_PORT_QUEBEC;
            case "SHE" -> SERVER_PORT_MONTREAL;
            default -> SERVER_PORT_SHERBROOKE;
        };
    }

    // book appointment method
    @Override
    public String bookAppointment(String patientID, String appointmentID, String appointmentType) throws RemoteException {
        try {
            if (!serverUsers.containsKey(patientID)) {
                addNewPatientID(patientID);
            }
            String serverName = AppointmentDetailsModel.locateAppointmentServer(appointmentID);
            if (serverName.equals(this.serverName)) {
                return bookLocalAppointment(patientID, appointmentID, appointmentType);
            } else {
                return bookRemoteAppointment(patientID, appointmentID, appointmentType);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed: An Appointment does not exist.";
        }
    }

    // book method for local appointment
    private String bookLocalAppointment(String patientID, String appointmentID, String appointmentType) throws RemoteException {
        AppointmentDetailsModel bookedAppointment = everyAppointment.get(appointmentType).get(appointmentID);
        Map<String, List<String>> patientAppointments = usersAppointments.get(patientID);
        patientAppointments.putIfAbsent(appointmentType, new ArrayList<>());
        List<String> appointmentsOfType = patientAppointments.get(appointmentType);

        if (bookedAppointment.isCapacityFull()) {
            for (Map.Entry<String, List<String>> entry : patientAppointments.entrySet()) {
                if (!entry.getKey().equals(appointmentType) && entry.getValue().contains(appointmentID)) {
                    return "Failed: Appointment " + appointmentID + " Already Booked for a different appointment type";
                }
            }
            return "Failed: Appointment " + appointmentID + " is Full";
        }

        if (appointmentsOfType.contains(appointmentID)) {
            return "Failed: Appointment " + appointmentID + " Already Booked";
        }

        if (bookedAppointment.getScheduledPatientIDs().contains(patientID)) {
            return "Failed: You have previously booked and canceled this appointment. You cannot book it again.";
        }

        for (List<String> appointments : patientAppointments.values()) {
            if (appointments.contains(appointmentID)) {
                return "Failed: Appointment " + appointmentID + " Already Booked for a different appointment type";
            }
        }

        appointmentsOfType.add(appointmentID);
        bookedAppointment.addScheduledPatientIDs(patientID);
        return "Success: Appointment " + appointmentID + " Booked Successfully";
    }

    // book appointment for remote means other server appointment
    private String bookRemoteAppointment(String patientID, String appointmentID, String appointmentType) throws IOException {
        if (exceedWeeklyLimit(patientID, appointmentID.substring(4))) {
            return "Sorry: You've reached the maximum weekly limit of 3 appointments for booking on other servers for this week.";
        }

        if (usersAppointments.containsKey(patientID) && usersAppointments.get(patientID).containsKey(appointmentType) &&
                usersAppointments.get(patientID).get(appointmentType).contains(appointmentID)) {
            return "Failed: You have previously booked and canceled this appointment. You cannot book it again.";
        }

        String serverResponse = sendUDPMessage(getServerPort(appointmentID.substring(0, 3)), "bookAppointment", patientID, appointmentType, appointmentID);

        if (serverResponse.startsWith("Success:")) {
            usersAppointments.putIfAbsent(patientID, new ConcurrentHashMap<>());
            usersAppointments.get(patientID).putIfAbsent(appointmentType, new ArrayList<>());
            usersAppointments.get(patientID).get(appointmentType).add(appointmentID);
            return "Success: Appointment " + appointmentID + " Booked Successfully";
        }

        return serverResponse;
    }

    // give the appointment schedule for given patient
    @Override
    public String getAppointmentSchedule(String patientID) throws RemoteException {
        if (!serverUsers.containsKey(patientID)) {
            addNewPatientID(patientID);
        }

        Map<String, List<String>> Appointments = usersAppointments.getOrDefault(patientID, new HashMap<>());

        StringBuilder builder = new StringBuilder();
        if (Appointments.isEmpty()) {
            builder.append("Booking Schedule Empty For ").append(patientID);
        } else {
            for (Map.Entry<String, List<String>> entry : Appointments.entrySet()) {
                builder.append(entry.getKey()).append(":\n");
                for (String appointmentID : entry.getValue()) {
                    builder.append(appointmentID).append(" \n");
                }
                builder.append("\n \n");
            }
        }

        String response = builder.toString();
        try {
            Log.serverLog(serverID, patientID, " RMI getBookingSchedule ", "null", response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    // cancel appointment method
    @Override
    public String cancelAppointment(String patientID, String appointmentID) throws IOException {
        String response;
        String appointmentType = findAppointmentType(patientID, appointmentID);

        if (AppointmentDetailsModel.locateAppointmentServer(appointmentID).equals(serverName)) {
            if (patientID.substring(0, 3).equals(serverID)) {
                if (!serverUsers.containsKey(patientID)) {
                    addNewPatientToUser(patientID);
                    response = "Failed: You " + patientID + " Are Not Registered in " + appointmentID;
                } else {
                    response = cancelAppointmentForRegisteredPatient(patientID, appointmentID, appointmentType);
                }
            } else {
                response = cancelAppointmentForNonLocalPatient(patientID, appointmentID, appointmentType);
            }
        } else {
            if (patientID.substring(0, 3).equals(serverID)) {
                if (!serverUsers.containsKey(patientID)) {
                    addNewPatientToUser(patientID);
                } else {
                    if (cancelAppointmentForPatient(patientID, appointmentID, appointmentType)) {
                        return sendUDPMessage(getServerPort(appointmentID.substring(0, 3)), "cancelAppointment", patientID, appointmentType, appointmentID);
                    }
                }
            }
            response = "Failed: You " + patientID + " Are Not Registered in " + appointmentID;
        }

        try {
            Log.serverLog(serverID, patientID, " RMI cancelAppointment ", " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ", response);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }

    // locate the appointment type
    private String findAppointmentType(String patientID, String appointmentID) {
        String appointmentType = "";
        Map<String, List<String>> innerMap = usersAppointments.get(patientID);

        for (Map.Entry<String, List<String>> entry : innerMap.entrySet()) {
            List<String> appointmentIds = entry.getValue();
            if (appointmentIds.contains(appointmentID)) {
                appointmentType = entry.getKey();
                break;
            }
        }
        return appointmentType;
    }

    private String cancelAppointmentForRegisteredPatient(String patientID, String appointmentID, String appointmentType) {
        if (usersAppointments.get(patientID).get(appointmentType).remove(appointmentID)) {
            everyAppointment.get(appointmentType).get(appointmentID).removeScheduledPatientID(patientID);
            return "Success: Appointment " + appointmentID + " Canceled for " + patientID;
        } else {
            return "Failed: You " + patientID + " Are Not Registered in " + appointmentID;
        }
    }

    private String cancelAppointmentForNonLocalPatient(String patientID, String appointmentID, String appointmentType) {
        if (everyAppointment.get(appointmentType).get(appointmentID).removeScheduledPatientID(patientID)) {
            usersAppointments.get(patientID).get(appointmentType).remove(appointmentID);
            return "Success: Appointment " + appointmentID + " Canceled for " + patientID;
        } else {
            return "Failed: You " + patientID + " Are Not Registered in " + appointmentID;
        }
    }

    private boolean cancelAppointmentForPatient(String patientID, String appointmentID, String appointmentType) {
        return usersAppointments.get(patientID).get(appointmentType).remove(appointmentID);
    }

    private String updateAppointment(String appointmentID, String appointmentType, int capacity) throws IOException {
        AppointmentDetailsModel appointment = getAppointment(appointmentID, appointmentType);                           // get all appointmentID exist
        if (appointment != null) {
            if (appointment.getAppointmentCapacity() <= capacity) {
                appointment.setAppointmentCapacity(capacity); // set capacity for particular appointmentID
                return logAndUpdate(appointmentID, appointmentType, capacity, "Capacity increased to " + capacity);
            } else {
                return logAddAppointment(appointmentID, appointmentType, capacity, " Appointment Already Exists, Cannot Modify the capacity");
            }
        } else if (!AppointmentDetailsModel.locateAppointmentServer(appointmentID).equals(serverName)) {
            return logAddAppointment(appointmentID, appointmentType, capacity, "Cannot add Appointment other than " + serverName);
        } else {
            appointment = new AppointmentDetailsModel(appointmentType, appointmentID, capacity);
            ConcurrentHashMap<String, AppointmentDetailsModel> appointmentHashMap = (ConcurrentHashMap<String, AppointmentDetailsModel>) everyAppointment.computeIfAbsent(appointmentType, k -> new ConcurrentHashMap<>());
            appointmentHashMap.put(appointmentID, appointment);
            return logAndUpdate(appointmentID, appointmentType, capacity, " Appointment added successfully");
        }
    }

    private String logAndUpdate(String appointmentID, String appointmentType, int capacity, String message) throws IOException {
        logAddAppointment(appointmentID, appointmentType, capacity, message);
        return "Success: " + message;
    }

    private AppointmentDetailsModel getAppointment(String appointmentID, String appointmentType) {
        ConcurrentHashMap<String, AppointmentDetailsModel> appointmentMap = (ConcurrentHashMap<String, AppointmentDetailsModel>) everyAppointment.getOrDefault(appointmentType, new ConcurrentHashMap<>());
        return appointmentMap.get(appointmentID);
    }

    private String logAddAppointment(String appointmentID, String appointmentType, int capacity, String message) throws IOException {
        String logMessage = "AppointmentID: " + appointmentID +
                " appointmentType: " + appointmentType +
                " capacity " + capacity +
                " " + message;
        Log.serverLog(serverID, "null", " RMI addAppointment ", logMessage);
        return "Failed: " + message;
    }

    private String processAppointmentRemoval(String appointmentID, String appointmentType) throws IOException {
        if (!AppointmentDetailsModel.locateAppointmentServer(appointmentID).equals(serverName)) {
            return logAndReturnFailure(appointmentID, appointmentType, "Cannot Remove Appointment from servers other than " + serverName);
        }

        ConcurrentHashMap<String, AppointmentDetailsModel> appointmentsOfType = (ConcurrentHashMap<String, AppointmentDetailsModel>) everyAppointment.get(appointmentType);
        if (appointmentsOfType == null || !appointmentsOfType.containsKey(appointmentID)) {
            return logAndReturnFailure(appointmentID, appointmentType, "Failed: Appointment " + appointmentID + " Does Not Exist");
        }

        AppointmentDetailsModel removedAppointment = appointmentsOfType.remove(appointmentID);
        if (removedAppointment != null) {
            givePatientsToNextSameAppointment(appointmentID, appointmentType, removedAppointment.getScheduledPatientIDs());
            return logAndReturnSuccess(appointmentID, appointmentType, "Appointment Removed Successfully");
        }

        return logAndReturnFailure(appointmentID, appointmentType, "Failed: Error occurred while removing Appointment");
    }

    private String logAndReturnSuccess(String appointmentID, String appointmentType, String message) throws IOException {
        logAppointment(appointmentID, appointmentType, message);
        return "Success: " + message;
    }

    private String logAndReturnFailure(String appointmentID, String appointmentType, String message) throws IOException {
        logAppointment(appointmentID, appointmentType, message);
        return "Failed: " + message;
    }

    private void logAppointment(String appointmentID, String appointmentType, String message) throws IOException {
        String logMessage = "appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " " + message;
        Log.serverLog(serverID, "null", " RMI removeAppointment ", logMessage);
    }

    private String sendUDPMessage(int serverPort, String method, String patientID, String appointmentType, String appointmentID) throws IOException {
        DatagramSocket aSocket = null;
        String result = "";
        String dataFromClient = method + ";" + patientID + ";" + appointmentType + ";" + appointmentID;
        Log.serverLog(serverID, patientID, " UDP request sent " + method + " ", " AppointmentID: " + appointmentID + " AppointmentType: " + appointmentType + " ", " ... ");
        try {
            aSocket = new DatagramSocket();
            byte[] message = dataFromClient.getBytes();
            InetAddress aHost = InetAddress.getByName("localhost");
            DatagramPacket request = new DatagramPacket(message, dataFromClient.length(), aHost, serverPort);
            aSocket.send(request);

            byte[] buffer = new byte[1000];
            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);

            aSocket.receive(reply);
            result = new String(reply.getData());
            String[] parts = result.split(";");
            result = parts[0];
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("IO: " + e.getMessage());
        } finally {
            if (aSocket != null)
                aSocket.close();
        }
        Log.serverLog(serverID, patientID, " UDP reply received" + method + " ", " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ", result);
        return result;

    }

    private void givePatientsToNextSameAppointment(String oldAppointmentID, String appointmentType, List<String> registeredClients) throws IOException {
        for (String patientID : registeredClients) {
            if (patientID.substring(0, 3).equals(serverID)) {
                if (usersAppointments.containsKey(patientID) && usersAppointments.get(patientID).containsKey(appointmentType)) {
                    usersAppointments.get(patientID).get(appointmentType).remove(oldAppointmentID);
                    String nextSameAppointment = getNextSameAppointment(everyAppointment.get(appointmentType).keySet(), appointmentType, oldAppointmentID);
                    if (!nextSameAppointment.equals("Failed")) {
                        bookAppointment(patientID, nextSameAppointment, appointmentType);
                    }
                }
            } else {
                sendUDPMessage(getServerPort(patientID.substring(0, 3)), "removeAppointment", patientID, appointmentType, oldAppointmentID);
            }
        }
    }

    public void addNewAppointment(String appointmentID, String appointmentType, int capacity) {
        AppointmentDetailsModel sampleConf = new AppointmentDetailsModel(appointmentType, appointmentID, capacity);
        everyAppointment.get(appointmentType).put(appointmentID, sampleConf);
    }

    public void addNewPatientID(String patientID) {
        UserModel newPatient = new UserModel(patientID);
        serverUsers.put(newPatient.getUserID(), newPatient);
        usersAppointments.put(newPatient.getUserID(), new ConcurrentHashMap<>());
    }

    private boolean exceedWeeklyLimit(String patientID, String appointmentData) {
        int limit = 0;
        for (int i = 0; i < 3; i++) {
            List<String> registeredIDs = new ArrayList<>();
            switch (i) {
                case 0:
                    if (usersAppointments.get(patientID).containsKey(APPOINTMENT_TYPE_PHYSICIAN)) {
                        registeredIDs = usersAppointments.get(patientID).get(APPOINTMENT_TYPE_PHYSICIAN);
                    }
                    break;
                case 1:
                    if (usersAppointments.get(patientID).containsKey(APPOINTMENT_TYPE_SURGEON)) {
                        registeredIDs = usersAppointments.get(patientID).get(APPOINTMENT_TYPE_SURGEON);
                    }
                    break;
                case 2:
                    if (usersAppointments.get(patientID).containsKey(APPOINTMENT_TYPE_DENTAL)) {
                        registeredIDs = usersAppointments.get(patientID).get(APPOINTMENT_TYPE_DENTAL);
                    }
                    break;
            }
            for (String appointmentID :
                    registeredIDs) {
                if (appointmentID.substring(6, 8).equals(appointmentData.substring(2, 4)) && appointmentID.substring(8, 10).equals(appointmentData.substring(4, 6))) {
                    int week1 = Integer.parseInt(appointmentID.substring(4, 6)) / 7;
                    int week2 = Integer.parseInt(appointmentData.substring(0, 2)) / 7;
                    if (week1 == week2) {
                        limit++;
                    }
                }
                if (limit == 3)
                    return true;
            }
        }
        return false;
    }

    private String getNextSameAppointment(Set<String> keySet, String appointmentType, String oldAppointmentID) {
        List<String> sortedIDs = new ArrayList<String>(keySet);
        sortedIDs.add(oldAppointmentID);
        Collections.sort(sortedIDs, new Comparator<String>() {
            @Override
            public int compare(String ID1, String ID2) {
                Integer timeSlot1 = switch (ID1.substring(3, 4).toUpperCase()) {
                    case "M" -> 1;
                    case "A" -> 2;
                    case "E" -> 3;
                    default -> 0;
                };
                Integer timeSlot2 = switch (ID2.substring(3, 4).toUpperCase()) {
                    case "M" -> 1;
                    case "A" -> 2;
                    case "E" -> 3;
                    default -> 0;
                };
                Integer date1 = Integer.parseInt(ID1.substring(8, 10) + ID1.substring(6, 8) + ID1.substring(4, 6));
                Integer date2 = Integer.parseInt(ID2.substring(8, 10) + ID2.substring(6, 8) + ID2.substring(4, 6));
                int dateCompare = date1.compareTo(date2);
                int timeSlotCompare = timeSlot1.compareTo(timeSlot2);
                if (dateCompare == 0) {
                    return ((timeSlotCompare == 0) ? dateCompare : timeSlotCompare);
                } else {
                    return dateCompare;
                }
            }
        });
        int index = sortedIDs.indexOf(oldAppointmentID) + 1;
        for (int i = index; i < sortedIDs.size(); i++) {
            if (!everyAppointment.get(appointmentType).get(sortedIDs.get(i)).isCapacityFull()) {
                return sortedIDs.get(i);
            }
        }
        return "Failed";
    }

    public void addNewPatientToUser(String patientID) {
        UserModel newCustomer = new UserModel(patientID);
        serverUsers.put(newCustomer.getUserID(), newCustomer);
        usersAppointments.put(newCustomer.getUserID(), new ConcurrentHashMap<>());
    }

    /**
     * for udp calls only
     *
     * @param oldPatientID
     * @param appointmentType
     * @param patientID
     * @throws RemoteException
     */
    public String removeAppointmentUDP(String oldPatientID, String appointmentType, String patientID) throws RemoteException {
        if (!serverUsers.containsKey(patientID)) {
            addNewPatientID(patientID);
            return "Failed: " + patientID + " Are Not Registered in " + oldPatientID;
        } else {
            if (usersAppointments.get(patientID).get(appointmentType).remove(oldPatientID)) {
                return "Success: Appointment " + oldPatientID + " Was Removed from " + patientID + " Schedule";
            }  else {
                return "Failed:" + patientID + " is not Registered in " + oldPatientID;
            }
        }
    }

    /**
     * for UDP calls only
     *
     * @param appointmentType
     * @return
     * @throws RemoteException
     */
    public String listAppointmentAvailabilityUDP(String appointmentType) throws RemoteException {
        Map<String, AppointmentDetailsModel> appointments = everyAppointment.get(appointmentType);
        StringBuilder builder = new StringBuilder();
        builder.append(serverName).append(" Server ").append(appointmentType).append(":\n");
        if (appointments.isEmpty()) {
            builder.append("No Appointments of Type ").append(appointmentType);
        } else {
            for (AppointmentDetailsModel appointment :
                    appointments.values()) {
                builder.append(appointment.toString()).append("\n");
            }
        }
        builder.append("\n___________________________\n");
        return builder.toString();
    }

}
