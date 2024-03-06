package Client;

public class CustomException extends Throwable {

    // Custom exception method improve Readability of error for user
    public CustomException(String anErrorOccurred, String errorMessage) {
        System.out.println(anErrorOccurred + errorMessage);
    }
}
