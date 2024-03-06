package Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {

    // Contain log file location
    private static final String logLocation = System.getProperty("user.dir") + File.separator + "src" + File.separator + "Log" + File.separator;

    // Contain server log file location
    private static final String serverLogLocation = logLocation + "Server" + File.separator;

    // Contain user log file location
    private static final String userLogLocation  = logLocation + "Users" + File.separator;

    private static final String DATE_FORMAT = "yyyy-MM-dd hh:mm:ss a";

    // use to print user log
    public static void userLog(String userID, String... messages) throws IOException {
        log(userLogLocation, userID + ".txt", messages);
    }

    // use to print server log
    public static void serverLog(String serverID, String... messages) throws IOException {
        log(serverLogLocation, getServerFileName(serverID), messages);
    }

    // use to delete log file
    public static void deleteLogFile(String ID) {
        String fileName = userLogLocation + ID + ".txt";
        File file = new File(fileName);
        if (file.exists()) {
            try {
                boolean deleted = file.delete();
                if (deleted) {
                    System.out.println("User Log File deleted successfully: " + fileName);
                } else {
                    System.out.println("Failed to delete User Log file: " + fileName);
                }
            } catch (SecurityException e) {
                System.out.println("Security Exception occurred while deleting User Log file: " + e.getMessage());
            }
        } else {
            System.out.println("File does not exist: " + fileName);
        }
    }

    // write and print logs in log file
    private static void log(String directory, String fileName, String... messages) throws IOException {
        File logFile = new File(directory + fileName);
        try (PrintWriter printWriter = new PrintWriter(new FileWriter(logFile, true))) {
            printWriter.println(getFormattedDate() + " " + String.join(" | ", messages));
        }
    }

    // get current date
    private static String getFormattedDate() {
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        return dateFormat.format(new Date());
    }

    // locate different server log file to write logs
    private static String getServerFileName(String serverID) {
        return switch (serverID.toUpperCase()) {
            case "MTL" -> "Montreal.txt";
            case "SHE" -> "Sherbrooke.txt";
            case "QUE" -> "Quebec.txt";
            default -> "UnknownServer.txt";
        };
    }


}
