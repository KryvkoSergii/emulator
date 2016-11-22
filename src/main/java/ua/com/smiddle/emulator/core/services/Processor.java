package ua.com.smiddle.emulator.core.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import ua.com.smiddle.cti.messages.model.messages.CTI;
import ua.com.smiddle.cti.messages.model.messages.agent_events.*;
import ua.com.smiddle.cti.messages.model.messages.common.Fields;
import ua.com.smiddle.cti.messages.model.messages.common.FloatingField;
import ua.com.smiddle.cti.messages.model.messages.common.PeripheralTypes;
import ua.com.smiddle.cti.messages.model.messages.miscellaneous.ConfigRequestEvent;
import ua.com.smiddle.cti.messages.model.messages.miscellaneous.ConfigRequestKeyEvent;
import ua.com.smiddle.cti.messages.model.messages.miscellaneous.PGStatusCodes;
import ua.com.smiddle.cti.messages.model.messages.session_management.*;
import ua.com.smiddle.emulator.core.model.AgentDescriptor;
import ua.com.smiddle.emulator.core.model.ServerDescriptor;
import ua.com.smiddle.emulator.core.util.LoggerUtil;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * @author srg on 22.11.16.
 * @project emulator
 */
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
    private Socket socket;
    private Transport transport;
    private boolean isEstablished = false;

    public Processor() {
    }

    //Getters and setters
    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }


    //Methods
    @PostConstruct
    private void init() {
        logger.logAnyway(module, "initialized...");
    }

    @Override
    public void run() {
        logger.logAnyway(module, "started...");
        transport = buildNewTransport(socket);
        while (!isInterrupted()) {
            processIncomingMessages(transport);
        }

    }

    private void processIncomingMessages(Transport transport) {
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
                    processOpenReq(openReq);
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
                    processQUERY_AGENT_STATE_REQ(queryAgentStateReq);
                    break;
                }
                case CTI.MSG_SET_AGENT_STATE_REQ: {
                    SetAgentStateReq setAgentStateReq = SetAgentStateReq.deserializeMessage(inputMessage);
                    logger.logMore_1(module, directionIn + setAgentStateReq.toString());
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
                    processCloseReq(closeReq);
                    break;
                }
                case CTI.MSG_MONITOR_START_REQ: {
                    MonitorStartReq monitorStartReq = MonitorStartReq.deserializeMessage(inputMessage);
                    logger.logMore_1(module, directionIn + monitorStartReq.toString());
                    processMONITOR_START_REQ(monitorStartReq);
                    break;
                }
                case CTI.MSG_MONITOR_STOP_REQ: {
                    MonitorStopReq monitorStopReq = MonitorStopReq.deserializeMessage(inputMessage);
                    logger.logMore_1(module, directionIn + monitorStopReq.toString());
                    processMONITOR_STOP_REQ(monitorStopReq);
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


    private Transport buildNewTransport(Socket socket) {
        Transport transport = context.getBean(Transport.class);
        transport.setSocket(socket);
        transport.start();
        return transport;
    }

    private void processOpenReq(Object message) throws Exception {
        OpenReq openReq = (OpenReq) message;
        ServerDescriptor serverDescriptor = new ServerDescriptor();

        serverDescriptor.setServiceMask(openReq.getServicesMask());
        serverDescriptor.setCallMsgMask(openReq.getCallMsgMask());
        serverDescriptor.setAgentStateMask(openReq.getAgentStateMask());
        serverDescriptor.setIdleTimeout(openReq.getIdleTimeout());
        if (openReq.getFloatingFields() != null) {
            for (FloatingField ff : openReq.getFloatingFields()) {
                if (ff.getTag() == Fields.TAG_CLIENT_ID.getTagId()) {
                    serverDescriptor.setClientID(ff.getData());
                } else if (ff.getTag() == Fields.TAG_CLIENT_PASSWORD.getTagId()) {
                    serverDescriptor.setClientPassword(ff.getData());
                }
            }
        }
        Integer monitoringID = pool.getMonitoringID().getAndIncrement();
        pool.getClientConnectionHolder().put(monitoringID, serverDescriptor);

        OpenConf openConf = new OpenConf();
        openConf.setInvokeId(openReq.getInvokeId());
        openConf.setServicesGranted(0x26);
        openConf.setMonitorId(monitoringID);
        openConf.setPGStatus(PGStatusCodes.PGS_OPC_DOWN);
        openConf.setICMCentralControllerTime(new Date());
        openConf.setPeripheralOnline((short) 1);
        openConf.setPeripheralType(PeripheralTypes.PT_ENTERPRISE_AGENT);
        openConf.setAgentState(AgentStates.AGENT_STATE_LOGOUT);
        logger.logMore_1(module, directionOut + "processOpenReq: prepared " + openConf);
        transport.getOutput().add(openConf.serializeMessage());
        isEstablished = true;
    }

    private void processMONITOR_START_REQ(Object message) throws Exception {
        MonitorStartReq monitorStartReq = (MonitorStartReq) message;
        MonitorStartConf monitorStartConf = new MonitorStartConf();
        int monitorID = pool.getMonitorID().getAndIncrement();
        monitorStartConf.setInvokeId(monitorStartReq.getInvokeId());
        monitorStartConf.setMonitorId(monitorID);
        logger.logMore_1(module, directionOut + "processMONITOR_START_REQ: prepared " + monitorStartConf);
        transport.getOutput().add(monitorStartConf.serializeMessage());
        /**
         * Доработать мониторы
         */
    }

    private void processMONITOR_STOP_REQ(Object message) throws Exception {
        MonitorStopReq monitorStopReq = (MonitorStopReq) message;
        MonitorStopConf monitorStopConf = new MonitorStopConf();
        monitorStopConf.setInvokeId(monitorStopReq.getInvokeId());
        logger.logMore_1(module, directionOut + "processMONITOR_STOP_REQ: prepared " + monitorStopConf);
        transport.getOutput().add(monitorStopConf.serializeMessage());
        /**
         * Доработать удаление мониторов
         */
    }

    private void processQUERY_AGENT_STATE_REQ(Object message) throws Exception {
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

    private void processCloseReq(Object message) throws Exception {
        CloseReq closeReq = (CloseReq) message;
        CloseConf closeConf = new CloseConf(closeReq.getInvokeId());
        logger.logMore_1(module, directionOut + " processCloseReq: prepared " + closeConf.toString());
        destroyBean();
    }

    @PreDestroy
    public void destroyBean() {
        logger.logAnyway(module, "Shutting down...");
        interrupt();
        transport.interrupt();
        isEstablished = false;
    }
}
