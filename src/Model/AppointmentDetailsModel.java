package Model;

import java.util.ArrayList;
import java.util.List;
import static Variables.DeclareVariable.*;

// managing and manipulating appointment related data
public class AppointmentDetailsModel {

    private String appointmentDate; // appointmentdate (e.x. 110224)
    private String appointmentTimeSlot;  // appointmentTimeslot (e.x m for morning)
    private List<String> allScheduledPatients;  // all schedule patients with their appointment store in list format
    private String appointmentServer;
    private String appointmentType;     // appointment (e.x dental, physician and surgeon
    private int appointmentCapacity; // capacity of each appointment
    private String appointmentID;  // appointmentID (e.x MTLM101124

    // Constructor
    public AppointmentDetailsModel(String appointmentType, String appointmentID, int appointmentCapacity) {
        this.appointmentID = appointmentID;
        this.appointmentType = appointmentType;
        this.appointmentCapacity = appointmentCapacity;
        this.appointmentTimeSlot = locateAppointmentTimeSlot(appointmentID);
        this.appointmentServer = locateAppointmentServer(appointmentID);
        this.appointmentDate = locateAppointmentDate(appointmentID);
        allScheduledPatients = new ArrayList<>();
    }

    // find the appointment server
    public static String locateAppointmentServer(String appointmentID) {
        String appointment = appointmentID.substring(0, 3).toUpperCase();
        return switch (appointment) {
            case "MTL" -> MONTREAL_APPOINTMENT_SERVER;
            case "QUE" -> QUEBEC_APPOINTMENT_SERVER;
            default -> SHERBROOKE_APPOINTMENT_SERVER;
        };
    }

    // find appointment timeslot
    public static String locateAppointmentTimeSlot(String appointmentID) {
        String timeSlot = appointmentID.substring(3, 4).toUpperCase();
        return switch (timeSlot) {
            case "M" -> MORNING_SLOT;
            case "A" -> AFTERNOON_SLOT;
            default -> EVENING_SLOT;
        };
    }

    // find appointment date
    public static String locateAppointmentDate(String appointmentID){
        return appointmentID.substring(4,6) + "/" + appointmentID.substring(6,8) + "/24" + appointmentID.substring(8,10);
    }

    public int addScheduledPatientIDs(String scheduledPatientID) {
        if (isCapacityFull()) {
            return ALL_APPOINTMENT_FULL;
        }
        if (allScheduledPatients.contains(scheduledPatientID)) {
            return EXIST_APPOINTMENT;
        } else {
            allScheduledPatients.add(scheduledPatientID);
            return GET_APPOINTMENT;
        }
    }

    public boolean removeScheduledPatientID(String scheduledPatientID){
        return allScheduledPatients.remove(scheduledPatientID);
    }

    @Override
    public String toString(){
        return  getAppointmentID() + ": in the " + getAppointmentTimeSlot() + " of " + getAppointmentDate() + " Total Available Capacity: "+ getRemainAppointmentCapacity() + " out of " + getAppointmentCapacity();
    }

    public String getAppointmentID() { return appointmentID;}

    public int getAppointmentCapacity(){return appointmentCapacity;}

    public void setAppointmentCapacity(int appointmentCapacity) {
        this.appointmentCapacity = appointmentCapacity;
    }

    public int getRemainAppointmentCapacity() {
        return appointmentCapacity - allScheduledPatients.size();
    }

    public String getAppointmentDate(){
        return appointmentDate;
    }

    public String getAppointmentTimeSlot() {
        return appointmentTimeSlot;
    }

    public boolean isCapacityFull() {
        return getAppointmentCapacity() == allScheduledPatients.size();
    }

    public List<String> getScheduledPatientIDs(){
        return allScheduledPatients;
    }
}
