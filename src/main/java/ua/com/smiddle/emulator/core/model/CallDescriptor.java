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


    //Constructors
    public CallDescriptor() {
    }

    public CallDescriptor(int connectionCallID, AgentDescriptor agentDescriptor, CallState callState) {
        this.connectionCallID = connectionCallID;
        this.agentDescriptor = agentDescriptor;
        this.callState = callState;
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
}
