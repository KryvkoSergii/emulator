package ua.com.smiddle.emulator.core.model;

import ua.com.smiddle.cti.messages.model.messages.agent_events.AgentStates;

/**
 * @author srg on 22.11.16.
 * @project emulator
 */
public class AgentDescriptor {
    private AgentStates state;
    private String AgentID;


    public AgentStates getState() {
        return state;
    }

    public void setState(AgentStates state) {
        this.state = state;
    }

    public String getAgentID() {
        return AgentID;
    }

    public void setAgentID(String agentID) {
        AgentID = agentID;
    }
}
