package ua.com.smiddle.emulator.core.services.scenario;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ua.com.smiddle.emulator.core.services.calls.CallsProcessor;

/**
 * @author ksa on 01.12.16.
 * @project emulator
 */
@Component("Scenario")
public class Scenario {
    @Autowired
    @Qualifier("CallsProcessorImpl")
    private CallsProcessor callsProcessor;

    @Scheduled(fixedRate = 15*60*1000)
    private void generateCalls(){
        callsProcessor.processIncomingACDCall();
    }
}
