package ua.com.smiddle.emulator.core.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import ua.com.smiddle.cti.messages.model.messages.CTI;
import ua.com.smiddle.cti.messages.model.messages.agent_events.*;
import ua.com.smiddle.cti.messages.model.messages.common.Fields;
import ua.com.smiddle.cti.messages.model.messages.common.FloatingField;
import ua.com.smiddle.cti.messages.model.messages.common.PeripheralTypes;
import ua.com.smiddle.cti.messages.model.messages.miscellaneous.ConfigRequestEvent;
import ua.com.smiddle.cti.messages.model.messages.miscellaneous.ConfigRequestKeyEvent;
import ua.com.smiddle.cti.messages.model.messages.miscellaneous.PGStatusCodes;
import ua.com.smiddle.cti.messages.model.messages.session_management.*;
import ua.com.smiddle.emulator.AgentDescriptor;
import ua.com.smiddle.emulator.core.exception.EmulatorException;
import ua.com.smiddle.emulator.core.model.AgentEvent;
import ua.com.smiddle.emulator.core.model.ServerDescriptor;
import ua.com.smiddle.emulator.core.model.UnknownFields;
import ua.com.smiddle.emulator.core.util.LoggerUtil;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * @author srg on 22.11.16.
 * @project emulator
 */
@Service("Processor")
public class Processor extends Thread {
    private final String module = "Processor";
    private final String directionIn = "CTI-Client -> CTI: ";
    private final String directionOut = "CTI-Client <- CTI: ";
    @Autowired
    private ApplicationContext context;
    @Autowired
    @Qualifier("LoggerUtil")
    private LoggerUtil logger;
    @Autowired
    @Qualifier("Pools")
    private Pools pool;
    @Autowired
    @Qualifier("AgentStateEventProcessor")
    private AgentStateEventProcessor agentStateEventProcessor;


