package ua.com.smiddle.emulator.core.services.calls;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Description;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ua.com.smiddle.cti.messages.model.messages.agent_events.AgentStates;
import ua.com.smiddle.cti.messages.model.messages.calls.*;
import ua.com.smiddle.cti.messages.model.messages.calls.events.*;
import ua.com.smiddle.cti.messages.model.messages.common.Fields;
import ua.com.smiddle.cti.messages.model.messages.common.FloatingField;
import ua.com.smiddle.cti.messages.model.messages.common.PeripheralTypes;
import ua.com.smiddle.cti.messages.model.messages.miscellaneous.EventDeviceTypes;
import ua.com.smiddle.emulator.AgentDescriptor;
import ua.com.smiddle.emulator.core.model.*;
import ua.com.smiddle.emulator.core.services.AgentStateEventProcessor;
import ua.com.smiddle.emulator.core.services.Pools;
import ua.com.smiddle.emulator.core.util.LoggerUtil;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author ksa on 01.12.16.
 * @project emulator
 */
@Service("CallsProcessorImpl")
@Description("Supports call sending")
public class CallsProcessorImpl implements CallsProcessor {
    private final String module = "CallsProcessorImpl";
    private final String directionIn = "CTI-Client -> CTI: ";
    private final String directionOut = "CTI-Client <- CTI: ";
    @Autowired
    @Qualifier("LoggerUtil")
    private LoggerUtil logger;
    @Autowired
    @Qualifier("Pools")
    private Pools pool;
    @Autowired
    @Qualifier("AgentStateEventProcessor")
    private AgentStateEventProcessor stateEventProcessor;

    @Async(value = "threadPoolSender")
    public void processIncomingACDCall(int connectionCallId, ServerDescriptor sd) {
        Collection<AgentDescriptor> agents = pool.getAgentMapping().values();
        for (AgentDescriptor ad : agents) {
            try {
                if (ad.getState() != AgentStates.AGENT_STATE_AVAILABLE) continue;
                pool.getCallsHolder().put(connectionCallId, new CallDescriptor(connectionCallId, ad, CallState.NONE_CALL));
                //установка клиентского сотояния
                ad.setState(AgentStates.AGENT_STATE_RESERVED);
                stateEventProcessor.getAgentEventQueue().add(new AgentEvent(ad, sd));
                //звонки
                BeginCallEvent beginCallEvent = prepareBeginCallEvent(connectionCallId, ad);
                sd.getTransport().getOutput().add(beginCallEvent.serializeMessage());
                logger.logMore_1(module, directionOut + beginCallEvent.toString());
                CallDeliveredEvent callDeliveredEvent = prepareCallDeliveredEvent(connectionCallId, ad);
                sd.getTransport().getOutput().add(callDeliveredEvent.serializeMessage());
                pool.getCallsHolder().get(connectionCallId).setCallState(CallState.DELIVERED_CALL);
                logger.logMore_1(module, directionOut + callDeliveredEvent.toString());
                break;
            } catch (Exception e) {
                logger.logAnyway(module, "processIncomingACDCall: for agent=" + ad.getAgentID() + " throw Exception=" + e.getMessage());
            }
        }
    }

