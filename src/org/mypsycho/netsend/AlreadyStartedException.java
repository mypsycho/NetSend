package org.mypsycho.netsend;

public class AlreadyStartedException extends IllegalStateException {

    /**
     * Constructs an AlreadyStartedException with the specified detail
     * message.  A detail message is a String that describes this particular
     * exception.
     *
     * @param s the String that contains a detailed message
     */
    public AlreadyStartedException(String s) {
        super(s);
    }
}
