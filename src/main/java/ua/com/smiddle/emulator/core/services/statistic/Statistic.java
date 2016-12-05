package ua.com.smiddle.emulator.core.services.statistic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ua.com.smiddle.emulator.core.model.CallDescriptor;
import ua.com.smiddle.emulator.core.services.Pools;
import ua.com.smiddle.emulator.core.util.LoggerUtil;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author ksa on 05.12.16.
 * @project emulator
 */
@Service("Statistic")
public class Statistic {
    @Autowired
    @Qualifier("Pools")
    private Pools pools;
    @Autowired
    @Qualifier("LoggerUtil")
    private LoggerUtil logger;
    private final String module = "Statistic";
    private List<CallDescriptor> callDescriptors = Collections.synchronizedList(new ArrayList<>());


    //Getters and setters
    public List<CallDescriptor> getCallDescriptors() {
        return callDescriptors;
    }

    public void setCallDescriptors(List<CallDescriptor> callDescriptors) {
        this.callDescriptors = callDescriptors;
    }


    //Methods
    @PostConstruct
    private void init() {
        logger.logAnyway(module, "initialized...");
    }

    @Scheduled(fixedDelay = -1, fixedRate = 60 * 1000)
    private void printProcessedCalls() {
        String s4 = "";
        if (callDescriptors.size() > 0)
            s4 = " callsHolder=" + callDescriptors.stream().map(CallDescriptor::toString).reduce(" ", String::concat);
        logger.logMore_1(module, "callsHolder: callsHolder size=" + callDescriptors.size() + s4);
    }
}