    @Async(value = "threadPoolSender")
    public void processAnswerCallReq(AnswerCallReq req, ServerDescriptor sd) throws Exception {
        CallDescriptor cd = pool.getCallsHolder().get(req.getConnectionCallID());
        AgentDescriptor ad = cd.getAgentDescriptor();

        AnswerCallConf answerCallConf = new AnswerCallConf(req.getInvokeId());
        sd.getTransport().getOutput().add(answerCallConf.serializeMessage());
        logger.logMore_1(module, directionOut + answerCallConf.toString());

        ad.setState(AgentStates.AGENT_STATE_TALKING);
        stateEventProcessor.getAgentEventQueue().add(new AgentEvent(ad, sd));

        CallDataUpdateEvent callDataUpdateEvent = prepareCallDataUpdateEvent(req.getConnectionCallID(), pool.getCallsHolder().get(req.getConnectionCallID()).getAgentDescriptor());
        sd.getTransport().getOutput().add(callDataUpdateEvent.serializeMessage());
        logger.logMore_1(module, directionOut + callDataUpdateEvent.toString());

        CallEstablishedEvent callEstablishedEvent = prepareCallEstablishedEvent(req.getConnectionCallID(), pool.getCallsHolder().get(req.getConnectionCallID()).getAgentDescriptor());
        sd.getTransport().getOutput().add(callDataUpdateEvent.serializeMessage());
        logger.logMore_1(module, directionOut + callEstablishedEvent.toString());
        cd.setCallState(CallState.ACTIVE_CALL);
    }

    @Async(value = "threadPoolSender")
    public void processClearCallReq(ClearCallReq req, ServerDescriptor sd) throws Exception {
        CallDescriptor cd = pool.getCallsHolder().get(req.getConnectionCallID());
        AgentDescriptor ad = cd.getAgentDescriptor();

        ClearCallConf clearCallConf = new ClearCallConf(req.getInvokeId());
        sd.getTransport().getOutput().add(clearCallConf.serializeMessage());
        logger.logMore_1(module, directionOut + clearCallConf.toString());

        CallClearedEvent callClearedEvent = prepareCallClearedEvent(req.getConnectionCallID(), ad);
        sd.getTransport().getOutput().add(clearCallConf.serializeMessage());
        logger.logMore_1(module, directionOut + callClearedEvent.toString());

        ad.setState(AgentStates.AGENT_STATE_AVAILABLE);
        stateEventProcessor.getAgentEventQueue().add(new AgentEvent(ad, sd));

        EndCallEvent endCallEvent = prepareEndCallEvent(req.getConnectionCallID(), ad);
        sd.getTransport().getOutput().add(endCallEvent.serializeMessage());
        logger.logMore_1(module, directionOut + callClearedEvent.toString());

        cd.setCallState(CallState.CLEARED_CALL);
        removeCalls(cd.getConnectionCallID());
    }

    private EndCallEvent prepareEndCallEvent(int connectionCallId, AgentDescriptor ad) {
        EndCallEvent c = new EndCallEvent();
        c.setMonitorId(ad.getMonitorID());
        c.setPeripheralId(UnknownFields.PeripheralID);
        c.setPeripheralType(PeripheralTypes.PT_SIEMENS_9006);
        c.setConnectionDeviceIDType(ConnectionDeviceIDTypes.CONNECTION_ID_STATIC);
        c.setConnectionCallID(connectionCallId);
        c.setFloatingFields(new ArrayList<>());
        c.getFloatingFields().add(new FloatingField(Fields.TAG_CONNECTION_DEVID.getTagId(), UnknownFields.ANI));
        return c;
    }

    private CallClearedEvent prepareCallClearedEvent(int connectionCallId, AgentDescriptor ad) {
        CallClearedEvent c = new CallClearedEvent();
        c.setMonitorId(connectionCallId);
        c.setPeripheralId(UnknownFields.PeripheralID);
        c.setPeripheralType(PeripheralTypes.PT_SIEMENS_9006);
        c.setConnectionDeviceIDType(ConnectionDeviceIDTypes.CONNECTION_ID_STATIC);
        c.setConnectionCallID(connectionCallId);
        c.setLocalConnectionState(LocalConnectionState.LCS_NULL);
        c.setEventCause(EventCause.CEC_NONE);
        c.setFloatingFields(new ArrayList<>());
        c.getFloatingFields().add(new FloatingField(Fields.TAG_CONNECTION_DEVID.getTagId(), UnknownFields.ANI));
        return c;
    }

