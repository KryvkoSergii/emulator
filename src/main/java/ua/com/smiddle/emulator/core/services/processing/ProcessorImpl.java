package ua.com.smiddle.emulator.core.services.processing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ua.com.smiddle.cti.messages.model.messages.CTI;
import ua.com.smiddle.cti.messages.model.messages.agent_events.*;
import ua.com.smiddle.cti.messages.model.messages.calls.AnswerCallReq;
import ua.com.smiddle.cti.messages.model.messages.calls.ClearCallReq;
import ua.com.smiddle.cti.messages.model.messages.common.Fields;
import ua.com.smiddle.cti.messages.model.messages.common.FloatingField;
import ua.com.smiddle.cti.messages.model.messages.common.PeripheralTypes;
import ua.com.smiddle.cti.messages.model.messages.miscellaneous.ConfigRequestEvent;
import ua.com.smiddle.cti.messages.model.messages.miscellaneous.ConfigRequestKeyEvent;
import ua.com.smiddle.cti.messages.model.messages.miscellaneous.PGStatusCodes;
import ua.com.smiddle.cti.messages.model.messages.session_management.*;
import ua.com.smiddle.emulator.AgentDescriptor;
import ua.com.smiddle.emulator.core.model.ServerDescriptor;
import ua.com.smiddle.emulator.core.model.UnknownFields;
import ua.com.smiddle.emulator.core.pool.Pools;
import ua.com.smiddle.emulator.core.services.processing.agentstates.AgentStateProcessor;
import ua.com.smiddle.emulator.core.services.processing.calls.CallsProcessor;
import ua.com.smiddle.emulator.core.services.prototype.bean.Transport;
import ua.com.smiddle.emulator.core.util.LoggerUtil;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author srg on 22.11.16.
 * @project emulator
 */
@Service("ProcessorImpl")
public class ProcessorImpl extends Thread implements Processor {
    private static final String module = "Processor";
    private static final String directionIn = "CTI-Client -> CTI: ";
    private static final String directionOut = "CTI-Client <- CTI: ";
    @Autowired
    @Qualifier("LoggerUtil")
    private LoggerUtil logger;
    @Autowired
    @Qualifier("Pools")
    private Pools pool;
    @Autowired
    @Qualifier("CallsProcessorImpl")
    private CallsProcessor callsProcessor;
    @Autowired
    @Qualifier("AgentStateProcessorImpl")
    private AgentStateProcessor agentStateProcessor;
    @Autowired
    private Environment env;
    private byte messageQueueType;
    private final AtomicInteger messageReadCounter = new AtomicInteger();
    private BlockingQueue<byte[]> input;


    //Getters ang setters
    public AtomicInteger getMessageReadCounter() {
        return messageReadCounter;
    }


    //Methods
    @PostConstruct
    private void init() {
        messageQueueType = Byte.valueOf(env.getProperty("connector.transport.queuetype"));
        logger.logAnyway(module, "initialized...");
        start();
    }

    @Override
    public void run() {
        logger.logAnyway(module, "started...");
        input = pool.getInputMessages();
        while (!isInterrupted()) {
            try {
                if (messageQueueType == 0) getIncomingMessageFromSeparateServerDescriptor();
                else getIncomingMessageFromSameQueue();
            } catch (Exception e) {
                logger.logAnyway(module, "run: throw Exception=" + e.getMessage());
            }
        }
    }

    private void getIncomingMessageFromSameQueue() throws Exception {
        processIncomingMessage(null, input.take());
    }

    @Deprecated
    private void getIncomingMessageFromSeparateServerDescriptor() {
        if (pool.getSubscribers().isEmpty())
            try {
                currentThread().sleep(100);
            } catch (InterruptedException e) {
            }
        pool.getSubscribers().stream().forEach(serverDescriptor -> {
            checkAndRemoveServerDescriptor(serverDescriptor);
            if (!serverDescriptor.getTransport().getInput().isEmpty())
                processIncomingMessagesFromSeparateServerDescriptor(serverDescriptor);
        });
    }

