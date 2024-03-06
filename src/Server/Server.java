package Server;

import Log.Log;
import Interface.ImplementHealthCareInterface;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import static Variables.DeclareVariable.*;

public class Server{

    private final String serverID;
    private final String serverName;                                                                                    // serverName (MTL,QUE,SHE)
    private final int serverRegistryPort;                                                                               // Different registry port for server
    private final int serverUdpPort;                                                                                    // Different server udp port
    private final ImplementHealthCareInterface remoteObject;

    public static void main(String[] args) throws Exception {
        Server QuebecServer = new Server("MTL");
        Server MontrealServer = new Server("QUE");
        Server SherbrookeServer = new Server("SHE");
    }

    // Constructor
    public Server(String serverID) throws Exception {
        this.serverID = serverID;
        this.serverName = getServerName(serverID);                                                                      // get the servername MTL,SHE,QUE
        this.serverRegistryPort = getServerRegistryPort(serverID);                                                      // get server registry port as per server name
        this.serverUdpPort = getServerUdpPort(serverID);                                                                // get udp server port

        remoteObject = new ImplementHealthCareInterface(serverID, serverName);
        bindRemoteObjectToRegistry();
        startUdpServer();
        logServerStatus();
        addFewRecords();
    }

    // checking server
    private String getServerName(String serverID) {
        return switch (serverID) {
            case "MTL" -> MONTREAL_APPOINTMENT_SERVER;
            case "QUE" -> QUEBEC_APPOINTMENT_SERVER;
            case "SHE" -> SHERBROOKE_APPOINTMENT_SERVER;
            default -> throw new IllegalArgumentException("Invalid server ID: " + serverID);
        };
    }

    private int getServerRegistryPort(String serverID) {
        return switch (serverID) {
            case "MTL" -> MONTREAL_REGISTRY_SERVER;
            case "QUE" -> QUEBEC_REGISTRY_SERVER;
            case "SHE" -> SHERBROOKE_REGISTRY_SERVER;
            default -> throw new IllegalArgumentException("Invalid server ID: " + serverID);
        };
    }

    private int getServerUdpPort(String serverID) {
        return switch (serverID) {
            case "MTL" -> SERVER_PORT_MONTREAL;
            case "QUE" -> SERVER_PORT_QUEBEC;
            case "SHE" -> SERVER_PORT_SHERBROOKE;
            default -> throw new IllegalArgumentException("Invalid server ID: " + serverID);
        };
    }

    private void bindRemoteObjectToRegistry() throws Exception {
        Registry registry = LocateRegistry.createRegistry(serverRegistryPort);                                          // Locate the registry for the server Registry port
        registry.bind(HEALTH_CARE_SYSTEM, remoteObject);                                                                // Bind(store) the remote object in rmi registry
    }

    private void startUdpServer() {
        Runnable task = this::listenForRequest;
        Thread thread = new Thread(task);
        thread.start();
    }

    private void logServerStatus() throws IOException {
        System.out.println(serverName + " Server is Ready...");
        Log.serverLog(serverID, " Server is Ready...");
    }

