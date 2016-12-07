package ua.com.smiddle.emulator.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ksa on 07.12.16.
 * @project emulator
 */
public class AgentStatistic {
    private long statisticId;
    private String agentId;
    //long (date), AgentState
    private List<Object[]> agentStates = new ArrayList<>();
    //long (date), CallState
    private List<Object[]> callsStatistic = new ArrayList<>();

    //Constructors
    public AgentStatistic() {
    }

    public AgentStatistic(long statisticId, String agentId) {
        this.statisticId = statisticId;
        this.agentId = agentId;
    }


    //Getters and setters
    public long getStatisticId() {
        return statisticId;
    }

    public void setStatisticId(long statisticId) {
        this.statisticId = statisticId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public List<Object[]> getAgentStates() {
        return agentStates;
    }

    public void setAgentStates(List<Object[]> agentStates) {
        this.agentStates = agentStates;
    }

    public List<Object[]> getCallsStatistic() {
        return callsStatistic;
    }

    public void setCallsStatistic(List<Object[]> callsStatistic) {
        this.callsStatistic = callsStatistic;
    }


    //Methods
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AgentStatistic{");
        sb.append("statisticId=").append(statisticId);
        sb.append(", agentId='").append(agentId).append('\'');
        sb.append(", agentStates=").append(agentStates);
        sb.append(", callsStatistic=").append(callsStatistic);
        sb.append('}');
        return sb.toString();
    }
}
