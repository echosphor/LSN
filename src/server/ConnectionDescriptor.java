package server;

import org.apache.mina.core.session.IoSession;

public class ConnectionDescriptor {
    
    private String m_clientID;
    private IoSession m_session;
    private boolean m_cleanSession;
    
    public ConnectionDescriptor(String clientID, IoSession session, boolean cleanSession) {
        this.m_clientID = clientID;
        this.m_session = session;
        this.m_cleanSession = cleanSession;
    }
    
    public boolean isCleanSession() {
        return m_cleanSession;
    }

    public String getClientID() {
        return m_clientID;
    }

    public IoSession getSession() {
        return m_session;
    }
}