    //Constructor
    public Processor() {
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
            for (Iterator iterator = pool.getSubscribers().iterator(); iterator.hasNext(); ) {
                ServerDescriptor sd = (ServerDescriptor) iterator.next();
                if (!sd.getTransport().isDone() && checkTimeOut(sd))
                    processIncomingMessages(sd);
                else {
                    sd.destroy();
                    logger.logAnyway(module, "Removing ServerDescriptor " + sd.getClientID());
                    pool.getSubscribers().remove(sd);
                }
            }
        }
    }

    private boolean checkTimeOut(ServerDescriptor sd) {
        //not started yet
        if (sd.getIdleTimeout() == 0) return true;
        //already started
        if (System.currentTimeMillis() - sd.getTransport().getLastIncommingMessage() < (sd.getIdleTimeout() * 1000)) {
            return true;
        } else return false;
    }

    private void processIncomingMessages(ServerDescriptor sd) {
        Transport transport = sd.getTransport();
        try {
            byte[] inputMessage = transport.getInput().poll();
            if (inputMessage == null) return;
            ByteBuffer buffer = ByteBuffer.wrap(inputMessage, 4, 8);
            int code = buffer.getInt();
            switch (code) {
                case CTI.MSG_HEARTBEAT_REQ: {
                    HeartbeatReq heartbeatReq = HeartbeatReq.deserializeMessage(inputMessage);
                    logger.logMore_2(module, directionIn + heartbeatReq.toString());
                    HeartbeatConf heartbeatConf = new HeartbeatConf(heartbeatReq.getInvokeId());
                    transport.getOutput().add(heartbeatConf.serializeMessage());
                    logger.logMore_2(module, directionOut + heartbeatConf.toString());
                    break;
                }
                case CTI.MSG_OPEN_REQ: {
                    OpenReq openReq = OpenReq.deserializeMessage(inputMessage);
                    logger.logMore_1(module, directionIn + openReq.toString());
                    processOPEN_REQ(openReq, sd);
                    break;
                }
                case CTI.MSG_CLIENT_EVENT_REPORT_REQ: {
                    ClientEventReportReq clientEventReportReq = ClientEventReportReq.deserializeMessage(inputMessage);
                    logger.logMore_1(module, directionIn + clientEventReportReq.toString());
                    ClientEventReportConf clientEventReportConf = new ClientEventReportConf();
                    clientEventReportConf.setInvokeID(clientEventReportReq.getInvokeID());
                    break;
                }
                case CTI.MSG_QUERY_AGENT_STATE_REQ: {
                    QueryAgentStateReq queryAgentStateReq = QueryAgentStateReq.deserializeMessage(inputMessage);
                    logger.logMore_1(module, directionIn + queryAgentStateReq.toString());
                    processQUERY_AGENT_STATE_REQ(queryAgentStateReq, sd);
                    break;
                }
                case CTI.MSG_SET_AGENT_STATE_REQ: {
                    SetAgentStateReq setAgentStateReq = SetAgentStateReq.deserializeMessage(inputMessage);
                    logger.logMore_1(module, directionIn + setAgentStateReq.toString());
                    processMSG_SET_AGENT_STATE_REQ(setAgentStateReq, sd);
                    break;
                }
                case CTI.MSG_CONFIG_REQUEST_KEY_EVENT: {
                    ConfigRequestKeyEvent configRequestKeyEvent = ConfigRequestKeyEvent.deserializeMessage(inputMessage);
                    logger.logMore_1(module, directionIn + configRequestKeyEvent.toString());
                    break;
                }
                case CTI.MSG_CONFIG_REQUEST_EVENT: {
                    ConfigRequestEvent configRequestEvent = ConfigRequestEvent.deserializeMessage(inputMessage);
                    logger.logMore_1(module, directionIn + configRequestEvent.toString());
                    break;
                }
                case CTI.MSG_CLOSE_REQ: {
                    CloseReq closeReq = CloseReq.deserializeMessage(inputMessage);
                    logger.logMore_1(module, directionIn + closeReq.toString());
                    processCloseReq(closeReq, sd);
                    break;
                }
                case CTI.MSG_MONITOR_START_REQ: {
                    MonitorStartReq monitorStartReq = MonitorStartReq.deserializeMessage(inputMessage);
                    logger.logMore_1(module, directionIn + monitorStartReq.toString());
                    processMONITOR_START_REQ(monitorStartReq, sd);
                    break;
                }
                case CTI.MSG_MONITOR_STOP_REQ: {
                    MonitorStopReq monitorStopReq = MonitorStopReq.deserializeMessage(inputMessage);
                    logger.logMore_1(module, directionIn + monitorStopReq.toString());
                    processMONITOR_STOP_REQ(monitorStopReq, sd);
                    break;
                }
                default: {
                    logger.logMore_1(module, "processIncomingMessages: unrecognized message" + Arrays.toString(inputMessage));
                }
            }
        } catch (Exception e) {
            logger.logMore_0(module, "processIncomingMessages: throw Exception=" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * AgentPassword игнорируется
     *
     * @param message
     * @throws Exception
     */
    private void processMSG_SET_AGENT_STATE_REQ(Object message, ServerDescriptor sd) throws Exception {
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
//        switch (tmpAgent.getState()) {
//            case AGENT_STATE_LOGOUT:
//                removeAgentInPools(tmpAgent);
//                break;
//            case AGENT_STATE_UNKNOWN:
//                removeAgentInPools(tmpAgent);
//                break;
//            default:
//                updateAgentInPools(tmpAgent);
//                break;
//        }
        updateAgentInPools(tmpAgent);
        SetAgentStateConf setAgentStateConf = new SetAgentStateConf();
        setAgentStateConf.setInvokeID(setAgentStateReq.getInvokeID());
        transport.getOutput().add(setAgentStateConf.serializeMessage());
        logger.logMore_1(module, directionOut + "processMSG_SET_AGENT_STATE_REQ: prepared " + setAgentStateConf);
        agentStateEventProcessor.getAgentEventQueue().add(new AgentEvent(tmpAgent, sd));
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
                if (tmpAgent.getMonitorID() != null)
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

    private void processOPEN_REQ(Object message, ServerDescriptor sd) throws Exception {
        Transport transport = sd.getTransport();
        OpenReq openReq = (OpenReq) message;
        sd.setServiceMask(openReq.getServicesMask());
        sd.setCallMsgMask(openReq.getCallMsgMask());
        sd.setAgentStateMask(openReq.getAgentStateMask());
        sd.setIdleTimeout(openReq.getIdleTimeout());
        if (openReq.getFloatingFields() != null) {
            for (FloatingField ff : openReq.getFloatingFields()) {
                if (ff.getTag() == Fields.TAG_CLIENT_ID.getTagId()) {
                    sd.setClientID(ff.getData());
                } else if (ff.getTag() == Fields.TAG_CLIENT_PASSWORD.getTagId()) {
                    sd.setClientPassword(ff.getData());
                }
            }
        }
        Integer monitoringID = pool.getMonitoringID().getAndIncrement();
        OpenConf openConf = new OpenConf();
        openConf.setInvokeId(openReq.getInvokeId());
        openConf.setServicesGranted(0x26);
        openConf.setMonitorId(monitoringID);
        openConf.setPGStatus(PGStatusCodes.PGS_OPC_DOWN);
        openConf.setICMCentralControllerTime(new Date());
        openConf.setPeripheralOnline((short) 1);
        openConf.setPeripheralType(PeripheralTypes.PT_ENTERPRISE_AGENT);
        openConf.setAgentState(AgentStates.AGENT_STATE_LOGOUT);
        transport.getOutput().add(openConf.serializeMessage());
        logger.logMore_1(module, directionOut + "processOpenReq: prepared " + openConf);
    }

    private void processMONITOR_START_REQ(Object message, ServerDescriptor sd) throws Exception {
        Transport transport = sd.getTransport();
        MonitorStartReq monitorStartReq = (MonitorStartReq) message;
        MonitorStartConf monitorStartConf = new MonitorStartConf();
        Integer monitorID = pool.getMonitorID().getAndIncrement();
        monitorStartConf.setInvokeId(monitorStartReq.getInvokeId());
        monitorStartConf.setMonitorId(monitorID);
        transport.getOutput().add(monitorStartConf.serializeMessage());
        logger.logMore_1(module, directionOut + "processMONITOR_START_REQ: prepared " + monitorStartConf);
        String instrument;
        for (FloatingField ff : monitorStartReq.getFloatingFields()) {
            if (ff.getTag() == Fields.TAG_MONITORED_DEVID_TAG.getTagId()) {
                //создает monitorId в пуле
                instrument = ff.getData();
                pool.getMonitorsHolder().put(instrument, monitorID);
                logger.logMore_2(module, "add monitorId=" + monitorID + " to instrument=" + instrument);
                //порверяет и добавляет monitorId агенту
                AgentDescriptor ad = pool.getInstrumentMapping().get(instrument);
                if (ad != null) {
                    ad.setMonitorID(monitorID);
                    logger.logMore_2(module, "add monitorId=" + monitorID + " to AgentId=" + ad.getAgentID());
                }
            }
        }
    }

    private void processMONITOR_STOP_REQ(Object message, ServerDescriptor sd) throws Exception {
        Transport transport = sd.getTransport();
        MonitorStopReq monitorStopReq = (MonitorStopReq) message;
        MonitorStopConf monitorStopConf = new MonitorStopConf();
        monitorStopConf.setInvokeId(monitorStopReq.getInvokeId());
        transport.getOutput().add(monitorStopConf.serializeMessage());
        logger.logMore_1(module, directionOut + "processMONITOR_STOP_REQ: prepared " + monitorStopConf);

        //находит и удаляет монитор в AgentID, удаляет запись
        try {
            String instrument = findMonitorIDinPool(monitorStopReq.getMonitorId());
            AgentDescriptor ad = pool.getInstrumentMapping().get(instrument);
            if (ad != null) ad.setMonitorID(null);
            pool.getMonitorsHolder().remove(instrument);
            logger.logMore_1(module, "processMONITOR_STOP_REQ: removed MonitorId=" + monitorStopReq.getMonitorId() + " for instrument=" + instrument);
        } catch (Exception e) {
            logger.logMore_1(module, "processMONITOR_STOP_REQ: request but not found for MonitorId=" + monitorStopReq.getMonitorId() + " throw Exception=" + e.getMessage());
        }
    }

    private String findMonitorIDinPool(Integer monitorId) throws Exception {
        Optional<String> instrument = pool.getMonitorsHolder().entrySet().stream()
                .filter(map -> monitorId.equals(map.getValue()))
                .map(map -> map.getKey())
                .findAny();
        return instrument.get();
    }

    private void processQUERY_AGENT_STATE_REQ(Object message, ServerDescriptor sd) throws Exception {
        Transport transport = sd.getTransport();
        QueryAgentStateReq queryAgentStateReq = (QueryAgentStateReq) message;
        QueryAgentStateConf queryAgentStateConf = new QueryAgentStateConf();
        queryAgentStateConf.setInvokeId(queryAgentStateReq.getInvokeId());
        queryAgentStateConf.setNumSkillGroups(UnknownFields.NumSkillGroups);
        queryAgentStateConf.setMrdid(queryAgentStateReq.getMrdid());
        queryAgentStateConf.setNumTasks(UnknownFields.NumTasks);
        queryAgentStateConf.setAgentMode(UnknownFields.AgentMode);
        queryAgentStateConf.setMaxTaskLimit(UnknownFields.MaxTaskLimit);
        queryAgentStateConf.setAgentIdICMA(queryAgentStateReq.getAgentIdICMA());
        if (queryAgentStateReq.getFloatingFields() != null) {
            for (FloatingField ff : queryAgentStateReq.getFloatingFields()) {
                if (ff.getTag() == Fields.TAG_AGENT_INSTRUMENT.getTagId()) {
                    AgentStates state = AgentStates.AGENT_STATE_UNKNOWN;
                    if (pool.getInstrumentMapping().containsKey(ff.getData())) {
                        AgentDescriptor agentDescriptor = pool.getInstrumentMapping().get(ff.getData());
                        state = agentDescriptor.getState();
                        queryAgentStateConf.setFloatingFields(new ArrayList<>());
                        queryAgentStateConf.getFloatingFields().add(new FloatingField(Fields.TAG_AGENT_ID.getTagId(), agentDescriptor.getAgentID()));
                    }
                    queryAgentStateConf.setAgentState(state);
                    queryAgentStateConf.setAgentAvailabilityStatus(state.ordinal());
                }
            }
        }
        transport.getOutput().add(queryAgentStateConf.serializeMessage());
        logger.logMore_1(module, directionOut + "processQUERY_AGENT_STATE_REQ: prepared " + queryAgentStateConf);

    }

    private void processCloseReq(Object message, ServerDescriptor sd) throws Exception {
        Transport transport = sd.getTransport();
        CloseReq closeReq = (CloseReq) message;
        CloseConf closeConf = new CloseConf(closeReq.getInvokeId());
        logger.logMore_1(module, directionOut + " processCloseReq: prepared " + closeConf.toString());
        transport.getOutput().add(closeConf.serializeMessage());
    }

    @PreDestroy
    public void destroyBean() {
        logger.logAnyway(module, "Shutting down...");
        interrupt();
        for (ServerDescriptor sd : pool.getSubscribers()) {
            sd.getTransport().interrupt();
        }
    }

    private ServerDescriptor findServerDescriptor(Transport transport) throws EmulatorException {
        for (ServerDescriptor sd : pool.getSubscribers()) {
            if (sd.getTransport().equals(transport)) return sd;
        }
        throw new EmulatorException("ServerDescriptor can't be defined for " + transport.getSocket().getRemoteSocketAddress());
    }
}
