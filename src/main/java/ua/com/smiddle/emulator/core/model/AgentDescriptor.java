package ua.com.smiddle.emulator.core.model;

import ua.com.smiddle.cti.messages.model.messages.agent_events.AgentStates;

import java.io.Serializable;

/**
 * @author srg on 22.11.16.
 * @project emulator
 */
public class AgentDescriptor implements Serializable{
    static final long serialVersionUID = 42L;
    private AgentStates state;
    private String AgentID;
    private String AgentPassword;
    private String AgentInstrument;
    private Integer MonitorID;


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

    public String getAgentPassword() {
        return AgentPassword;
    }

    public void setAgentPassword(String agentPassword) {
        AgentPassword = agentPassword;
    }

    public String getAgentInstrument() {
        return AgentInstrument;
    }

    public void setAgentInstrument(String agentInstrument) {
        AgentInstrument = agentInstrument;
    }

    public Integer getMonitorID() {
        return MonitorID;
    }

    public void setMonitorID(Integer monitorID) {
        MonitorID = monitorID;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AgentDescriptor{");
        sb.append("state=").append(state);
        sb.append(", AgentID='").append(AgentID).append('\'');
        sb.append(", AgentPassword='").append(AgentPassword).append('\'');
        sb.append(", AgentInstrument='").append(AgentInstrument).append('\'');
        sb.append(", MonitorID=").append(MonitorID);
        sb.append('}');
        return sb.toString();
    }
}
