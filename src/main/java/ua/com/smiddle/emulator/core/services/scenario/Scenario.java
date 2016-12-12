package ua.com.smiddle.emulator.core.services.scenario;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ua.com.smiddle.emulator.core.model.UnknownFields;
import ua.com.smiddle.emulator.core.pool.Pools;
import ua.com.smiddle.emulator.core.services.processing.calls.CallsProcessor;
import ua.com.smiddle.emulator.core.util.LoggerUtil;

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
    @Autowired
    private Environment env;

    @Scheduled(initialDelay = 25 * 1000, fixedRate = 10 * 1000)
    private void generateCalls() {
        if (Boolean.valueOf(env.getProperty("connector.ganerate.call"))) {
            if (logger.getDebugLevel() > 0)
                logger.logAnyway(module, "start generating ACD calls from=" + UnknownFields.ANI + " to=" + UnknownFields.IVR);
            callsProcessor.processIncomingACDCallList();
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
        }
    }

}
