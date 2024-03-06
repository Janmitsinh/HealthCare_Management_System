# HealthCare_Management_System
Goal: - The goal of a Distributed Health Care Management System (DHMS) is to manage  medical appointments
Here, we have two types of users of the system:
1] By Hospital admins: - Admins use DHMS to manage the information about medical 
appointments. To perform add, remove, list appointment and also patient actions.
2] By Patients: - Patients use DHMS to book, cancel and get Appointment Schedules across 
different hospitals within the Medicare System.
Here, we have three servers which run in three different cities: -
1] Montreal (MTL)
2] Sherbrooke (SHE)
3] Quebec (QUE)
Admin and Patient Functionalities: -
Admin Specific Functions: -
1] addAppoinment(appointmentID, appointmentType, capacity): - Checks if an appointment of 
the same type already exists, if it does, it should fail and if not, the appointment will add.
2] removeAppointment(appointmentID, appointmentType): - Admin can only remove 
appointments from their server. If an Appointment was removed, we must book another closest 
appointment for the patient registered for that doctor.
3] listAppointmentAvailability(appointmentType): - List out all appointments of a given type 
from all three servers.
Patient-specific Functions: -
1] bookAppointment(patientID, appointmentID, appointmentType): The patient can also book 
from other servers with a weekly 3 limit.
2] getAppointmentSchedule(patientID): - Give the appointment schedule.
3] cancelAppointment(patientID, appointmentID): - Patient can remove an appointment from 
their schedule.

Important/Difficult Part of this Assignment: -
1. Implementing Java RMI: Need to design and implement the Java RMI interface for the 
server and ensure that remote access calls between the client and the server are handled 
correctly and reliably.
2. Managing distributed data: Need to manage appointment records in HashMap, 
ensuring consistency and consistency of data for multiple servers for different cities 
(Montreal, Quebec, Sherbrooke). This requires an understanding of data structures.
3. Inter-Server Communication: Use functionality for servers to communicate with each 
other, using UDP/IP sockets, especially for applications such as booking appointments with 
hospitals.
