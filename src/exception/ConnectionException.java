package exception;

public class ConnectionException extends LSNException {
	
	private static final long serialVersionUID = -7089454353286253094L;

	public ConnectionException(String msg) {
        super(msg);
    }

    public ConnectionException(Throwable e) {
        super(e);
    }
}
