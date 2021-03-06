package ua.com.smiddle.emulator.core.services.agentstates;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Description;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ua.com.smiddle.cti.messages.model.messages.agent_events.AgentStateEvent;
import ua.com.smiddle.cti.messages.model.messages.agent_events.AgentStates;
import ua.com.smiddle.cti.messages.model.messages.agent_events.SetAgentStateConf;
import ua.com.smiddle.cti.messages.model.messages.agent_events.SetAgentStateReq;
import ua.com.smiddle.cti.messages.model.messages.common.Fields;
import ua.com.smiddle.cti.messages.model.messages.common.FloatingField;
import ua.com.smiddle.cti.messages.model.messages.common.PeripheralTypes;
import ua.com.smiddle.emulator.AgentDescriptor;
import ua.com.smiddle.emulator.core.model.UnknownFields;
import ua.com.smiddle.emulator.core.services.Pools;
import ua.com.smiddle.emulator.core.services.statistic.Statistic;
import ua.com.smiddle.emulator.core.util.LoggerUtil;

import javax.annotation.PostConstruct;

/**
 * @author ksa on 02.12.16.
 * @project emulator
 */
@Service("AgentStateProcessorImpl")
@Description("Supports agent states requests and events processing, adds to transport queues")
public class AgentStateProcessorImpl implements AgentStateProcessor {
    private static final String module = "AgentStateProcessorImpl";
    private static final String directionIn = "CTI-Client -> CTI: ";
    private static final String directionOut = "CTI-Client <- CTI: ";
    @Autowired
    @Qualifier("LoggerUtil")
    private LoggerUtil logger;
    @Autowired
    @Qualifier("Pools")
    private Pools pool;
    @Autowired
    @Qualifier("Statistic")
    private Statistic statistic;


    //Constructors
    public AgentStateProcessorImpl() {
    }


    //Methods
    @PostConstruct
    private void init() {
        logger.logAnyway(module, "initialized...");
    }

    //===============METHODS================================

    /**
     * Выполняется текущим потоком.
     *
     * @param agentDescriptor
     * @throws Exception
     */
    @Override
    public void processAgentStateEvent(AgentDescriptor agentDescriptor) throws Exception {
        switch (agentDescriptor.getState()) {
            case AGENT_STATE_LOGIN: {
                AgentStateEvent ase = buildAgentStateEvent(agentDescriptor);
                sendMessageToAllSubscribers(ase.serializeMessage());
                statistic.logAgentStatistic(agentDescriptor);
                if (logger.getDebugLevel() > 1)
                    logger.logMore_1(module, directionOut + ase);
                agentDescriptor.setState(AgentStates.AGENT_STATE_NOT_READY);
                processAgentStateEvent(agentDescriptor);
                break;
            }
//            case AGENT_STATE_NOT_READY: {
//                AgentStateEvent ase = buildAgentStateEvent(agentDescriptor);
//                sendMessageToAllSubscribers(ase.serializeMessage());
//                statistic.logAgentStatistic(agentDescriptor);
//                if (logger.getDebugLevel() > 1)
//                    logger.logMore_1(module, directionOut + ase);
//                break;
//            }
            case AGENT_STATE_LOGOUT: {
                AgentStateEvent ase = buildAgentStateEvent(agentDescriptor);
                sendMessageToAllSubscribers(ase.serializeMessage());
                statistic.logAgentStatistic(agentDescriptor);
                removeAgentInPools(agentDescriptor);
                if (logger.getDebugLevel() > 1)
                    logger.logMore_1(module, directionOut + ase);
                break;
            }
            default: {
                AgentStateEvent ase = buildAgentStateEvent(agentDescriptor);
                sendMessageToAllSubscribers(ase.serializeMessage());
                statistic.logAgentStatistic(agentDescriptor);
                if (logger.getDebugLevel() > 1)
                    logger.logMore_1(module, directionOut + ase);
                break;
            }
        }
    }

    /**
     * AgentPassword игнорируется
     * Обрабатывает только запросы из клиентской стороны.
     * Выполняется в отдельном потоке
     *
     * @param message
     * @throws Exception
     */
    @Async("threadPoolSender")
    @Override
    public void processSetAgentStateReq(Object message) throws Exception {
//        Transport transport = sd.getTransport();
        final AgentDescriptor tmpAgent = new AgentDescriptor();
        SetAgentStateReq setAgentStateReq = (SetAgentStateReq) message;
        for (FloatingField ff : setAgentStateReq.getFloatingFields()) {
            if (ff.getTag() == Fields.TAG_AGENT_INSTRUMENT.getTagId())
                //обработка инструмента
                tmpAgent.setAgentInstrument(ff.getData());
            else if (ff.getTag() == Fields.TAG_AGENT_ID.getTagId())
                //обработка AgentID
                tmpAgent.setAgentID(ff.getData());
        }
        tmpAgent.setState(setAgentStateReq.getAgentState());
        updateAgentInPools(tmpAgent);
        SetAgentStateConf setAgentStateConf = new SetAgentStateConf();
        setAgentStateConf.setInvokeID(setAgentStateReq.getInvokeID());
        sendMessageToAllSubscribers(setAgentStateConf.serializeMessage());
        if (logger.getDebugLevel() > 1)
            logger.logMore_1(module, directionOut + "processSetAgentStateReq: prepared " + setAgentStateConf);
        processAgentStateEvent(tmpAgent);
    }

