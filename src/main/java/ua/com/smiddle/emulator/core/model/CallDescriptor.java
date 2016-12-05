package ua.com.smiddle.emulator.core.model;

import ua.com.smiddle.emulator.AgentDescriptor;

/**
 * @author ksa on 01.12.16.
 * @project emulator
 */
public class CallDescriptor {
    private int connectionCallID;
    private AgentDescriptor agentDescriptor;
    private CallState callState;
    private long callStart;


    //Constructors
    public CallDescriptor() {
    }

    public CallDescriptor(int connectionCallID, AgentDescriptor agentDescriptor, CallState callState, long callStart) {
        this.connectionCallID = connectionCallID;
        this.agentDescriptor = agentDescriptor;
        this.callState = callState;
        this.callStart = callStart;
    }


    //Getters and setters
    public int getConnectionCallID() {
        return connectionCallID;
    }

    public void setConnectionCallID(int connectionCallID) {
        this.connectionCallID = connectionCallID;
    }

    public AgentDescriptor getAgentDescriptor() {
        return agentDescriptor;
    }

    public void setAgentDescriptor(AgentDescriptor agentDescriptor) {
        this.agentDescriptor = agentDescriptor;
    }

    public CallState getCallState() {
        return callState;
    }

    public void setCallState(CallState callState) {
        this.callState = callState;
    }

    public long getCallStart() {
        return callStart;
    }

    public void setCallStart(long callStart) {
        this.callStart = callStart;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CallDescriptor{");
        sb.append("connectionCallID=").append(connectionCallID);
        sb.append(", agentDescriptor=").append(agentDescriptor);
        sb.append(", callState=").append(callState);
        sb.append(", callStart=").append(callStart);
        sb.append('}');
        return sb.toString();
    }
}
