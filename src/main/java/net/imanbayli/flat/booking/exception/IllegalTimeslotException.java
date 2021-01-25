package net.imanbayli.flat.booking.exception;

public class IllegalTimeslotException extends RuntimeException{
    public IllegalTimeslotException(String message) {
        super(message);
    }
}
