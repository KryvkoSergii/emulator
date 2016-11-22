package ua.com.smiddle.emulator.core.services;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import ua.com.smiddle.emulator.core.model.AgentDescriptor;
import ua.com.smiddle.emulator.core.model.ServerDescriptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author srg on 22.11.16.
 * @project emulator
 */
@Service("Pools")
@Scope("singleton")
public class Pools {
    //<MonitoringID,ServerDescriptor> OPEN_REQ
    Map<Integer, ServerDescriptor> clientConnectionHolder = new ConcurrentHashMap();
    //<MonitorID,ServerDescriptor> MONITOR_START_REQ
    Map<Integer, ServerDescriptor> monitorsHolder = new ConcurrentHashMap();
    //<AgentInstrument,ServerDescriptor>
    Map<String, AgentDescriptor> instrumentMapping = new ConcurrentHashMap();
    private AtomicInteger monitoringID = new AtomicInteger();
    //<MonitorID,ServerDescriptor> MONITOR_START_REQ
    private AtomicInteger monitorID = new AtomicInteger();


    //Getters and setters
    public Map getClientConnectionHolder() {
        return clientConnectionHolder;
    }

    public AtomicInteger getMonitoringID() {
        return monitoringID;
    }

    public void setMonitoringID(AtomicInteger monitoringID) {
        this.monitoringID = monitoringID;
    }

    public void setClientConnectionHolder(Map<Integer, ServerDescriptor> clientConnectionHolder) {
        this.clientConnectionHolder = clientConnectionHolder;
    }

    public Map<String, AgentDescriptor> getInstrumentMapping() {
        return instrumentMapping;
    }

    public void setInstrumentMapping(Map<String, AgentDescriptor> instrumentMapping) {
        this.instrumentMapping = instrumentMapping;
    }

    public Map<Integer, ServerDescriptor> getMonitorsHolder() {
        return monitorsHolder;
    }

    public void setMonitorsHolder(Map<Integer, ServerDescriptor> monitorsHolder) {
        this.monitorsHolder = monitorsHolder;
    }

    public AtomicInteger getMonitorID() {
        return monitorID;
    }

    public void setMonitorID(AtomicInteger monitorID) {
        this.monitorID = monitorID;
    }
}