    @Deprecated
    private void processIncomingMessagesFromSeparateServerDescriptor(ServerDescriptor sd) {
        Transport transport = sd.getTransport();
        try {
            if (transport.getInput().isEmpty()) return;
            messageReadCounter.getAndIncrement();
            byte[] inputMessage = transport.getInput().take();
            processIncomingMessage(sd, inputMessage);
        } catch (Exception e) {
            logger.logAnyway(module, "processIncomingMessagesFromSeparateServerDescriptor: throw Exception=" + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async("threadPoolSender")
    private void processIncomingMessage(ServerDescriptor sd, byte[] inputMessage) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(inputMessage, 4, 8);
        int code = buffer.getInt();
        switch (code) {
            case CTI.MSG_HEARTBEAT_REQ: {
                HeartbeatReq heartbeatReq = HeartbeatReq.deserializeMessage(inputMessage);
                logger.logMore_2(module, directionIn + heartbeatReq.toString());
                HeartbeatConf heartbeatConf = new HeartbeatConf(heartbeatReq.getInvokeId());
                agentStateProcessor.sendMessageToAllSubscribers(heartbeatConf.serializeMessage());
                if (logger.getDebugLevel() > 2)
                    logger.logMore_2(module, directionOut + heartbeatConf.toString());
                break;
            }
            case CTI.MSG_OPEN_REQ: {
                OpenReq openReq = OpenReq.deserializeMessage(inputMessage);
                if (logger.getDebugLevel() > 1)
                    logger.logMore_1(module, directionIn + openReq.toString());
                processOPEN_REQ(openReq, sd);
                break;
            }
            case CTI.MSG_CLIENT_EVENT_REPORT_REQ: {
                ClientEventReportReq clientEventReportReq = ClientEventReportReq.deserializeMessage(inputMessage);
                if (logger.getDebugLevel() > 1)
                    logger.logMore_1(module, directionIn + clientEventReportReq.toString());
                ClientEventReportConf clientEventReportConf = new ClientEventReportConf();
                clientEventReportConf.setInvokeID(clientEventReportReq.getInvokeID());
                break;
            }
            case CTI.MSG_QUERY_AGENT_STATE_REQ: {
                QueryAgentStateReq queryAgentStateReq = QueryAgentStateReq.deserializeMessage(inputMessage);
                if (logger.getDebugLevel() > 1)
                    logger.logMore_1(module, directionIn + queryAgentStateReq.toString());
                processQueryAgentStateReq(queryAgentStateReq);
                break;
            }
            case CTI.MSG_SET_AGENT_STATE_REQ: {
                SetAgentStateReq setAgentStateReq = SetAgentStateReq.deserializeMessage(inputMessage);
                if (logger.getDebugLevel() > 1)
                    logger.logMore_1(module, directionIn + setAgentStateReq.toString());
                agentStateProcessor.processSetAgentStateReq(setAgentStateReq);
                break;
            }
            case CTI.MSG_CONFIG_REQUEST_KEY_EVENT: {
                ConfigRequestKeyEvent configRequestKeyEvent = ConfigRequestKeyEvent.deserializeMessage(inputMessage);
                if (logger.getDebugLevel() > 1)
                    logger.logMore_1(module, directionIn + configRequestKeyEvent.toString());
                break;
            }
            case CTI.MSG_CONFIG_REQUEST_EVENT: {
                ConfigRequestEvent configRequestEvent = ConfigRequestEvent.deserializeMessage(inputMessage);
                if (logger.getDebugLevel() > 1)
                    logger.logMore_1(module, directionIn + configRequestEvent.toString());
                break;
            }
            case CTI.MSG_CLOSE_REQ: {
                CloseReq closeReq = CloseReq.deserializeMessage(inputMessage);
                if (logger.getDebugLevel() > 1)
                    logger.logMore_1(module, directionIn + closeReq.toString());
                processCloseReq(closeReq, sd);
                break;
            }
            case CTI.MSG_MONITOR_START_REQ: {
                MonitorStartReq monitorStartReq = MonitorStartReq.deserializeMessage(inputMessage);
                if (logger.getDebugLevel() > 1)
                    logger.logMore_1(module, directionIn + monitorStartReq.toString());
                processMONITOR_START_REQ(monitorStartReq);
                break;
            }
            case CTI.MSG_MONITOR_STOP_REQ: {
                MonitorStopReq monitorStopReq = MonitorStopReq.deserializeMessage(inputMessage);
                if (logger.getDebugLevel() > 1)
                    logger.logMore_1(module, directionIn + monitorStopReq.toString());
                processMONITOR_STOP_REQ(monitorStopReq);
                break;
            }
            case CTI.MSG_ANSWER_CALL_REQ: {
                AnswerCallReq answerCallReq = AnswerCallReq.deserializeMessage(inputMessage);
                if (logger.getDebugLevel() > 1)
                    logger.logMore_1(module, directionIn + answerCallReq.toString());
                callsProcessor.processAnswerCallReq(answerCallReq);
                break;
            }
            case CTI.MSG_CLEAR_CALL_REQ: {
                ClearCallReq clearCallReq = ClearCallReq.deserializeMessage(inputMessage);
                if (logger.getDebugLevel() > 1)
                    logger.logMore_1(module, directionIn + clearCallReq.toString());
                callsProcessor.processClearCallReq(clearCallReq);
                break;
            }
            default: {
                logger.logAnyway(module, "processIncomingMessagesFromSeparateServerDescriptor: unrecognized message" + Arrays.toString(inputMessage));
            }
        }
    }

    private void processOPEN_REQ(Object message, ServerDescriptor sd) throws Exception {
        sd = (sd != null) ? sd : findServerDescriptor(CTI.MSG_OPEN_REQ);
        if (sd == null) {
            logger.logAnyway(module, directionOut + "processOpenReq: can't find ServerDescriptor for new OpenReq");
            return;
        }

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
        agentStateProcessor.sendMessageToAllSubscribers(openConf.serializeMessage());
        if (logger.getDebugLevel() > 1)
            logger.logMore_1(module, directionOut + "processOpenReq: prepared " + openConf);
    }

    private void processMONITOR_START_REQ(Object message) throws Exception {
        MonitorStartReq monitorStartReq = (MonitorStartReq) message;
        MonitorStartConf monitorStartConf = new MonitorStartConf();
        Integer monitorID = pool.getMonitorID().getAndIncrement();
        monitorStartConf.setInvokeId(monitorStartReq.getInvokeId());
        monitorStartConf.setMonitorId(monitorID);
        agentStateProcessor.sendMessageToAllSubscribers(monitorStartConf.serializeMessage());
        if (logger.getDebugLevel() > 1)
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

    private void processMONITOR_STOP_REQ(Object message) throws Exception {
        MonitorStopReq monitorStopReq = (MonitorStopReq) message;
        MonitorStopConf monitorStopConf = new MonitorStopConf();
        monitorStopConf.setInvokeId(monitorStopReq.getInvokeId());
        agentStateProcessor.sendMessageToAllSubscribers(monitorStopConf.serializeMessage());
        if (logger.getDebugLevel() > 1)
            logger.logMore_1(module, directionOut + "processMONITOR_STOP_REQ: prepared " + monitorStopConf);
        //находит и удаляет монитор в AgentID, удаляет запись
        try {
            String instrument = findMonitorIDinPool(monitorStopReq.getMonitorId());
            //очистка InstrumentMapping пула
            AgentDescriptor ad = pool.getInstrumentMapping().get(instrument);
            if (ad != null) ad.setMonitorID(0);
            //очистка MonitorsHolder пула
            pool.getMonitorsHolder().remove(instrument);
            if (logger.getDebugLevel() > 1)
                logger.logMore_1(module, "processMONITOR_STOP_REQ: removed MonitorId=" + monitorStopReq.getMonitorId() + " for instrument=" + instrument);
        } catch (Exception e) {
            if (logger.getDebugLevel() > 1)
                logger.logMore_1(module, "processMONITOR_STOP_REQ: request but not found for MonitorId=" + monitorStopReq.getMonitorId() + " throw Exception=" + e.getMessage());
        }
    }

    public String findMonitorIDinPool(Integer monitorId) {
        Optional<String> instrument = pool.getMonitorsHolder().entrySet().stream()
                .filter(map -> monitorId.equals(map.getValue()))
                .map(Map.Entry::getKey)
                .findAny();
        return instrument.get();
    }

    private void processQueryAgentStateReq(Object message) throws Exception {
        QueryAgentStateReq queryAgentStateReq = (QueryAgentStateReq) message;
        QueryAgentStateConf queryAgentStateConf = new QueryAgentStateConf();
        queryAgentStateConf.setInvokeId(queryAgentStateReq.getInvokeId());
        queryAgentStateConf.setNumSkillGroups(UnknownFields.NumSkillGroups);
        queryAgentStateConf.setMrdid(queryAgentStateReq.getMrdid());
        queryAgentStateConf.setNumTasks(UnknownFields.NumTasks);
        queryAgentStateConf.setAgentMode(UnknownFields.AgentMode);
        queryAgentStateConf.setMaxTaskLimit(UnknownFields.MaxTaskLimit);
        queryAgentStateConf.setAgentIdICMA(queryAgentStateReq.getAgentIdICMA());
        queryAgentStateConf.setFloatingFields(new ArrayList<>());
        if (queryAgentStateReq.getFloatingFields() != null) {
            for (FloatingField ff : queryAgentStateReq.getFloatingFields()) {
                if (ff.getTag() == Fields.TAG_AGENT_INSTRUMENT.getTagId()) {
                    AgentStates state = AgentStates.AGENT_STATE_UNKNOWN;
                    if (pool.getInstrumentMapping().containsKey(ff.getData())) {
                        AgentDescriptor agentDescriptor = pool.getInstrumentMapping().get(ff.getData());
                        state = agentDescriptor.getState();
                        queryAgentStateConf.getFloatingFields().add(new FloatingField(Fields.TAG_AGENT_ID.getTagId(), agentDescriptor.getAgentID()));
                    }
                    queryAgentStateConf.setAgentState(state);
                    queryAgentStateConf.setAgentAvailabilityStatus(state.ordinal());
                }
            }
        }
        agentStateProcessor.sendMessageToAllSubscribers(queryAgentStateConf.serializeMessage());
        if (logger.getDebugLevel() > 1)
            logger.logMore_1(module, directionOut + "processQueryAgentStateReq: prepared " + queryAgentStateConf);
    }

    private void processCloseReq(Object message, ServerDescriptor sd) throws Exception {
        CloseReq closeReq = (CloseReq) message;
        CloseConf closeConf = new CloseConf(closeReq.getInvokeId());
        if (logger.getDebugLevel() > 1)
            logger.logMore_1(module, directionOut + " processCloseReq: prepared " + closeConf.toString());
        agentStateProcessor.sendMessageToAllSubscribers(closeConf.serializeMessage());
    }

    @PreDestroy
    public void destroyBean() {
        logger.logAnyway(module, "Shutting down...");
        interrupt();
        for (ServerDescriptor sd : pool.getSubscribers()) {
            sd.getTransport().interrupt();
        }
    }

    @Scheduled(initialDelay = -1, fixedDelay = 20 * 1000)
    private void clearSubscribers() {
        for (Iterator iterator = pool.getSubscribers().iterator(); iterator.hasNext(); ) {
            ServerDescriptor sd = (ServerDescriptor) iterator.next();
            if (sd.getTransport().isDone() || !checkTimeOut(sd)) {
                removeServerDescriptor(sd, "clearSubscribers");
            }
        }
    }

    private boolean checkTimeOut(ServerDescriptor sd) {
        //not started yet
        if (sd.getIdleTimeout() == 0) return true;
        //already started
        return System.currentTimeMillis() - sd.getTransport().getLastIncomingMessage() < (sd.getIdleTimeout() * 1000);
    }

    private void removeServerDescriptor(ServerDescriptor sd, String method) {
        sd.getTransport().interrupt();
        logger.logAnyway(module, method + ":Removing ServerDescriptor " + sd.getClientID());
        pool.getSubscribers().remove(sd);
    }

    private void checkAndRemoveServerDescriptor(ServerDescriptor descriptor) {
        if (descriptor.getTransport() == null || descriptor.getTransport().getSocket() == null || descriptor.getTransport().getSocket().isClosed())
            removeServerDescriptor(descriptor, "checkAndRemoveServerDescriptor");
    }

    /**
     * Defines ServerDescriptor, which has register one of MSG_OPEN_REQ or MSG_CLOSE_REQ message;
     * Uses in case of one incoming message queue.
     *
     * @param CTIMessageType
     * @return
     */
    private ServerDescriptor findServerDescriptor(int CTIMessageType) {
        for (Iterator<ServerDescriptor> subscriberIterator = pool.getSubscribers().iterator(); subscriberIterator.hasNext(); ) {
            ServerDescriptor serverDescriptor = subscriberIterator.next();
            switch (CTIMessageType) {
                case CTI.MSG_OPEN_REQ: {
                    if (serverDescriptor.getTransport().isOpenReqDetected()) {
                        serverDescriptor.getTransport().setOpenReqDetected(false);
                        return serverDescriptor;
                    }
                }
                case CTI.MSG_CLOSE_REQ: {
                    if (serverDescriptor.getTransport().isOpenReqDetected()) {
                        serverDescriptor.getTransport().setOpenReqDetected(false);
                        return serverDescriptor;
                    }
                }
            }
        }
        return null;
    }
}