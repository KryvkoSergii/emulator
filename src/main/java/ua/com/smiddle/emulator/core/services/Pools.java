package ua.com.smiddle.emulator.core.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ua.com.smiddle.emulator.AgentDescriptor;
import ua.com.smiddle.emulator.core.model.CallDescriptor;
import ua.com.smiddle.emulator.core.model.ServerDescriptor;
import ua.com.smiddle.emulator.core.util.LoggerUtil;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author srg on 22.11.16.
 * @project emulator
 */
@Service("Pools")
@Scope("singleton")
public class Pools {
    private static final String module = "Pools";
    @Autowired
    @Qualifier("LoggerUtil")
    private LoggerUtil logger;
    private Set<ServerDescriptor> subscribers = ConcurrentHashMap.newKeySet();
    //<MonitoringID,ServerDescriptor> OPEN_REQ
    private Map<Integer, ServerDescriptor> clientConnectionHolder = new ConcurrentHashMap();
    //<MonitoringID,ServerDescriptor> OPEN_REQ
    private Map<Integer, CallDescriptor> callsHolder = new ConcurrentHashMap();
    //<AgentInstrument,MonitorID> MONITOR_START_REQ
    private Map<String, Integer> monitorsHolder = new ConcurrentHashMap();
    //<AgentInstrument,ServerDescriptor>
    private Map<String, AgentDescriptor> instrumentMapping = new ConcurrentHashMap();
    //<AgentID,ServerDescriptor>
    private Map<String, AgentDescriptor> agentMapping = new ConcurrentHashMap();
    private AtomicInteger monitoringID = new AtomicInteger(1);
    //<MonitorID,ServerDescriptor> MONITOR_START_REQ
    private AtomicInteger monitorID = new AtomicInteger(1);


    //Getters and setters
    public Map getClientConnectionHolder() {
        return clientConnectionHolder;
    }

    public void setClientConnectionHolder(Map<Integer, ServerDescriptor> clientConnectionHolder) {
        this.clientConnectionHolder = clientConnectionHolder;
    }

    public AtomicInteger getMonitoringID() {
        return monitoringID;
    }

    public void setMonitoringID(AtomicInteger monitoringID) {
        this.monitoringID = monitoringID;
    }

    public Map<String, AgentDescriptor> getInstrumentMapping() {
        return instrumentMapping;
    }

    public void setInstrumentMapping(Map<String, AgentDescriptor> instrumentMapping) {
        this.instrumentMapping = instrumentMapping;
    }

    public Map<String, Integer> getMonitorsHolder() {
        return monitorsHolder;
    }

    public void setMonitorsHolder(Map<String, Integer> monitorsHolder) {
        this.monitorsHolder = monitorsHolder;
    }

    public AtomicInteger getMonitorID() {
        return monitorID;
    }

    public void setMonitorID(AtomicInteger monitorID) {
        this.monitorID = monitorID;
    }

    public Map<String, AgentDescriptor> getAgentMapping() {
        return agentMapping;
    }

    public void setAgentMapping(Map<String, AgentDescriptor> agentMapping) {
        this.agentMapping = agentMapping;
    }

    public Set<ServerDescriptor> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(Set<ServerDescriptor> subscribers) {
        this.subscribers = subscribers;
    }

    public Map<Integer, CallDescriptor> getCallsHolder() {
        return callsHolder;
    }

    public void setCallsHolder(Map<Integer, CallDescriptor> callsHolder) {
        this.callsHolder = callsHolder;
    }


    //Methods
    @Scheduled(initialDelay = 5 * 1000, fixedDelay = 5 * 1000)
    private void getPoolsState() {
        String s1 = "", s2 = "", s3 = "", s4 = "", s5 = "";

        if (logger.getDebugLevel() > 2) {
            if (agentMapping.size() > 0)
                s1 = " agentMapping=" + agentMapping.entrySet().stream().map(map ->
                        map.getValue().toString()).reduce(" ", String::concat);
            logger.logMore_2(module, "getPoolsState: agentMapping size=" + agentMapping.size() + s1);

            if (instrumentMapping.size() > 0)
                s2 = " instrumentMapping=" + instrumentMapping.entrySet().stream().map(map ->
                        map.getValue().toString()).reduce(" ", String::concat);
            logger.logMore_2(module, "getPoolsState: instrumentMapping size=" + instrumentMapping.size() + s2);

            if (monitorsHolder.size() > 0)
                s3 = " monitorsHolder=" + monitorsHolder.entrySet().stream().map(Object::toString).reduce(" ", String::concat);
            logger.logMore_2(module, "getPoolsState: monitorsHolder size=" + monitorsHolder.size() + s3);

            if (subscribers.size() > 0)
                s4 = " subscribers=" + subscribers.stream().map(ServerDescriptor::toString).reduce(" ", String::concat);
            logger.logMore_2(module, "getPoolsState: subscribers size=" + subscribers.size() + s4);

            if (getCallsHolder().size() > 0)
                s5 = " callsHolder=" + callsHolder.values().stream().map(CallDescriptor::toString).reduce(" ", String::concat);
            logger.logMore_2(module, "callsHolder: callsHolder size=" + callsHolder.size() + s5);
        }

        if (logger.getDebugLevel()>1) {
            logger.logMore_1(module, "getPoolsState: agentMapping size=" + agentMapping.size() + s1);
            logger.logMore_1(module, "getPoolsState: instrumentMapping size=" + instrumentMapping.size() + s2);
            logger.logMore_1(module, "getPoolsState: monitorsHolder size=" + monitorsHolder.size() + s3);
            logger.logMore_1(module, "getPoolsState: subscribers size=" + subscribers.size() + s4);
            logger.logMore_1(module, "callsHolder: callsHolder size=" + callsHolder.size() + s5);
        }
    }
}
