package Interface;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface HealthCareInterface extends Remote {
    /* admin methods */

    // Define addAppointment method
    String addAppointment(String appointmentID, String appointmentType, int capacity) throws RemoteException;

    // Define removeAppointment method
    String removeAppointment (String appointmentID, String appointmentType) throws RemoteException;

    // Define listAppointmentAvailabilty method
    String listAppointmentAvailability (String appointmentType) throws IOException;

    /* admin & patient methods */

    // Define bookAppointment method
    String bookAppointment (String patientID,String appointmentID, String appointmentType) throws RemoteException;

    // Define getAppointmentSchedule method
    String getAppointmentSchedule (String patientID) throws RemoteException;

    // Define cancelAppointment method
    String cancelAppointment (String patientID, String appointmentID) throws IOException;
}
