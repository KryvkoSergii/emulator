package ua.com.smiddle.emulator.core.model;

/**
 * @author srg on 22.11.16.
 * @project emulator
 */
public class ServerDescriptor {
    private String ClientID;
    private String ClientPassword;
    private int idleTimeout;
    private int serviceMask;
    private int callMsgMask;
    private int agentStateMask;

    public ServerDescriptor() {
    }

    public String getClientID() {
        return ClientID;
    }

    public void setClientID(String clientID) {
        ClientID = clientID;
    }

    public String getClientPassword() {
        return ClientPassword;
    }

    public void setClientPassword(String clientPassword) {
        ClientPassword = clientPassword;
    }

    public int getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public int getServiceMask() {
        return serviceMask;
    }

    public void setServiceMask(int serviceMask) {
        this.serviceMask = serviceMask;
    }

    public int getCallMsgMask() {
        return callMsgMask;
    }

    public void setCallMsgMask(int callMsgMask) {
        this.callMsgMask = callMsgMask;
    }

    public int getAgentStateMask() {
        return agentStateMask;
    }

    public void setAgentStateMask(int agentStateMask) {
        this.agentStateMask = agentStateMask;
    }
}
