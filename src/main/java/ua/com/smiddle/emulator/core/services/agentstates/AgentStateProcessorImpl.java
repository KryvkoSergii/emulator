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
import ua.com.smiddle.emulator.core.model.AgentEvent;
import ua.com.smiddle.emulator.core.model.ServerDescriptor;
import ua.com.smiddle.emulator.core.model.UnknownFields;
import ua.com.smiddle.emulator.core.services.Pools;
import ua.com.smiddle.emulator.core.services.Transport;
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
     * @param event
     * @throws Exception
     */
    @Override
    public void processAgentStateEvent(AgentEvent event) throws Exception {
        switch (event.getAgentDescriptor().getState()) {
            case AGENT_STATE_LOGIN: {
                AgentStateEvent ase = buildAgentStateEvent(event);
                event.getAgentDescriptor().setState(AgentStates.AGENT_STATE_NOT_READY);
                event.getServerDescriptor().getTransport().getOutput().add(ase.serializeMessage());
                processAgentStateEvent(event);
                logger.logMore_1(module, directionOut + ase);
                break;
            }
            case AGENT_STATE_NOT_READY: {
                AgentStateEvent ase = buildAgentStateEvent(event);
                event.getServerDescriptor().getTransport().getOutput().add(ase.serializeMessage());
                logger.logMore_1(module, directionOut + ase);
                break;
            }
            case AGENT_STATE_LOGOUT: {
                AgentStateEvent ase = buildAgentStateEvent(event);
                event.getServerDescriptor().getTransport().getOutput().add(ase.serializeMessage());
                removeAgentInPools(event.getAgentDescriptor());
                logger.logMore_1(module, directionOut + ase);
                break;
            }
            default: {
                AgentStateEvent ase = buildAgentStateEvent(event);
                event.getServerDescriptor().getTransport().getOutput().add(ase.serializeMessage());
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
    public void processSetAgentStateReq(Object message, ServerDescriptor sd) throws Exception {
        Transport transport = sd.getTransport();
        AgentDescriptor tmpAgent = new AgentDescriptor();
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
        transport.getOutput().add(setAgentStateConf.serializeMessage());
        logger.logMore_1(module, directionOut + "processSetAgentStateReq: prepared " + setAgentStateConf);
        processAgentStateEvent(new AgentEvent(tmpAgent, sd));
    }


    //===============PRIVATE METHODS================================
    private AgentStateEvent buildAgentStateEvent(AgentEvent event) {
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

    private void removeAgentInPools(AgentDescriptor tmpAgent) {
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

    private void updateAgentInPools(AgentDescriptor tmpAgent) {
        Integer monitorID = pool.getMonitorsHolder().get(tmpAgent.getAgentInstrument());
        if (monitorID != null)
            tmpAgent.setMonitorID(monitorID);
        if (tmpAgent.getAgentInstrument() != null) {
            if (pool.getInstrumentMapping().containsKey(tmpAgent.getAgentInstrument())) {
                AgentDescriptor a = pool.getInstrumentMapping().get(tmpAgent.getAgentInstrument());
                a.setAgentInstrument(tmpAgent.getAgentInstrument());
                if (tmpAgent.getAgentID() != null)
                    a.setAgentID(tmpAgent.getAgentID());
                a.setMonitorID(tmpAgent.getMonitorID());
                a.setState(tmpAgent.getState());
                logger.logMore_1(module, "updateAgentInPools: updated in InstrumentMapping=" + a.toString());
            } else {
                pool.getInstrumentMapping().put(tmpAgent.getAgentInstrument(), tmpAgent);
                logger.logMore_1(module, "updateAgentInPools: created in InstrumentMapping=" + tmpAgent.toString());
            }
        }

        if (tmpAgent.getAgentID() != null) {
            if (!pool.getAgentMapping().containsKey(tmpAgent.getAgentID())) {
                pool.getAgentMapping().put(tmpAgent.getAgentID(), tmpAgent);
                logger.logMore_1(module, "updateAgentInPools: created in AgentMapping=" + tmpAgent);
            }
        }
    }

}
