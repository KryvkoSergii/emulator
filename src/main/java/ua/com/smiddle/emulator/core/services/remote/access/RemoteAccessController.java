package ua.com.smiddle.emulator.core.services.remote.access;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ua.com.smiddle.emulator.AgentDescriptor;
import ua.com.smiddle.emulator.core.services.Pools;
import ua.com.smiddle.emulator.core.util.LoggerUtil;

import javax.servlet.http.HttpServletResponse;
import java.util.Collection;

/**
 * @author ksa on 29.11.16.
 * @project emulator
 */
@RestController("ResultRESTController")
@RequestMapping("/cti-emulator/remote")
public class RemoteAccessController {
    private final String module = "RemoteAccessController";
    @Autowired
    @Qualifier("LoggerUtil")
    private LoggerUtil logger;
    @Autowired
    @Qualifier("Pools")
    private Pools pool;

    @CrossOrigin
    @RequestMapping(value = "/agents", method = RequestMethod.GET,
            consumes = "application/json; charset=UTF-8", produces = "application/json; charset=UTF-8")
    public Object getAgents(HttpServletResponse response) {
        logger.logMore_1(module, "getAgents: got request");
        Collection<AgentDescriptor> agents = pool.getAgentMapping().values();
        logger.logMore_1(module, "getAgents: returned size=" + agents.size());
        return agents;
    }
}