    private CallEstablishedEvent prepareCallEstablishedEvent(int connectionCallId, AgentDescriptor ad) {
        CallEstablishedEvent c = new CallEstablishedEvent();
        c.setMonitorId(connectionCallId);
        c.setPeripheralId(UnknownFields.PeripheralID);
        c.setPeripheralType(PeripheralTypes.PT_SIEMENS_9006);
        c.setConnectionDeviceIDType(ConnectionDeviceIDTypes.CONNECTION_ID_STATIC);
        c.setConnectionCallID(connectionCallId);
        c.setLineHandle(UnknownFields.LineHandle);
        c.setLineType(LineTypes.LINETYPE_INBOUND_ACD);
        c.setServiceNumber(UnknownFields.ServiceNumber);
        c.setServiceID(UnknownFields.ServiceNumber);
        c.setSkillGroupNumber(UnknownFields.SkillGroupNumber);
        c.setSkillGroupID(UnknownFields.SkillGroupID);
        c.setSkillGroupPriority(UnknownFields.SkillGroupPriority);
        c.setAnsweringDeviceType(EventDeviceTypes.DEVICE_IDENTIFIER);
        c.setCallingDeviceType(EventDeviceTypes.DEVICE_IDENTIFIER);
        c.setCalledDeviceType(EventDeviceTypes.DEVICE_IDENTIFIER);
        c.setLastRedirectDeviceType(EventDeviceTypes.DEVICE_IDENTIFIER);
        c.setLocalConnectionState(LocalConnectionState.LCS_CONNECT);
        c.setEventCause(EventCause.CEC_NONE);
        c.setFloatingFields(new ArrayList<>());
        c.getFloatingFields().add(new FloatingField(Fields.TAG_CONNECTION_DEVID.getTagId(), UnknownFields.ANI));
        return c;
    }

    private CallDataUpdateEvent prepareCallDataUpdateEvent(int connectionCallId, AgentDescriptor ad) {
        CallDataUpdateEvent c = new CallDataUpdateEvent();
        c.setMonitorId(ad.getMonitorID());
        c.setPeripheralId(UnknownFields.PeripheralID);
        c.setPeripheralType(PeripheralTypes.PT_SIEMENS_9006);
        c.setNumCTIClients(UnknownFields.NumCTIClients);
        c.setNumNamedVariables(UnknownFields.NumNamedVariables);
        c.setNumNamedArrays(UnknownFields.NumNamedArrays);
        c.setCallType(CallTypes.CALLTYPE_PREROUTE_ACD_IN);
        c.setConnectionDeviceIDType(ConnectionDeviceIDTypes.CONNECTION_ID_STATIC);
        c.setConnectionCallID(connectionCallId);
        c.setNewConnectionDeviceIDType(ConnectionDeviceIDTypes.CONNECTION_ID_STATIC);
        c.setNewConnectionCallID(connectionCallId);
        c.setCalledPartyDisposition(UnknownFields.CalledPartyDisposition);
        c.setCampaignID(UnknownFields.CampaignID);
        c.setQueryRuleID(UnknownFields.QueryRuleID);
        c.setFloatingFields(new ArrayList<>());
        c.getFloatingFields().add(new FloatingField(Fields.TAG_CONNECTION_DEVID.getTagId(), UnknownFields.ANI));
        c.getFloatingFields().add(new FloatingField(Fields.TAG_NEW_CONNECTION_DEVID.getTagId(), UnknownFields.ANI));
        c.getFloatingFields().add(new FloatingField(Fields.TAG_ANI.getTagId(), UnknownFields.ANI));
        return c;
    }

