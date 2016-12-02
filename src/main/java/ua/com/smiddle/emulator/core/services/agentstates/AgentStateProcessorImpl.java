package ua.com.smiddle.emulator.core.services.agentstates;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import ua.com.smiddle.cti.messages.model.messages.agent_events.AgentStateEvent;
import ua.com.smiddle.cti.messages.model.messages.agent_events.AgentStates;
import ua.com.smiddle.cti.messages.model.messages.common.PeripheralTypes;
import ua.com.smiddle.emulator.AgentDescriptor;
import ua.com.smiddle.emulator.core.model.AgentEvent;
import ua.com.smiddle.emulator.core.model.UnknownFields;
import ua.com.smiddle.emulator.core.services.Pools;
import ua.com.smiddle.emulator.core.util.LoggerUtil;

import javax.annotation.PostConstruct;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author ksa on 02.12.16.
 * @project emulator
 */
public class AgentStateProcessorImpl {

    private static final String module = "AgentStateProcessorImpl";
    private static final String directionIn = "CTI-Client -> CTI: ";
    private static final String directionOut = "CTI-Client <- CTI: ";
    @Autowired
    @Qualifier("LoggerUtil")
    private LoggerUtil logger;
    @Autowired
    @Qualifier("Pools")
    private Pools pool;


    //Constructors
    public AgentStateProcessorImpl() {
    }


    //Methods
    @PostConstruct
    private void init() {
        logger.logAnyway(module, "initialized...");
    }

    @Async("threadPoolSender")
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