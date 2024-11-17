import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static CSVUtility patientCSV = new CSVUtility("Patient_List.csv");
    private static CSVUtility staffCSV = new CSVUtility("Staff_List.csv");
    private static PasswordManager patientPasswordManager = new PasswordManager(patientCSV);
    private static PasswordManager staffPasswordManager = new PasswordManager(staffCSV);
    private static List<User> patientUsers = new ArrayList<>();

    public static void main(String[] args) {
    	 AppointmentManager.loadAppointmentsFromCSV("appointments.csv");
        UserInitialization userInitialization = new UserInitialization(patientCSV, staffCSV);
		userInitialization.initializeUsers();

    	loadPatientsFromCSV();
        displayLoginMenu();
        
    }
    
    public static List<User> loadPatientsFromCSV() {
        patientUsers.clear(); // Clear the list before reloading
        try (BufferedReader br = new BufferedReader(new FileReader("Patient_List.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("Patient ID")) continue; // Skip header line
                Patient patient = Patient.fromCSV(line);
                patientUsers.add(patient);  // Add the patient to the list
              
            }
        } catch (IOException e) {
            System.out.println("Error reading patient CSV: " + e.getMessage());
        }
        return patientUsers;
    }
    
    
    

    private static void displayLoginMenu() {
        Scanner scanner = new Scanner(System.in);
        try {
            while (true) {
            	System.out.println("  _    _   _    _    ____  ");
            	System.out.println(" | |  | | | \\  / |  / __|  ");
            	System.out.println(" | |__| | |  \\/  | / /__   ");
            	System.out.println(" |  __  | | |\\/| | \\__  \\  ");
            	System.out.println(" | |  | | | |  | |  ___) | ");
            	System.out.println(" |_|  |_| |_|  |_| |____/  ");

                // Display Welcome Message
                System.out.println("\nWelcome to the Hospital Management System!\n");
                
                // Display a border line
                System.out.println("===============================");
                
                // Display menu options
                System.out.println("Please enter your choice to continue:");
                System.out.println("1. Login");
                System.out.println("2. Exit");
                System.out.print("Choose an option: ");

                if (scanner.hasNextInt()) {
                    int choice = scanner.nextInt();
                    scanner.nextLine(); // Consume newline

                    switch (choice) {
                        case 1:
                            System.out.print("User ID: ");
                            String userID = scanner.nextLine();
                            System.out.print("Password: ");
                            String password = scanner.nextLine();

                            User user = authenticateUser(userID, password);
                            if (user != null) {
                                displayPasswordChangeMenu(user);
                            } else {
                                System.out.println("Invalid User ID or Password. Please try again.");
                            }
                            break;
                        case 2:
                            System.out.println("Terminating program.");
                            return; // Exit the program
                        default:
                            System.out.println("Invalid option. Please try again.");
                            break;
                    }
                } else {
                    System.out.println("Invalid input. Please enter a number.");
                    scanner.nextLine(); // Consume invalid input
                }
            }
        } finally {
            scanner.close();
        }
    }


    private static User authenticateUser(String userID, String password) {
        List<String[]> patientData = patientCSV.readCSV();
        List<String[]> staffData = staffCSV.readCSV();

        // Find the index of the "Hashed Password" and "Salt" headers in patient CSV
        int hashedPasswordIndex = -1;
        int saltIndex = -1;
        String[] patientHeaders = patientData.get(0);
        for (int i = 0; i < patientHeaders.length; i++) {
            if (patientHeaders[i].equals("Hashed Password")) {
                hashedPasswordIndex = i;
            } else if (patientHeaders[i].equals("Salt")) {
                saltIndex = i;
            }
        }

        if (hashedPasswordIndex == -1 || saltIndex == -1) {
            System.err.println("Hashed Password or Salt header not found in patient CSV.");
            return null;
        }

        // Process patient data
        for (int i = 1; i < patientData.size(); i++) {
            String[] user = patientData.get(i);
            if (user[0].equals(userID)) {
                String storedPassword = user[hashedPasswordIndex] + ":" + user[saltIndex];
                try {
                    if (PasswordUtils.validatePassword(password, storedPassword)) {
                        return new Patient(userID, user[hashedPasswordIndex], user[saltIndex], user[1], user[2], user[3], user[4], user[5]);
                    }
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    e.printStackTrace();
                }
            }
        }

        // Find the index of the "Hashed Password" and "Salt" headers in staff CSV
        hashedPasswordIndex = -1;
        saltIndex = -1;
        String[] staffHeaders = staffData.get(0);
        for (int i = 0; i < staffHeaders.length; i++) {
            if (staffHeaders[i].equals("Hashed Password")) {
                hashedPasswordIndex = i;
            } else if (staffHeaders[i].equals("Salt")) {
                saltIndex = i;
            }
        }

        if (hashedPasswordIndex == -1 || saltIndex == -1) {
            System.err.println("Hashed Password or Salt header not found in staff CSV.");
            return null;
        }

        // Process staff data
        for (int i = 1; i < staffData.size(); i++) {
            String[] user = staffData.get(i);
            if (user[0].equals(userID)) {
                String storedPassword = user[hashedPasswordIndex] + ":" + user[saltIndex];
                try {
                    if (PasswordUtils.validatePassword(password, storedPassword)) {
                        User.Role role = determineUserRole(userID);
                        switch (role) {
                            case ADMINISTRATOR:
                                return new Administrator(userID, user[hashedPasswordIndex], user[saltIndex], user[1]);
                            case DOCTOR:
                                return new Doctor(userID, user[hashedPasswordIndex], user[saltIndex], user[1], user[3], Integer.parseInt(user[4]));
                            case PHARMACIST:
                                return new Pharmacist(userID, user[hashedPasswordIndex], user[saltIndex], user[1]);
                        }
                    }
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
    private static User.Role determineUserRole(String userID) {
        // Logic to determine user role based on userID
        // For demonstration, we'll assume userID starting with 'A' is Administrator, 'D' is Doctor, 'P' is Pharmacist, and 'T' is Patient
        if (userID.startsWith("A")) {
            return User.Role.ADMINISTRATOR;
        } else if (userID.startsWith("D")) {
            return User.Role.DOCTOR;
        } else if (userID.startsWith("P")) {
            return User.Role.PHARMACIST;
        } else {
            return User.Role.PATIENT;
        }
    }

    private static void displayPasswordChangeMenu(User user) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
        	 System.out.println("\n" + "=".repeat(40));
        	    System.out.println(" ".repeat(10) + "WELCOME TO HMS");
        	    System.out.println("=".repeat(40));
        	    System.out.println("Hello, " + user.getName() + "!\n");
        	    
        	    System.out.println("What would you like to do?");
        	    System.out.println(" ".repeat(10) + "╔════════════════════════╗");
        	    System.out.println(" ".repeat(10) + "║   1. Change Password   ║");
        	    System.out.println(" ".repeat(10) + "║   2. Display Menu      ║");
        	    System.out.println(" ".repeat(10) + "║   3. Logout            ║");
        	    System.out.println(" ".repeat(10) + "╚════════════════════════╝");
        	    System.out.print("\nChoose an option (1-3): ");
            
            if (scanner.hasNextInt()) {
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                switch (choice) {
                    case 1:
                        PasswordManager passwordManager = user.role == User.Role.PATIENT ? patientPasswordManager : staffPasswordManager;
                        user.changePassword(scanner, passwordManager);
                        break;
                    case 2:
                        user.displayRoleSpecificMenu();
                        break;
                    case 3:
                        System.out.println("Logged out.");
                        return; // Return to the login menu
                    default:
                        System.out.println("Invalid option. Please try again.");
                        break;
                }
            } else {
                System.out.println("Invalid input. Please enter a number.");
                scanner.nextLine(); // Consume invalid input
            }
        }
    }
    public static List<User> getPatientsList() {
        return patientUsers;
    }
}
