package Client;

import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
import Interface.HealthCareInterface;
import Log.Log;
import static Variables.DeclareVariable.*;

public class Client {

    private static Scanner input;  // to get input

    public static void main(String[] args) {
        try {
            startClientServer();
        } catch (CustomException | Exception e) {
//            e.printStackTrace();
            System.out.println("Please try again!.");
        }
    }

    // Login section. It will handle login of admin and patient
    public static void startClientServer() throws CustomException {
        try {
            input = new Scanner(System.in);
            System.out.println("------------- Welcome to Health Care Management System -------------");
            System.out.println("Please Enter your ID:");
            String userID = input.next().trim().toUpperCase();
            Log.userLog(userID, " login attempt");

            switch (checkUserType(userID)) {
                case ACCOUNT_TYPE_PATIENT:
                    handlePatientLogin(userID);
                    break;
                case ACCOUNT_TYPE_ADMIN:
                    handleAdminLogin(userID);
                    break;
                default:
                    System.out.println("Please Enter a valid UserID !!!");
                    Log.userLog(userID, " UserID is not in correct format");
                    Log.deleteLogFile(userID);
                    startClientServer();
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error occur in client server: " + e.getMessage());
            e.printStackTrace();  // throw a stack of error if try block fail
        }
    }

    // print patient login information and pass patientId and serverPort to patient
    private static void handlePatientLogin(String userID) throws IOException, CustomException {
        System.out.println("Patient with an ID of " + userID  + " successful Login ");
        Log.userLog(userID, " Patient Login successful");
        patient(getServerPort(userID.substring(0, 3)), userID);     // no need of userID
    }

    // print admin login information and pass adminId and serverPort to admin
    private static void handleAdminLogin(String userID) throws IOException, CustomException {
        System.out.println("Admin with an ID of (" + userID + "]" + "successful Login");
        Log.userLog(userID, " Admin Login successful");
        admin(getServerPort(userID.substring(0, 3)), userID);
    }

    // get server port for different cities
    private static int getServerPort(String cities) {
        return switch (cities.toUpperCase()) {
            case "MTL" -> MONTREAL_REGISTRY_SERVER;
            case "SHE" -> SHERBROOKE_REGISTRY_SERVER;
            case "QUE" -> QUEBEC_REGISTRY_SERVER;
            default -> 1;
        };
    }

    // check the type of user depends on character A (admin) or P (patient)
    private static int checkUserType(String userID) {
        // Check Invalid user ID length. Length of userID should be exactly 8.
        if (userID.length() != 8) {
            return 0;
        }

        String getUserCity = userID.substring(0, 3).toUpperCase();
        char userTypeChar = userID.charAt(3);

        if (getUserCity.equals("MTL") || getUserCity.equals("QUE") || getUserCity.equals("SHE")) {
            if (userTypeChar == 'P') {
                return ACCOUNT_TYPE_PATIENT;
            } else if (userTypeChar == 'A') {
                return ACCOUNT_TYPE_ADMIN;
            }
        }

        return 0;
    }

    // Locate RMI registry and lookup for HEALTH_CARE_SYSTEM remoteObject to invokes methods in remote object interface
    private static void patient(int serverPort, String patientID) throws CustomException, IOException {
        try {
            Registry registry = LocateRegistry.getRegistry(serverPort);                                                   // it will locate rmiregistry (which is running on registry port) using serverport
            HealthCareInterface remoteObject = (HealthCareInterface) registry.lookup(HEALTH_CARE_SYSTEM);                 // lookup method find the remote object on the Rmi Registry and return the refrence of the remote object.
            boolean isRepeat = true;

            do {
                getMenuOption(ACCOUNT_TYPE_PATIENT);                                                                      // it will trigger getMenuOption method for patient
                int menuSelection = input.nextInt();
                String appointmentType;
                String appointmentID;
                String serverResponse;

                switch (menuSelection) {
                    case PATIENT_BOOK_APPOINTMENT: // invoke bookAppointment method
                        appointmentType = getMenuOptionForAppointmentType();
                        appointmentID = getAppointmentID();
                        Log.userLog(patientID, " booking to " + "Appointment");
                        serverResponse = remoteObject.bookAppointment(patientID, appointmentID, appointmentType);
                        System.out.println(serverResponse);
                        Log.userLog(patientID, "bookAppointment ", " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ", serverResponse);
                        break;
                    case PATIENT_CANCEL_APPOINTMENT: // invoke cancelAppointment method
                        appointmentType = getMenuOptionForAppointmentType();
                        appointmentID = getAppointmentID();
                        Log.userLog(patientID, " attempting to " + "cancelAppointment");
                        serverResponse = remoteObject.cancelAppointment(patientID, appointmentID);
                        System.out.println(serverResponse);
                        Log.userLog(patientID, "cancelAppointment", " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ", serverResponse);
                        break;
                    case PATIENT_GET_APPOINTMENT_SCHEDULE: // invoke patientAppointmentSchedule method
                        Log.userLog(patientID, " trying to getAppointmentSchedule");
                        serverResponse = remoteObject.getAppointmentSchedule(patientID);
                        System.out.println(serverResponse);
                        Log.userLog(patientID, " bookAppointment", " null ", serverResponse);
                        break;
                    case PATIENT_LOGOUT:
                        isRepeat = false;
                        Log.userLog(patientID, " trying to Logout");
                        break;
                }
            } while (isRepeat);
            startClientServer();
        } catch (Exception e){
            System.err.println("Something wrong : You can not perform this operation " + e.getMessage() + " Please try again.");
            Log.userLog(e.getMessage() ,"trying to Logout");                                              // e.printStack();
        }
    }

    // Locate RMI registry and lookup for HEALTH_CARE_SYSTEM remoteObject to invokes methods in remote object interface
    private static void admin(int serverPort, String appointmentAdminID) throws CustomException, IOException {
      try {
          Registry registry = LocateRegistry.getRegistry(serverPort);
          HealthCareInterface remoteObject = (HealthCareInterface) registry.lookup(HEALTH_CARE_SYSTEM);
          boolean isRepeat = true;

          do {
              getMenuOption(ACCOUNT_TYPE_ADMIN);                                                                                // it will trigger getMenuOption method for admin
              int menuSelection = input.nextInt();

              switch (menuSelection) {
                  case ADMIN_ADD_APPOINTMENT:                                                                                   // invoke addAppointment method
                  case ADMIN_REMOVE_APPOINTMENT:                                                                                // invoke removeAppointment method
                  case ADMIN_LIST_APPOINTMENT_AVAILABILITY:                                                                     // invoke listAppointmentAvailability method
                      handleAdminAppointment(menuSelection, serverPort, appointmentAdminID, remoteObject);
                      break;
                  case ADMIN_BOOK_APPOINTMENT:                                                                                  // invoke bookAppointment method
                  case ADMIN_GET_APPOINTMENT_SCHEDULE:                                                                          // invoke appointmentSchedule method
                  case ADMIN_CANCEL_APPOINTMENT:                                                                                // invoke cancelAppointment method
                      handleAdminAction(menuSelection, appointmentAdminID, remoteObject);
                      break;
                  case ADMIN_LOGOUT:
                      isRepeat = false;
                      Log.userLog(appointmentAdminID, "trying to Logout");
                      break;
              }
          } while (isRepeat);

          startClientServer();
      } catch (Exception e) {
          System.err.println("You can not perform this operation: " + e.getMessage());
//          e.printStackTrace();
          Log.userLog(e.getMessage() ,"trying to Logout");

      }
    }

    // handle admin appointment
    private static void handleAdminAppointment(int menuSelection, int serverPort, String appointmentAdminID, HealthCareInterface remoteObject) throws IOException {
        String action = listOfActionForAdmin(menuSelection);

        String appointmentID;
        String appointmentType;
        Log.userLog(appointmentAdminID, "attempting to " + action);

        switch (menuSelection) {
            case ADMIN_ADD_APPOINTMENT:
                appointmentType = getMenuOptionForAppointmentType();
                appointmentID= getAppointmentID();
                int capacity = getCapacity();
                String serverResponse = remoteObject.addAppointment(appointmentID, appointmentType, capacity);  // invoke add Appointment method
                System.out.println(serverResponse);
                Log.userLog(appointmentAdminID, action, "appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " appointmentCapacity: " + capacity + " ", serverResponse);
                break;
            case ADMIN_REMOVE_APPOINTMENT:
                appointmentType = getMenuOptionForAppointmentType();
                appointmentID = getAppointmentID();
                serverResponse = remoteObject.removeAppointment(appointmentID, appointmentType);                 // invoke remove Appointment method
                System.out.println(serverResponse);
                Log.userLog(appointmentAdminID, action, "appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ", serverResponse);
                break;
            case ADMIN_LIST_APPOINTMENT_AVAILABILITY:
                appointmentType = getMenuOptionForAppointmentType();
                serverResponse = remoteObject.listAppointmentAvailability(appointmentType);                      // invoke listAppointmentAvailability method
                System.out.println(serverResponse);
                Log.userLog(appointmentAdminID, action, "appointmentType: " + appointmentType + " ", serverResponse);
                break;
        }
    }

    private static void handleAdminAction(int menuSelection, String appointmentAdminID, HealthCareInterface remoteObject) throws IOException {
        String patientID;
        String appointmentType;
        String appointmentID;
        String action = getActionForAdminAction(menuSelection);
        Log.userLog(appointmentAdminID, "attempting to " + action);

        switch (menuSelection) {
            case ADMIN_BOOK_APPOINTMENT:
                patientID = askForPatientIDFromAdmin(appointmentAdminID.substring(0, 3));
                appointmentType = getMenuOptionForAppointmentType();
                appointmentID = getAppointmentID();
                String serverResponse = remoteObject.bookAppointment(patientID, appointmentID, appointmentType);        // invoke bookAppointment method
                System.out.println(serverResponse);
                Log.userLog(appointmentAdminID, action, "patientID: " + patientID + " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ", serverResponse);
                break;
            case ADMIN_GET_APPOINTMENT_SCHEDULE:
                patientID = askForPatientIDFromAdmin(appointmentAdminID.substring(0, 3));
                serverResponse = remoteObject.getAppointmentSchedule(patientID);                                        // invoke getAppointmentSchedule method
                System.out.println(serverResponse);
                Log.userLog(appointmentAdminID, action, "patientID: " + patientID + " ", serverResponse);
                break;
            case ADMIN_CANCEL_APPOINTMENT:
                patientID = askForPatientIDFromAdmin(appointmentAdminID.substring(0, 3));
                appointmentType = getMenuOptionForAppointmentType();
                appointmentID = getAppointmentID();
                serverResponse = remoteObject.cancelAppointment(patientID, appointmentID);             // invoke cancelAppointment method
                System.out.println(serverResponse);
                Log.userLog(appointmentAdminID, action, "patientID: " + patientID + " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ", serverResponse);
                break;
        }
    }

    private static String askForPatientIDFromAdmin(String cities) {
        Scanner scanner = new Scanner(System.in);
        String userID;
        do {
            System.out.println("Please enter a patientID (Within " + cities + " Server):");
            userID = scanner.next().trim().toUpperCase();
        } while (checkUserType(userID) != ACCOUNT_TYPE_PATIENT || !userID.substring(0, 3).equals(cities));
        return userID;
    }

    private static String getActionForAdminAction(int menuSelection) {
        return switch (menuSelection) {
            case ADMIN_BOOK_APPOINTMENT -> "bookAppointment";
            case ADMIN_GET_APPOINTMENT_SCHEDULE -> "getBookingSchedule";
            case ADMIN_CANCEL_APPOINTMENT -> "cancelAppointment";
            default -> "";
        };
    }

    private static String listOfActionForAdmin(int menuSelection) {
        return switch (menuSelection) {
            case ADMIN_ADD_APPOINTMENT -> "addAppointment";
            case ADMIN_REMOVE_APPOINTMENT -> "removeAppointment";
            case ADMIN_LIST_APPOINTMENT_AVAILABILITY -> "listAppointmentAvailability";
            default -> "";
        };
    }

    // Menu of three appointment
    private static String getMenuOptionForAppointmentType() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("-----------------------------------------");
        System.out.println("Please pick an option below:");
        System.out.println("[Option 1] Physician");
        System.out.println("[Option 2] Surgeon");
        System.out.println("[Option 3] Dental");
        System.out.println();
        System.out.println("Please enter corresponding number from the menu above");
        return switch (scanner.nextInt()) {
            case 1 -> APPOINTMENT_TYPE_PHYSICIAN;
            case 2 -> APPOINTMENT_TYPE_SURGEON;
            case 3 -> APPOINTMENT_TYPE_DENTAL;
            default -> getMenuOptionForAppointmentType();
        };
    }

    // get appointmentID from user
    private static String getAppointmentID() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("-----------------------------------------");
        System.out.println("Please enter the appointmentID (combination of city, time slot, and appointment data)");
        String appointmentID = scanner.next().trim().toUpperCase();

        if (isValidAppointmentID(appointmentID)) {
            return appointmentID;
        } else {
            System.out.println("Invalid appointmentID format. Please try again.");
            return getAppointmentID(); // Recursively call getAppointmentID until a valid appointmentID is entered
        }
    }

