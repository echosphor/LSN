package exception;

public class PollException extends LSNException {

	private static final long serialVersionUID = -4195923587280164537L;

	public PollException(String msg) {
        super(msg);
    }

    public PollException(Throwable e) {
        super(e);
    }
}
