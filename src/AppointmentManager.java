
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class AppointmentManager {
    private static List<Appointment> appointments = new ArrayList<>();
    private static final String APPOINTMENTS_FILE = "appointments.csv";

    // Load appointments from CSV file
    public static List<Appointment> loadAppointmentsFromCSV() {
        appointments.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(APPOINTMENTS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                Appointment appointment = Appointment.fromCSV(line);
                appointments.add(appointment);
            }
        } catch (IOException e) {
            System.out.println("Error reading from CSV file: " + e.getMessage());
        }
        return appointments;
    }

    // Add a new appointment
    public static void addAppointment(Appointment appointment) {
        appointments.add(appointment);
        saveAppointmentsToCSV();  // Save after adding new appointment
    }

    // Save all appointments to CSV
    public static void saveAppointmentsToCSV() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(APPOINTMENTS_FILE))) {
            for (Appointment appointment : appointments) {
                StringBuilder line = new StringBuilder();
                line.append(appointment.getDoctorId()).append(",")
                    .append(appointment.getPatientId()).append(",")
                    .append(appointment.getDate()).append(",")
                    .append(appointment.getTimeSlot()).append(",")
                    .append(appointment.getStatus());

                // Adding the service, medication, medStatus, and notes to the CSV
                if (appointment.getOutcome() != null) {
                    AppointmentOutcome outcome = appointment.getOutcome();
                    line.append(",").append(outcome.getService())
                        .append(",").append(outcome.getMedication())
                        .append(",").append(outcome.getMedStatus())
                        .append(",").append(outcome.getNotes());
                } else {
                    line.append(",,,,"); // Fill with empty values if no outcome
                }
                writer.println(line);
            }
        } catch (IOException e) {
            System.out.println("Error saving appointments: " + e.getMessage());
        }
    }

    public static void updateAppointmentInCSV(Appointment updatedAppointment) {
        List<Appointment> allAppointments = getAppointmentsFromCSVForValidation();

        // Update the appointment in the list
        for (int i = 0; i < allAppointments.size(); i++) {
            Appointment appointment = allAppointments.get(i);
            if (appointment.getDoctorId().equals(updatedAppointment.getDoctorId()) &&
                appointment.getDate().equals(updatedAppointment.getDate()) &&
                appointment.getTimeSlot().equals(updatedAppointment.getTimeSlot())) {
                allAppointments.set(i, updatedAppointment);
                break;
            }
        }

        // Write back to the CSV file
        try (PrintWriter writer = new PrintWriter(new FileWriter(APPOINTMENTS_FILE))) {
            for (Appointment appointment : allAppointments) {
                writer.println(appointment.toCSV());
            }
        } catch (IOException e) {
            System.out.println("Error updating appointments CSV: " + e.getMessage());
        }
    }

    // Retrieve all appointments
    public static List<Appointment> getAppointments() {
        return appointments;
    }

    public static void viewPatientAppointments(String patientId) {
        System.out.println("Your scheduled appointments:");
        boolean found = false;
        for (Appointment appointment : appointments) {
            if (patientId.equals(appointment.getPatientId())) {
                System.out.println("Date: " + appointment.getDate() + " Time: " + appointment.getTimeSlot() 
                    + " Doctor: " + appointment.getDoctorId() + " Status: " + appointment.getStatus());
                found = true;
            }
        }
        if (!found) {
            System.out.println("You have no scheduled appointments.");
        }
    }

    public static void viewAvailableSlots() {
        boolean found = false;
        System.out.println("Appointment slots:");
        for (Appointment appointment : appointments) {
            if (appointment.getPatientId() == null && appointment.getStatus() == Appointment.Status.PENDING) {
                System.out.println("Doctor: " + appointment.getDoctorId() + " Date: " + appointment.getDate()
                    + " Time: " + appointment.getTimeSlot());
                found = true;
            }
        }
        if (!found) {
            System.out.println("No available appointment slots at the moment.");
        }
    }
    public static void cancelAppointment(String patientId, Appointment appointment) {
        // Find the appointment in the list and update its status
        for (Appointment appt : appointments) {
        	
        	if (appt.getStatus() == Appointment.Status.CANCELED) {
        	    System.out.println("This appointment is already canceled.");
        	    return;
        	}
        	
            if (appt.equals(appointment) && patientId.equals(appt.getPatientId())) {
                appt.setStatus(Appointment.Status.PENDING);
                appt.setPatientId(null); // Clear the patient ID
                System.out.println("Appointment with Doctor: " + appt.getDoctorId() +
                        " on " + appt.getDate() + " at " + appt.getTimeSlot() + " has been cancelled.");
                saveAppointmentsToCSV(); // Save the updated list to CSV
                return;
            }
        }
        System.out.println("Error: Appointment not found or already cancelled.");
    }
    
    public static boolean validateTimeSlot(String timeSlot) {
        // List of valid time slots from 10:00 to 18:00 with 1-hour increments
        String[] validTimeSlots = {"10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00"};

        // Check if the input time slot is valid
        for (String validSlot : validTimeSlots) {
            if (validSlot.equals(timeSlot)) {
                return true; // Valid time slot
            }
        }

        // If the time slot is not in the valid list
        return false; 
    }
    
    public static boolean rescheduleAppointment(String patientId, Appointment oldAppointment, Appointment newSlot) {
        if (newSlot.isAvailable()) {
            // First cancel the old appointment
            oldAppointment.setStatus(Appointment.Status.PENDING);
            oldAppointment.setPatientId(null);

            // Now set the new appointment
            newSlot.setPatientId(patientId);
            newSlot.setStatus(Appointment.Status.REQUESTED);
            appointments.add(newSlot);  // Add new slot to the list
            saveAppointmentsToCSV();    // Save changes
            return true;
        } else {
            System.out.println("Selected slot is no longer available.");
            return false;
        }
    }

    public static void processDoctorAppointmentRequests(String doctorId) {
        System.out.println("\nHandling Appointment Requests:");
        List<Appointment> appointments = getAppointments();

        // Filter appointments for the specific doctor with status 'REQUESTED'
        List<Appointment> pendingAppointments = new ArrayList<>();
        for (Appointment appointment : appointments) {
            if (appointment.getDoctorId().equals(doctorId) && appointment.getStatus() == Appointment.Status.REQUESTED) {
                pendingAppointments.add(appointment);
            }
        }

        // Check if there are no pending appointment requests
        if (pendingAppointments.isEmpty()) {
            System.out.println("No appointments to accept or decline.");
            return;
        }

        // Display the pending appointments for the doctor
        for (int i = 0; i < pendingAppointments.size(); i++) {
            Appointment appointment = pendingAppointments.get(i);
            System.out.println((i + 1) + ". Date: " + appointment.getDate() + 
                               " Time: " + appointment.getTimeSlot() + 
                               " Patient: " + appointment.getPatientId());
        }

        // Prompt doctor to accept or decline an appointment
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter appointment number to accept/decline: ");
        int choice = scanner.nextInt() - 1;
        scanner.nextLine();

        // Validate the user's choice
        if (choice >= 0 && choice < pendingAppointments.size()) {
            Appointment selectedAppointment = pendingAppointments.get(choice);
            System.out.print("Accept (y/n)? ");
            String response = scanner.nextLine();
            selectedAppointment.setStatus(response.equalsIgnoreCase("y") ? Appointment.Status.CONFIRMED : Appointment.Status.DECLINED);
            saveAppointmentsToCSV();
            System.out.println("Appointment updated.");
        } else {
            System.out.println("Invalid appointment number.");
        }
    }

    public static boolean isAvailabilityConflict(String doctorId, String date, String timeSlot) {
        List<Appointment> appointmentsForValidation = getAppointmentsFromCSVForValidation();

        for (Appointment appointment : appointmentsForValidation) {
            if (appointment.getDoctorId().equals(doctorId) &&
                appointment.getDate().equals(date) &&
                appointment.getTimeSlot().equals(timeSlot)) {
                return true; // Conflict found
            }
        }
        return false; // No conflict
    }

    public static void handleDoctorAvailability(String doctorId, String date, String timeSlot) {
        // Check if the availability already exists
        if (isAvailabilityConflict(doctorId, date, timeSlot)) {
            System.out.println("Error: You have already set availability for this date and time.");
            return;
        }

        // If no conflict, add the new appointment
        Appointment appointment = new Appointment(date, timeSlot, Appointment.Status.PENDING);
        appointment.setDoctorId(doctorId);
        addAppointment(appointment);
        saveAppointmentsToCSV(); // Save to CSV file
        System.out.println("Availability set successfully!");
    }

    public static void handlePatientScheduling(String patientId, Appointment selectedAppointment) {
        selectedAppointment.setPatientId(patientId);
        selectedAppointment.setStatus(Appointment.Status.REQUESTED);
        saveAppointmentsToCSV();
    }

    // Method to load appointments from CSV only for validation purposes
    public static List<Appointment> getAppointmentsFromCSVForValidation() {
        List<Appointment> appointmentsForValidation = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(APPOINTMENTS_FILE))) {
            String line;

            while ((line = reader.readLine()) != null) {
                // Split the line into columns
                String[] data = line.split(",");
                
                // Ensure there are enough columns (doctorId, patientId, date, timeSlot, status, service, medication, medStatus, notes)
                if (data.length < 9) {
                    continue;
                }
                
                // Extract and trim the fields from the CSV
                String doctorId = data[0].trim();
                String patientId = data[1].trim();
                String date = data[2].trim();
                String timeSlot = data[3].trim();
                String statusStr = data[4].trim();
                String service = data[5].trim();
                String medication = data[6].trim();
                String medStatus = data[7].trim();
                String notes = data[8].trim();

                // Parse the status safely
                Appointment.Status status;
                try {
                    status = Appointment.Status.valueOf(statusStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid status found in CSV: " + statusStr + ". Defaulting to PENDING.");
                    status = Appointment.Status.PENDING; // Default status
                }

                // Create the Appointment object
                Appointment appointment = new Appointment(date, timeSlot, status);
                appointment.setDoctorId(doctorId);
                appointment.setPatientId(patientId);

                // Create the AppointmentOutcome object
                AppointmentOutcome outcome = new AppointmentOutcome(service, medication, medStatus, notes);
                appointment.setOutcome(outcome);

                appointmentsForValidation.add(appointment);
            }
        } catch (IOException e) {
            System.out.println("Error reading appointments from CSV: " + e.getMessage());
        }
        return appointmentsForValidation;
    }
}