    @Override
    public void sendMessageToAllSubscribers(byte[] message) {
        pool.getSubscribers().forEach(serverDescriptor -> serverDescriptor.getTransport().getOutput().add(message));
    }


    //===============PRIVATE METHODS================================

    private AgentStateEvent buildAgentStateEvent(AgentDescriptor agentDescriptor) {
        AgentStateEvent a = new AgentStateEvent();
        a.setMonitorId(agentDescriptor.getMonitorID());
        a.setPeripheralId(UnknownFields.PeripheralID);
        a.setSessionId(UnknownFields.SessionId);
        a.setPeripheralType(PeripheralTypes.PT_ACMI_ERS);
        a.setSkillGroupState((short) AgentStates.setIntState(agentDescriptor.getState()));
        a.setStateDuration(UnknownFields.StateDuration);
        a.setSkillGroupNumber(UnknownFields.SkillGroupNumber);
        a.setSkillGroupId(UnknownFields.SkillGroupID);
        a.setSkillGroupPriority(UnknownFields.SkillGroupPriority);
        a.setAgentState(agentDescriptor.getState());
        a.setEventReasonCode(UnknownFields.EventReasonCode);
        a.setMrdid(UnknownFields.MRDID);
        a.setNumTasks(UnknownFields.NumTasks);
        a.setAgentMode(UnknownFields.AgentMode);
        a.setAgentIdICMA(UnknownFields.ICMAgentID);
        a.setAgentAvailabilityStatus(UnknownFields.AgentAvailabilityStatus);
        a.setNumTasks(UnknownFields.NumFltSkillGroups);
        return a;
    }

    private void removeAgentInPools(AgentDescriptor tmpAgent) {
        AgentDescriptor inPools = null;
        if (tmpAgent.getAgentInstrument() != null)
            inPools = pool.getInstrumentMapping().remove(tmpAgent.getAgentInstrument());
        if (inPools != null) pool.getAgentMapping().remove(inPools.getAgentID());
        else if (tmpAgent.getAgentID() != null)
            pool.getAgentMapping().remove(tmpAgent.getAgentID());
//        if (tmpAgent.getMonitorID() != null)
//            pool.getMonitorsHolder().remove(tmpAgent.getMonitorID());
        if (logger.getDebugLevel() > 2)
            logger.logMore_2(module, "removed for=" + tmpAgent.getAgentID() + " " + tmpAgent.getAgentInstrument());
    }

    private void updateAgentInPools(AgentDescriptor tmpAgent) {
        Integer monitorID = pool.getMonitorsHolder().get(tmpAgent.getAgentInstrument());
        if (monitorID != null)
            tmpAgent.setMonitorID(monitorID);
        if (tmpAgent.getAgentInstrument() != null) {
            AgentDescriptor a;
            if ((a = pool.getInstrumentMapping().get(tmpAgent.getAgentInstrument())) != null) {
                a.setAgentInstrument(tmpAgent.getAgentInstrument());
                if (tmpAgent.getAgentID() != null)
                    a.setAgentID(tmpAgent.getAgentID());
                a.setMonitorID(tmpAgent.getMonitorID());
                a.setState(tmpAgent.getState());
                if (logger.getDebugLevel() > 1)
                    logger.logMore_1(module, "updateAgentInPools: updated in InstrumentMapping=" + a.toString());
            } else {
                pool.getInstrumentMapping().put(tmpAgent.getAgentInstrument(), tmpAgent);
                if (logger.getDebugLevel() > 1)
                    logger.logMore_1(module, "updateAgentInPools: created in InstrumentMapping=" + tmpAgent.toString());
            }
        }

        if (tmpAgent.getAgentID() != null) {
            if (!pool.getAgentMapping().containsKey(tmpAgent.getAgentID())) {
                pool.getAgentMapping().put(tmpAgent.getAgentID(), tmpAgent);
                if (logger.getDebugLevel() > 1)
                    logger.logMore_1(module, "updateAgentInPools: created in AgentMapping=" + tmpAgent);
            }
        }
    }

}
