package ua.com.smiddle.emulator.core.model;

/**
 * @author srg on 23.11.16.
 * @project emulator
 */
public class AgentEvent {
    private AgentDescriptor agentDescriptor;
    private ServerDescriptor serverDescriptor;


    //Constructors
    public AgentEvent() {
    }

    public AgentEvent(AgentDescriptor agentDescriptor, ServerDescriptor serverDescriptor) {
        this.agentDescriptor = agentDescriptor;
        this.serverDescriptor = serverDescriptor;
    }


    //Getters and setters
    public AgentDescriptor getAgentDescriptor() {
        return agentDescriptor;
    }

    public void setAgentDescriptor(AgentDescriptor agentDescriptor) {
        this.agentDescriptor = agentDescriptor;
    }

    public ServerDescriptor getServerDescriptor() {
        return serverDescriptor;
    }

    public void setServerDescriptor(ServerDescriptor serverDescriptor) {
        this.serverDescriptor = serverDescriptor;
    }
}
