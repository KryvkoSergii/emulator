package ua.com.smiddle.emulator.core.services.remote.access;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import ua.com.smiddle.emulator.core.model.AgentStatistic;
import ua.com.smiddle.emulator.core.services.Pools;
import ua.com.smiddle.emulator.core.services.statistic.Statistic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author ksa on 07.12.16.
 * @project emulator
 */
@Controller("WebController")
@RequestMapping("/cti-emulator/web")
public class WebController {
    @Autowired
    @Qualifier(value = "Statistic")
    private Statistic statistic;
    @Autowired
    @Qualifier(value = "Pools")
    private Pools pools;

    @RequestMapping(value = {"/agents_stat", "/index.html"}, method = RequestMethod.GET)
    public ModelAndView getIndexPage() {
        ModelAndView model = new ModelAndView("index");
        model.addObject("statistic", convert(statistic.getAgentStatistic().values()));
        model.addObject("TimeStamp", new Date());
        model.addObject("AgentMapping", pools.getAgentMapping().size());
        model.addObject("InstrumentMapping", pools.getInstrumentMapping().size());
        model.addObject("Subscribers", pools.getSubscribers().size());
        model.addObject("CallsHolder", pools.getCallsHolder().size());
        model.addObject("MonitorsHolder", pools.getMonitorsHolder().size());
        return model;
    }

    private Collection convert(Collection<AgentStatistic> collection) {
        List<String[]> l = new ArrayList<>();
        String[] row;
        for (AgentStatistic as : collection) {
            row = new String[5];
            row[0] = as.getAgentId();
            row[1] = as.getAgentStates().stream().map(objects -> String.valueOf(objects[1]).concat(", ")).reduce(" ", String::concat);
            row[2] = String.valueOf(as.getAgentStates().stream().count());
            row[3] = as.getCallsStatistic().stream().map(objects -> String.valueOf(objects[1]).concat(", ")).reduce(" ", String::concat);
            row[4] = String.valueOf(as.getCallsStatistic().stream().count());
            l.add(row);
        }
        return l;
    }

}
