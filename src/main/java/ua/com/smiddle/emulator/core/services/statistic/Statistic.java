package ua.com.smiddle.emulator.core.services.statistic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ua.com.smiddle.emulator.AgentDescriptor;
import ua.com.smiddle.emulator.core.model.AgentStatistic;
import ua.com.smiddle.emulator.core.model.CallDescriptor;
import ua.com.smiddle.emulator.core.services.Pools;
import ua.com.smiddle.emulator.core.util.LoggerUtil;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

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
    //    private List<CallDescriptor> callDescriptors = Collections.synchronizedList(new ArrayList<>());
    private Map<Long, AgentStatistic> agentStatistic = new ConcurrentHashMap<>();
    private AtomicLong agentStatisticIdGenerator = new AtomicLong(1000);
    private volatile long lastCleared = 0;


    //Getters and setters
//    public List<CallDescriptor> getCallDescriptors() {
//        return callDescriptors;
//    }
//
//    public void setCallDescriptors(List<CallDescriptor> callDescriptors) {
//        this.callDescriptors = callDescriptors;
//    }

    public Map<Long, AgentStatistic> getAgentStatistic() {
        return agentStatistic;
    }

    public void setAgentStatistic(Map<Long, AgentStatistic> agentStatistic) {
        this.agentStatistic = agentStatistic;
    }

    public long getLastCleared() {
        return lastCleared;
    }

    public void setLastCleared(long lastCleared) {
        this.lastCleared = lastCleared;
    }

    //Methods
    @PostConstruct
    private void init() {
        logger.logAnyway(module, "initialized...");
    }

    public void logAgentStatistic(AgentDescriptor agentDescriptor) {
        AgentStatistic agentStatistic = getAgentStatistic().get(agentDescriptor.getAgentStatisticId());
        long id;
        if (agentStatistic == null) {
            //проверка в пуле по агентскому инструменту, и поиск statisticId
            if (agentDescriptor.getAgentInstrument() != null) {
                AgentDescriptor tmp = pools.getInstrumentMapping().get(agentDescriptor.getAgentInstrument());
                agentStatistic = getAgentStatistic().get(tmp != null ? tmp.getAgentStatisticId() : 0);
            }

            //если statisticId нет ни в AgentStatistic, ни в agentInstrument - создается новая
            if (agentStatistic == null) {
                id = agentStatisticIdGenerator.getAndIncrement();
                agentDescriptor.setAgentStatisticId(id);
                agentStatistic = new AgentStatistic(id, agentDescriptor.getAgentID());
                this.agentStatistic.put(id, agentStatistic);
            }
        }
        agentStatistic.getAgentStates().add(new Object[]{System.currentTimeMillis(), agentDescriptor.getState()});
    }

    public void logCallStatistic(CallDescriptor callDescriptor) {
        long id = callDescriptor.getAgentDescriptor().getAgentStatisticId();
        AgentStatistic as = getAgentStatistic().get(id);
        as.getCallsStatistic().add(new Object[]{System.currentTimeMillis(), callDescriptor.getCallState()});
    }

    public void clearAgentStatistic(String marker) {
        agentStatistic.clear();
        lastCleared = System.currentTimeMillis();
        logger.logMore_0(module, marker + ": cleared Agent Statistic");
    }

    //    @Scheduled(fixedDelay = -1, fixedRate = 60 * 1000)
//    private void printProcessedCalls() {
//        String s4 = "";
//        if (callDescriptors.size() > 0)
//            s4 = " callsHolder=" + callDescriptors.stream().map(CallDescriptor::toString).reduce(" ", String::concat);
//        logger.logMore_1(module, "callsHolder: callsHolder size=" + callDescriptors.size() + s4);
//    }

    @Scheduled(fixedDelay = -1, fixedRate = 2 * 60 * 1000)
    private void clearAgentStatisticScheduled() {
        if (System.currentTimeMillis() - lastCleared > 5 * 60 * 1000) {
            clearAgentStatistic("clearAgentStatisticScheduled");
        }
    }
}
