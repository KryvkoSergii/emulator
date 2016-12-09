package ua.com.smiddle.emulator.core.services.scenario;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ua.com.smiddle.cti.messages.model.messages.agent_events.AgentStates;
import ua.com.smiddle.emulator.core.model.UnknownFields;
import ua.com.smiddle.emulator.core.services.Pools;
import ua.com.smiddle.emulator.core.services.calls.CallsProcessor;
import ua.com.smiddle.emulator.core.util.LoggerUtil;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author ksa on 01.12.16.
 * @project emulator
 */
@Component("Scenario")
public class Scenario {
    private final String module = "Scenario";
    @Autowired
    @Qualifier("CallsProcessorImpl")
    private CallsProcessor callsProcessor;
    @Autowired
    @Qualifier("Pools")
    private Pools pools;
    @Autowired
    @Qualifier("LoggerUtil")
    private LoggerUtil logger;
    private AtomicInteger connectionCallId = new AtomicInteger(100);
    //    private boolean startCalling = true;
    private Queue incommingCallsQueue = new LinkedBlockingQueue<>();
    @Autowired
    private Environment env;

    @Scheduled(initialDelay = 25 * 1000, fixedRate = 10 * 1000)
    private void generateCalls() {
        if (Boolean.valueOf(env.getProperty("connector.ganerate.call"))) {
            if (logger.getDebugLevel() > 0)
                logger.logAnyway(module, "start generating ACD calls from=" + UnknownFields.ANI + " to=" + UnknownFields.IVR);
//        int initCallCount = 0, callCount = 0;
            final AtomicInteger callCountStart = new AtomicInteger(0);
            pools.getAgentMapping().values().stream()
                    .filter(agentDescriptor -> agentDescriptor.getState() == AgentStates.AGENT_STATE_AVAILABLE)
                    .collect(Collectors.toList())
                    .forEach(agentDescriptor1 -> {
                        incommingCallsQueue.add(connectionCallId.getAndIncrement());
                        callCountStart.getAndIncrement();
                    });
//        for (int i = 0; i < pools.getAgentMapping().size(); i++) {
//            incommingCallsQueue.add(connectionCallId.getAndIncrement());
//            callCount++;
//        }
            callsProcessor.processIncomingACDCallList(incommingCallsQueue);
            if (callCountStart.get() > 0)
                logger.logAnyway(module, "generated calls start number=" + callCountStart.get());
            else {
                if (logger.getDebugLevel() > 1)
                    logger.logMore_0(module, "generated calls start number=" + callCountStart.get());
            }
        } else {
            if (logger.getDebugLevel() > 1)
                logger.logMore_1(module, "generating calls=" + Boolean.valueOf(env.getProperty("connector.ganerate.call")));
        }
    }

    @Scheduled(initialDelay = 40 * 1000, fixedRate = 10 * 1000)
    private void dropCalls() {
        if (Boolean.valueOf(env.getProperty("connector.ganerate.call"))) {
            if (logger.getDebugLevel() > 0) logger.logMore_0(module, "start dropping ACD calls...");
            final AtomicInteger callCountDrop = new AtomicInteger(0);
            pools.getCallsHolder().values().stream()
                    .filter(callDescriptor -> ((System.currentTimeMillis() - callDescriptor.getCallStart()) > 15 * 1000))
                    .collect(Collectors.toList())
                    .forEach(callDescriptor1 -> {
                        callsProcessor.processACDCallsEndByCustomer(callDescriptor1.getConnectionCallID());
                        callCountDrop.incrementAndGet();
                    });
            if (callCountDrop.get() > 0)
                logger.logAnyway(module, "initiated dropped ACD calls number=" + callCountDrop.get());
            else {
                if (logger.getDebugLevel() > 1)
                    logger.logMore_0(module, "initiated dropped ACD calls number=" + callCountDrop.get());
            }
//        for (int connectionCallId : pools.getCallsHolder().keySet()) {
//            callsProcessor.processACDCallsEndByCustomer(connectionCallId);
//            callCountDrop++;
//        }
//        if (callCountDrop > 0) startCalling = true;
        }
    }

}
