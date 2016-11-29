package ua.com.smiddle.emulator.core.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ua.com.smiddle.cti.messages.model.messages.agent_events.AgentStateEvent;
import ua.com.smiddle.cti.messages.model.messages.agent_events.AgentStates;
import ua.com.smiddle.cti.messages.model.messages.common.PeripheralTypes;
import ua.com.smiddle.emulator.AgentDescriptor;
import ua.com.smiddle.emulator.core.model.AgentEvent;
import ua.com.smiddle.emulator.core.model.UnknownFields;
import ua.com.smiddle.emulator.core.util.LoggerUtil;

import javax.annotation.PostConstruct;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author srg on 22.11.16.
 * @project emulator
 */
@Service("AgentStateEventProcessor")
public class AgentStateEventProcessor extends Thread {
    private final String module = "AgentStateEventProcessor";
    private final String directionIn = "CTI-Client -> CTI: ";
    private final String directionOut = "CTI-Client <- CTI: ";
    @Autowired
    @Qualifier("LoggerUtil")
    private LoggerUtil logger;
    @Autowired
    @Qualifier("Pools")
    private Pools pool;
    private BlockingQueue<AgentEvent> agentEventQueue = new LinkedBlockingQueue<>();


    //Constructors
    public AgentStateEventProcessor() {
    }


    //Getters and setters
    public BlockingQueue<AgentEvent> getAgentEventQueue() {
        return agentEventQueue;
    }

    public void setAgentEventQueue(BlockingQueue<AgentEvent> agentEventQueue) {
        this.agentEventQueue = agentEventQueue;
    }


    //Methods
    @PostConstruct
    private void init() {
        logger.logAnyway(module, "initialized...");
        start();
    }

    @Override
    public void run() {
        logger.logAnyway(module, "started...");
        while (!isInterrupted()) {
            try {
                AgentEvent ae = agentEventQueue.take();
                processAGENT_STATE_EVENT(ae);
            } catch (Exception e) {
                logger.logAnyway(module, "throw Exception=" + e.getMessage());
            }

        }
    }

    private void processAGENT_STATE_EVENT(AgentEvent event) throws Exception {
        switch (event.getAgentDescriptor().getState()) {
            case AGENT_STATE_LOGIN: {
                AgentStateEvent ase = buildAGENT_STATE_EVENT(event);
                event.getAgentDescriptor().setState(AgentStates.AGENT_STATE_NOT_READY);
                event.getServerDescriptor().getTransport().getOutput().add(ase.serializeMessage());
                agentEventQueue.add(event);
                logger.logMore_1(module, directionOut + ase);
                break;
            }
            case AGENT_STATE_NOT_READY: {
                AgentStateEvent ase = buildAGENT_STATE_EVENT(event);
                event.getServerDescriptor().getTransport().getOutput().add(ase.serializeMessage());
                logger.logMore_1(module, directionOut + ase);
                break;
            }
            case AGENT_STATE_LOGOUT: {
                AgentStateEvent ase = buildAGENT_STATE_EVENT(event);
                event.getServerDescriptor().getTransport().getOutput().add(ase.serializeMessage());
                removeAgentInPools(event.getAgentDescriptor());
                logger.logMore_1(module, directionOut + ase);
                break;
            }
            default: {
                AgentStateEvent ase = buildAGENT_STATE_EVENT(event);
                event.getServerDescriptor().getTransport().getOutput().add(ase.serializeMessage());
                logger.logMore_1(module, directionOut + ase);
                break;
            }
        }
    }

    private AgentStateEvent buildAGENT_STATE_EVENT(AgentEvent event) {
        AgentStateEvent a = new AgentStateEvent();
        a.setMonitorId(event.getAgentDescriptor().getMonitorID());
        a.setPeripheralId(UnknownFields.PeripheralID);
        a.setSessionId(UnknownFields.SessionId);
        a.setPeripheralType(PeripheralTypes.PT_ACMI_ERS);
        a.setSkillGroupState((short) AgentStates.setIntState(event.getAgentDescriptor().getState()));
        a.setStateDuration(UnknownFields.StateDuration);
        a.setSkillGroupNumber(UnknownFields.SkillGroupNumber);
        a.setSkillGroupId(UnknownFields.SkillGroupID);
        a.setSkillGroupPriority(UnknownFields.SkillGroupPriority);
        a.setAgentState(event.getAgentDescriptor().getState());
        a.setEventReasonCode(UnknownFields.EventReasonCode);
        a.setMrdid(UnknownFields.MRDID);
        a.setNumTasks(UnknownFields.NumTasks);
        a.setAgentMode(UnknownFields.AgentMode);
        a.setAgentIdICMA(UnknownFields.ICMAgentID);
        a.setAgentAvailabilityStatus(UnknownFields.AgentAvailabilityStatus);
        a.setNumTasks(UnknownFields.NumFltSkillGroups);
        return a;
    }

    public void removeAgentInPools(AgentDescriptor tmpAgent) {
        AgentDescriptor inPools = null;
        if (tmpAgent.getAgentInstrument() != null)
            inPools = pool.getInstrumentMapping().remove(tmpAgent.getAgentInstrument());
        if (inPools != null) pool.getAgentMapping().remove(inPools.getAgentID());
        else if (tmpAgent.getAgentID() != null)
            pool.getAgentMapping().remove(tmpAgent.getAgentID());
//        if (tmpAgent.getMonitorID() != null)
//            pool.getMonitorsHolder().remove(tmpAgent.getMonitorID());
        logger.logMore_2(module, "removed for=" + tmpAgent.getAgentID() + " " + tmpAgent.getAgentInstrument());
    }


}
