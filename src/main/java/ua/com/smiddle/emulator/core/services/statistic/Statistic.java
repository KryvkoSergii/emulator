package ua.com.smiddle.emulator.core.services.statistic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ua.com.smiddle.emulator.AgentDescriptor;
import ua.com.smiddle.emulator.core.model.AgentStatistic;
import ua.com.smiddle.emulator.core.model.CallDescriptor;
import ua.com.smiddle.emulator.core.pool.Pools;
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
    @Autowired
    private Environment env;
    private final String module = "Statistic";
    //    private List<CallDescriptor> callDescriptors = Collections.synchronizedList(new ArrayList<>());
    private Map<Long, AgentStatistic> agentStatistic = new ConcurrentHashMap<>();
    private AtomicLong agentStatisticIdGenerator = new AtomicLong(1000);
    private volatile long lastCleared = 0;
    private boolean statisticEnabled;


    //Getters and setters

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
        statisticEnabled = Boolean.parseBoolean(env.getProperty("connector.statistic.enabled"));
        logger.logAnyway(module, "initialized: statisticEnabled=" + statisticEnabled);
    }

    public void logAgentStatistic(AgentDescriptor agentDescriptor) {
        if (!statisticEnabled) return;
        AgentStatistic agentStatistic = getAgentStatistic().get(agentDescriptor.getAgentStatisticId());
        long id;
        if (agentStatistic == null) {
            agentStatistic = getAgentStatistic().get(agentDescriptor.getAgentStatisticId());
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
        if (!statisticEnabled) return;
        AgentStatistic as = getAgentStatistic().get(callDescriptor.getAgentDescriptor().getAgentStatisticId());
        if (as != null)
            as.getCallsStatistic().add(new Object[]{System.currentTimeMillis(), callDescriptor.getCallState()});
        else {
            if (logger.getDebugLevel() > 0)
                logger.logMore_0(module, "logCallStatistic: can't find callDescriptor for" + callDescriptor);
        }
    }

    public void clearAgentStatistic(String marker) {
        agentStatistic.clear();
        lastCleared = System.currentTimeMillis();
        if (logger.getDebugLevel() > 0)
            logger.logMore_0(module, marker + ": cleared Agent Statistic");
    }

    @Scheduled(fixedDelay = -1, fixedRate = 2 * 60 * 1000)
    private void clearAgentStatisticScheduled() {
        if (System.currentTimeMillis() - lastCleared > 5 * 60 * 1000) {
            clearAgentStatistic("clearAgentStatisticScheduled");
        }
    }

//    private String findMonitorIDinPool(Integer monitorId) {
//        Optional<String> instrument = pools.getMonitorsHolder().entrySet().stream()
//                .filter(map -> monitorId.equals(map.getValue()))
//                .map(Map.Entry::getKey)
//                .findAny();
//        return instrument.get();
//    }
}
