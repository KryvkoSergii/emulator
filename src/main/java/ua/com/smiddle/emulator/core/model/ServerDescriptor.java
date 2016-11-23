package ua.com.smiddle.emulator.core.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import ua.com.smiddle.emulator.core.services.Transport;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.Socket;

/**
 * @author srg on 22.11.16.
 * @project emulator
 */
public class ServerDescriptor {
    @Autowired
    private ApplicationContext context;
    private String ClientID;
    private String ClientPassword;
    private int idleTimeout;
    private int serviceMask;
    private int callMsgMask;
    private int agentStateMask;
    private Transport transport;
    private Integer monitoringID;


    //Constructor
    public ServerDescriptor() {
    }


    //Getters and setters
    public String getClientID() {
        return ClientID;
    }

    public void setClientID(String clientID) {
        ClientID = clientID;
    }

    public String getClientPassword() {
        return ClientPassword;
    }

    public void setClientPassword(String clientPassword) {
        ClientPassword = clientPassword;
    }

    public int getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public int getServiceMask() {
        return serviceMask;
    }

    public void setServiceMask(int serviceMask) {
        this.serviceMask = serviceMask;
    }

    public int getCallMsgMask() {
        return callMsgMask;
    }

    public void setCallMsgMask(int callMsgMask) {
        this.callMsgMask = callMsgMask;
    }

    public int getAgentStateMask() {
        return agentStateMask;
    }

    public void setAgentStateMask(int agentStateMask) {
        this.agentStateMask = agentStateMask;
    }

    public Transport getTransport() {
        return transport;
    }

    public void setTransport(Transport transport) {
        this.transport = transport;
    }

    public Integer getMonitoringID() {
        return monitoringID;
    }

    public void setMonitoringID(Integer monitoringID) {
        this.monitoringID = monitoringID;
    }


    //Methods
    private Transport buildNewTransport(Socket socket) {
        Transport transport = context.getBean(Transport.class);
        transport.setSocket(socket);
        transport.start();
        return transport;
    }

    public void buildTransport(Socket socket) {
        transport = buildNewTransport(socket);
    }

    @PostConstruct
    public void init() {

    }

    @PreDestroy
    public void destroy() {

    }
}