    // validate the AppointmentID
    private static boolean isValidAppointmentID(String appointmentID) {
        if (appointmentID.length() != 10) {
            return false;
        }
        String cityCode = appointmentID.substring(0, 3).toUpperCase();
        char timeSlot = appointmentID.charAt(3);
        return (cityCode.equals("MTL") || cityCode.equals("SHE") || cityCode.equals("QUE"))
                && (timeSlot == 'M' || timeSlot == 'A' || timeSlot == 'E');
    }

    // ask for capacity of particular appointment
    private static int getCapacity() {
        System.out.println("-----------------------------------------");
        System.out.println("Please input the appointment booking capacity:");
        return input.nextInt();
    }

    private static void getMenuOption(int userType) {
        System.out.println("-----------------------------------------");
        System.out.println("Please pick an option below:");
        if (userType == ACCOUNT_TYPE_PATIENT) {
            System.out.println("[Option 1] Book Appointment");
            System.out.println("[Option 2] Get Appointment Schedule");
            System.out.println("[Option 3] Cancel Appointment");
            System.out.println("[Option 4] Logout");
            System.out.println();
            System.out.println("Please enter corresponding number from the menu above");
        } else if (userType == ACCOUNT_TYPE_ADMIN) {
            System.out.println("[Option 1] Add Appointment");
            System.out.println("[Option 2] Remove Appointment");
            System.out.println("[Option 3] List Appointment Availability");
            System.out.println("[Option 4] Book Appointment for Patient");
            System.out.println("[Option 5] Get Appointment Schedule for Patient");
            System.out.println("[Option 6] Cancel Appointment for Patient");
            System.out.println("[Option 7] Logout");
            System.out.println();
            System.out.println("Please enter corresponding number from the menu above");
        }
    }
}
