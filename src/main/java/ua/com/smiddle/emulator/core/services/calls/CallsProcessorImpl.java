package ua.com.smiddle.emulator.core.services.calls;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Description;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ua.com.smiddle.cti.messages.model.messages.agent_events.AgentStates;
import ua.com.smiddle.cti.messages.model.messages.calls.CallTypes;
import ua.com.smiddle.cti.messages.model.messages.calls.ConnectionDeviceIDTypes;
import ua.com.smiddle.cti.messages.model.messages.calls.EventCause;
import ua.com.smiddle.cti.messages.model.messages.calls.LineTypes;
import ua.com.smiddle.cti.messages.model.messages.calls.events.BeginCallEvent;
import ua.com.smiddle.cti.messages.model.messages.calls.events.CallDeliveredEvent;
import ua.com.smiddle.cti.messages.model.messages.common.Fields;
import ua.com.smiddle.cti.messages.model.messages.common.FloatingField;
import ua.com.smiddle.cti.messages.model.messages.common.PeripheralTypes;
import ua.com.smiddle.cti.messages.model.messages.miscellaneous.EventDeviceTypes;
import ua.com.smiddle.emulator.AgentDescriptor;
import ua.com.smiddle.emulator.core.model.AgentEvent;
import ua.com.smiddle.emulator.core.model.ServerDescriptor;
import ua.com.smiddle.emulator.core.model.UnknownFields;
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
public class CallsProcessorImpl {
    private final String module = "CallsProcessorImpl";
    @Autowired
    @Qualifier("LoggerUtil")
    private LoggerUtil logger;
    @Autowired
    @Qualifier("Pools")
    private Pools pool;
    @Autowired
    @Qualifier("AgentStateEventProcessor")
    private AgentStateEventProcessor stateEventProcessor;

    @Async(value = "processCalls")
    public void processIncomingCall(int connectionCallId, ServerDescriptor sd) {
        Collection<AgentDescriptor> agents = pool.getAgentMapping().values();

        for (AgentDescriptor ad : agents) {
            try {
                if (ad.getState() != AgentStates.AGENT_STATE_AVAILABLE) continue;
                //установка клиентского сотояния
                ad.setState(AgentStates.AGENT_STATE_RESERVED);
                stateEventProcessor.getAgentEventQueue().add(new AgentEvent(ad, sd));
                //звонки
                BeginCallEvent beginCallEvent = prepareBeginCallEvent(connectionCallId, ad, sd);
                sd.getTransport().getOutput().add(beginCallEvent.serializeMessage());

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
                c.getFloatingFields().add(new FloatingField(Fields.TAG_ANI.getTagId(), UnknownFields.ANI));
                c.getFloatingFields().add(new FloatingField(Fields.TAG_DNIS.getTagId(), ad.getAgentInstrument()));
                c.getFloatingFields().add(new FloatingField(Fields.TAG_DIALED_NUMBER.getTagId(), UnknownFields.IVR));

            } catch (Exception e) {
                logger.logAnyway(module, "processIncomingCall: for agent=" + ad.getAgentID() + " throw Exception=" + e.getMessage());
            }

        }


    }

    private BeginCallEvent prepareBeginCallEvent(int connectionCallId, AgentDescriptor ad, ServerDescriptor sd) {
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


}
