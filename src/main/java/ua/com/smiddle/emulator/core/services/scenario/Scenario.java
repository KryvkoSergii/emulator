package ua.com.smiddle.emulator.core.services.scenario;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ua.com.smiddle.emulator.core.model.UnknownFields;
import ua.com.smiddle.emulator.core.services.Pools;
import ua.com.smiddle.emulator.core.services.calls.CallsProcessor;
import ua.com.smiddle.emulator.core.util.LoggerUtil;

import java.util.concurrent.atomic.AtomicInteger;

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
    private boolean startCalling = true;

    @Scheduled(initialDelay = -1, fixedRate = 15 * 1000)
    private void generateCalls() {
        if (startCalling) {
            logger.logAnyway(module, "start generating ACD calls from=" + UnknownFields.ANI + " to=" + UnknownFields.IVR);
            int initCallCount = 0, callCount = 0;
            for (int i = 0; i < pools.getAgentMapping().size(); i++) {
                callsProcessor.processIncomingACDCall(connectionCallId.getAndIncrement());
                callCount++;
            }
            logger.logAnyway(module, "generated calls number=" + (callCount + initCallCount));
            if (callCount > 0) startCalling = false;
        } else {
            logger.logAnyway(module, "start dropping ACD calls...");
            int callCountDrop = 0;
            for (int connectionCallId : pools.getCallsHolder().keySet()) {
                callsProcessor.processACDCallsEndByCustomer(connectionCallId);
                callCountDrop++;
            }
            if (callCountDrop > 0) startCalling = true;
        }
    }
}
