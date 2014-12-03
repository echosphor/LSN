package exception;

public class PushException extends LSNException {
	
	private static final long serialVersionUID = 950905134256287252L;

	public PushException(String msg) {
        super(msg);
    }

    public PushException(Throwable e) {
        super(e);
    }
}
