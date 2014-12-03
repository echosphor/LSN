package exception;

public class LSNException extends RuntimeException {

	private static final long serialVersionUID = -6688498901727757328L;

	public LSNException() {
        super();
    }
    
    public LSNException(String msg) {
        super(msg);
    }
    
    public LSNException(Throwable cause) {
        super(cause);
    }

    public LSNException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
