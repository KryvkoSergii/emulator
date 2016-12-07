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
import ua.com.smiddle.emulator.core.model.CallDescriptor;
import ua.com.smiddle.emulator.core.model.CallState;
import ua.com.smiddle.emulator.core.model.UnknownFields;
import ua.com.smiddle.emulator.core.services.Pools;
import ua.com.smiddle.emulator.core.services.agentstates.AgentStateProcessor;
import ua.com.smiddle.emulator.core.services.statistic.Statistic;
import ua.com.smiddle.emulator.core.util.LoggerUtil;

import java.util.ArrayList;
import java.util.Queue;

/**
 * @author ksa on 01.12.16.
 * @project emulator
 */
@Service("CallsProcessorImpl")
@Description("Supports call messages processing and adding to transport queues")
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
    @Qualifier("AgentStateProcessorImpl")
    private AgentStateProcessor agentStateProcessor;
    @Autowired
    @Qualifier("Statistic")
    private Statistic statistic;


    //===============METHODS================================
    public void processIncomingACDCallList(Queue<Integer> connectionCallIdQueue) {
        int generatedCallsCount = 0;
        Integer connectionCallId;
        for (AgentDescriptor ad : pool.getAgentMapping().values()) {
            connectionCallId = connectionCallIdQueue.poll();
            if (connectionCallId != null && ad.getState() == AgentStates.AGENT_STATE_AVAILABLE) {
                processIncomingACDCall(connectionCallId, ad);
                generatedCallsCount++;
            }
        }
        logger.logMore_1(module, "processIncomingACDCallList: start processing incoming ACD calls number=" + generatedCallsCount);
    }

    @Async(value = "threadPoolSender")
    public void processIncomingACDCall(int connectionCallId, AgentDescriptor ad) {
        try {
//            if (ad.getState() != AgentStates.AGENT_STATE_AVAILABLE)
//                continue;
            ad.setState(AgentStates.AGENT_STATE_RESERVED);
            pool.getCallsHolder().put(connectionCallId, new CallDescriptor(connectionCallId, ad, CallState.NONE_CALL, System.currentTimeMillis()));
            //установка клиентского сотояния
            agentStateProcessor.processAgentStateEvent(ad);
            //звонки
            BeginCallEvent beginCallEvent = prepareBeginCallEvent(connectionCallId, ad);
            agentStateProcessor.sendMessageToAllSubscribers(beginCallEvent.serializeMessage());
            logger.logMore_1(module, directionOut + beginCallEvent.toString());
            CallDeliveredEvent callDeliveredEvent = prepareCallDeliveredEvent(connectionCallId, ad);
            agentStateProcessor.sendMessageToAllSubscribers(callDeliveredEvent.serializeMessage());
            CallDescriptor callDescriptor = pool.getCallsHolder().get(connectionCallId);
            callDescriptor.setCallState(CallState.DELIVERED_CALL);

            statistic.logCallStatistic(callDescriptor);
            logger.logMore_1(module, directionOut + callDeliveredEvent.toString());
            logger.logMore_1(module, "processIncomingACDCall: process connectionCallId=" + connectionCallId + " for agent=" + ad.getAgentID());
            return;
        } catch (Exception e) {
            logger.logAnyway(module, "processIncomingACDCall: for agent=" + ad.getAgentID() + " throw Exception=" + e.getMessage());
        }
        logger.logMore_1(module, "processIncomingACDCall: unable find agent for connectionCallId=" + connectionCallId);
    }

    @Async(value = "threadPoolSender")
    public void processAnswerCallReq(AnswerCallReq req) throws Exception {
        CallDescriptor callDescriptor = pool.getCallsHolder().get(req.getConnectionCallID());
        AgentDescriptor ad = callDescriptor.getAgentDescriptor();

        AnswerCallConf answerCallConf = new AnswerCallConf(req.getInvokeId());
        agentStateProcessor.sendMessageToAllSubscribers(answerCallConf.serializeMessage());
        logger.logMore_1(module, directionOut + answerCallConf.toString());

        ad.setState(AgentStates.AGENT_STATE_TALKING);
        agentStateProcessor.processAgentStateEvent(ad);

        CallDataUpdateEvent callDataUpdateEvent = prepareCallDataUpdateEvent(req.getConnectionCallID(), pool.getCallsHolder().get(req.getConnectionCallID()).getAgentDescriptor());
        agentStateProcessor.sendMessageToAllSubscribers(callDataUpdateEvent.serializeMessage());
        logger.logMore_1(module, directionOut + callDataUpdateEvent.toString());

        CallEstablishedEvent callEstablishedEvent = prepareCallEstablishedEvent(req.getConnectionCallID(), pool.getCallsHolder().get(req.getConnectionCallID()).getAgentDescriptor());
        agentStateProcessor.sendMessageToAllSubscribers(callEstablishedEvent.serializeMessage());
        logger.logMore_1(module, directionOut + callEstablishedEvent.toString());
        callDescriptor.setCallState(CallState.ACTIVE_CALL);
        statistic.logCallStatistic(callDescriptor);
    }

    @Async(value = "threadPoolSender")
    public void processClearCallReq(ClearCallReq req) throws Exception {
        ClearCallConf clearCallConf = new ClearCallConf(req.getInvokeId());
        agentStateProcessor.sendMessageToAllSubscribers(clearCallConf.serializeMessage());
        logger.logMore_1(module, directionOut + clearCallConf.toString());
        processCallEnd(req.getConnectionCallID());
    }

    @Async(value = "threadPoolSender")
    public void processACDCallsEndByCustomer(int connectionCallId) {
        try {
            processCallEnd(connectionCallId);
        } catch (Exception e) {
            logger.logMore_1(module, "processACDCallsEndByCustomer: throw Exception=" + e.getMessage());
        }
    }


    //===============PRIVATE METHODS================================
    private void processCallEnd(int connectionCallId) throws Exception {
        CallDescriptor callDescriptor = pool.getCallsHolder().get(connectionCallId);
        AgentDescriptor ad = callDescriptor.getAgentDescriptor();

        int duration = (int) (System.currentTimeMillis() - callDescriptor.getCallStart()) / 1000;

        logger.logMore_1(module, "processCallEnd: processing call end connectionCallId=" + connectionCallId + " agentId="
                + ad.getAgentID() + " call duration=" + duration + "sec");

        CallClearedEvent callClearedEvent = prepareCallClearedEvent(connectionCallId, ad);
        agentStateProcessor.sendMessageToAllSubscribers(callClearedEvent.serializeMessage());
        logger.logMore_1(module, directionOut + callClearedEvent.toString());

        ad.setState(AgentStates.AGENT_STATE_WORK_READY);
        agentStateProcessor.processAgentStateEvent(ad);

        EndCallEvent endCallEvent = prepareEndCallEvent(connectionCallId, ad);
        agentStateProcessor.sendMessageToAllSubscribers(endCallEvent.serializeMessage());
        logger.logMore_1(module, directionOut + endCallEvent.toString());

        callDescriptor.setCallState(CallState.CLEARED_CALL);
        callDescriptor.setCallStart(duration);
        statistic.logCallStatistic(callDescriptor);
        removeCalls(callDescriptor.getConnectionCallID());
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
        c.setMonitorId(ad.getMonitorID());
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
        c.setMonitorId(ad.getMonitorID());
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
        c.setMonitorId(ad.getMonitorID());
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
        c.setLocalConnectionState(LocalConnectionState.LCS_CONNECT);
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
        if (cd != null) {
            logger.logMore_1(module, "removeCalls: removed call=" + connectionCallId
                    + " state=" + cd.getCallState() + " agent=" + cd.getAgentDescriptor().getAgentID());
            putProcessedCallsToStatistic(cd);
        } else logger.logMore_1(module, "removeCalls: call id=" + connectionCallId + " not exists");
    }

    private void putProcessedCallsToStatistic(CallDescriptor cd) {
        cd.setAgentDescriptor(null);
        statistic.getCallDescriptors().add(cd);
    }
}