    private CallDeliveredEvent prepareCallDeliveredEvent(int connectionCallId, AgentDescriptor ad) {
        CallDeliveredEvent c = new CallDeliveredEvent();
        c.setMonitorId(connectionCallId);
        c.setPeripheralId(UnknownFields.PeripheralID);
        c.setPeripheralType(PeripheralTypes.PT_SIEMENS_9006);
        c.setConnectionDeviceIDType(ConnectionDeviceIDTypes.CONNECTION_ID_STATIC);
        c.setConnectionCallID(connectionCallId);
        c.setLineHandle(UnknownFields.LineHandle);
        c.setLineType(LineTypes.LINETYPE_INBOUND_ACD);
        c.setServiceNumber(UnknownFields.ServiceNumber);
        c.setServiceID(UnknownFields.ServiceNumber);
        c.setSkillGroupNumber(UnknownFields.SkillGroupNumber);
        c.setSkillGroupID(UnknownFields.SkillGroupID);
        c.setSkillGroupPriority(UnknownFields.SkillGroupPriority);
        c.setAlertingDeviceType(EventDeviceTypes.AGENT_DEVICE);
        c.setCallingDeviceType(EventDeviceTypes.DEVICE_IDENTIFIER);
        c.setCalledDeviceType(EventDeviceTypes.DEVICE_IDENTIFIER);
        c.setLastRedirectDeviceType(EventDeviceTypes.DEVICE_IDENTIFIER);
        c.setEventCause(EventCause.CEC_INVALID_ACCOUNT_CODE);
        c.setNumNamedVariables(UnknownFields.NumNamedVariables);
        c.setNumNamedArrays(UnknownFields.NumNamedArrays);
        c.setFloatingFields(new ArrayList<>());
        c.getFloatingFields().add(new FloatingField(Fields.TAG_CONNECTION_DEVID.getTagId(), UnknownFields.ANI));
        c.getFloatingFields().add(new FloatingField(Fields.TAG_ALERTING_DEVID.getTagId(), ad.getAgentInstrument()));
        c.getFloatingFields().add(new FloatingField(Fields.TAG_CALLING_DEVID.getTagId(), UnknownFields.ANI));
        c.getFloatingFields().add(new FloatingField(Fields.TAG_CALLED_DEVID.getTagId(), ad.getAgentInstrument()));
        return c;
    }

    private BeginCallEvent prepareBeginCallEvent(int connectionCallId, AgentDescriptor ad) {
        BeginCallEvent c = new BeginCallEvent();
        c.setMonitorId(ad.getMonitorID());
        c.setPeripheralId(UnknownFields.PeripheralID);
        c.setPeripheralType(PeripheralTypes.PT_SIEMENS_9006);
        c.setNumCTIClients(UnknownFields.NumCTIClients);
        c.setNumNamedVariables(UnknownFields.NumNamedVariables);
        c.setNumNamedArrays(UnknownFields.NumNamedArrays);
        c.setCallType(CallTypes.CALLTYPE_PREROUTE_ACD_IN);
        c.setConnectionDeviceIDType(ConnectionDeviceIDTypes.CONNECTION_ID_STATIC);
        c.setConnectionCallID(connectionCallId);
        c.setCalledPartyDisposition(UnknownFields.CalledPartyDisposition);
        c.setFloatingFields(new ArrayList<>());
        c.getFloatingFields().add(new FloatingField(Fields.TAG_CONNECTION_DEVID.getTagId(), UnknownFields.ANI));
        c.getFloatingFields().add(new FloatingField(Fields.TAG_ANI.getTagId(), UnknownFields.ANI));
        c.getFloatingFields().add(new FloatingField(Fields.TAG_DNIS.getTagId(), ad.getAgentInstrument()));
        c.getFloatingFields().add(new FloatingField(Fields.TAG_DIALED_NUMBER.getTagId(), UnknownFields.IVR));
        return c;
    }

    /**
     * add statistic
     *
     * @param connectionCallId
     */
    private void removeCalls(int connectionCallId) {
        CallDescriptor cd = pool.getCallsHolder().remove(connectionCallId);
        if (cd != null)
            logger.logMore_1(module, "removeCalls: removed call=" + connectionCallId
                    + " state=" + cd.getCallState() + " agent=" + cd.getAgentDescriptor().getAgentID());
        else logger.logMore_1(module, "removeCalls: call id=" + connectionCallId + " not exists");
    }


}