    // pre-requisite records which is required to perform test scenarios
    private void addFewRecords() {
        switch (serverID) {
            case "MTL":
                remoteObject.addNewAppointment("MTLA050324", APPOINTMENT_TYPE_PHYSICIAN, 2);             // Add same appointmentDi for this and below record if MTLP2345 patient try to book appointmentDI MTLA050324 which is exist in both physician and surgon
                remoteObject.addNewAppointment("MTLA050324", APPOINTMENT_TYPE_DENTAL, 2);
                remoteObject.addNewAppointment("MTLA050324", APPOINTMENT_TYPE_PHYSICIAN, 2);
                remoteObject.addNewAppointment("MTLA060324", APPOINTMENT_TYPE_PHYSICIAN, 2);
                remoteObject.addNewAppointment("MTLE050324", APPOINTMENT_TYPE_SURGEON, 1);
                remoteObject.addNewAppointment("MTLM060324", APPOINTMENT_TYPE_DENTAL, 12);
                remoteObject.addNewAppointment("MTLA070324", APPOINTMENT_TYPE_SURGEON, 8);
                remoteObject.addNewPatientID("MTLP1090");
                remoteObject.addNewPatientID("MTLP1121");
                break;
            case "QUE":
                remoteObject.addNewAppointment("QUEM100624", APPOINTMENT_TYPE_PHYSICIAN, 5);
                remoteObject.addNewAppointment("QUEM090624", APPOINTMENT_TYPE_PHYSICIAN, 5);
                remoteObject.addNewAppointment("QUEM080624", APPOINTMENT_TYPE_SURGEON, 5);
                remoteObject.addNewAppointment("QUEM080624", APPOINTMENT_TYPE_DENTAL, 18);
                remoteObject.addNewPatientID("QUEP0821");
                remoteObject.addNewPatientID("QUEP1020");
                break;
            case "SHE":
                remoteObject.addNewAppointment("SHEE080624", APPOINTMENT_TYPE_PHYSICIAN, 1);
                remoteObject.addNewAppointment("SHEA070624", APPOINTMENT_TYPE_PHYSICIAN, 2);
                remoteObject.addNewAppointment("SHEA050624", APPOINTMENT_TYPE_DENTAL, 20);
                remoteObject.addNewAppointment("SHEM090624", APPOINTMENT_TYPE_SURGEON, 5);
                remoteObject.addNewPatientID("SHEP1001");
                remoteObject.addNewPatientID("SHEP2001");
                break;
        }
    }

    private void listenForRequest() {
        try (DatagramSocket aSocket = new DatagramSocket(serverUdpPort)) {
            System.out.println(serverName + " UDP Server Started at port " + aSocket.getLocalPort() + " ././././././");
            Log.serverLog(serverID, " UDP Server Started at port " + aSocket.getLocalPort());
            while (true) {
                processRequest(aSocket);
            }
        } catch (IOException e) {
            System.out.println("SocketException: " + e.getMessage());
        }
    }

    private void processRequest(DatagramSocket socket) {
        try {
            byte[] buffer = new byte[1000];                                                                             // Adjust buffer size(1000 bytes of data can be received from the network in a single read operation
            DatagramPacket request = new DatagramPacket(buffer, buffer.length);                                         // request is used to receive the incoming data from the network
            socket.receive(request);                                                                                    // This will receive 1000 bytes of data which is store in request
            String requestData = new String(request.getData()).trim();                                                  // It will convert receive data into string and trim the whitespaces
            String[] parts = requestData.split(";");                                                              // It will split the data by ';' and store into array
            if (parts.length >= 4) {
                String method = parts[0];
                String patientID = parts[1];
                String appointmentType = parts[2];
                String appointmentID = parts[3];
                String result = switch (method.toLowerCase()) {
                    case "removeappointment" -> processRemoveAppointment(patientID, appointmentType, appointmentID);
                    case "listappointmentavailability" -> processListAppointmentAvailability(appointmentType);
                    case "bookappointment" -> processBookAppointment(patientID, appointmentID, appointmentType);
                    case "cancelappointment" -> processCancelAppointment(patientID, appointmentID, appointmentType);
                    default -> "";
                };
                sendResponse(socket, request.getAddress(), request.getPort(), result);                                  // send response to client
                Log.serverLog(serverID, patientID, " UDP reply sent " + method + " ", " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ", result);
            } else {
                System.out.println("Invalid request data: " + requestData);
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private String processRemoveAppointment(String patientID, String appointmentType, String appointmentID) throws RemoteException {
        return remoteObject.removeAppointmentUDP(appointmentID, appointmentType, patientID) + ";";
    }

    private String processListAppointmentAvailability(String appointmentType) throws RemoteException {
        return remoteObject.listAppointmentAvailabilityUDP(appointmentType) + ";";
    }

    private String processBookAppointment(String patientID, String appointmentID, String appointmentType) throws RemoteException {
        return remoteObject.bookAppointment(patientID, appointmentID, appointmentType) + ";";
    }

    private String processCancelAppointment(String patientID, String appointmentID, String appointmentType) throws IOException {
        return remoteObject.cancelAppointment(patientID, appointmentID) + ";";
    }

    private void sendResponse(DatagramSocket socket, InetAddress address, int port, String responseData) throws IOException {
        byte[] sendData = responseData.getBytes(); // convert data into bytes to send data to client
        DatagramPacket reply = new DatagramPacket(sendData, sendData.length, address, port);    // data ne Packets(bundle) na form ma send karse
        socket.send(reply);
    }
}
