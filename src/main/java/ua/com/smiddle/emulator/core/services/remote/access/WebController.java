package ua.com.smiddle.emulator.core.services.remote.access;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import ua.com.smiddle.emulator.core.model.AgentStatistic;
import ua.com.smiddle.emulator.core.services.statistic.Statistic;

import java.util.ArrayList;
import java.util.Collection;
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

    @RequestMapping(value = {"/agents_stat", "/index.html"}, method = RequestMethod.GET)
    public ModelAndView getIndexPage() {
        ModelAndView model = new ModelAndView("index");
        model.addObject("statistic", convert(statistic.getAgentStatistic().values()));
        return model;
    }

    private Collection convert(Collection<AgentStatistic> collection) {
        List<String[]> l = new ArrayList<>();
        String[] row;
        for (AgentStatistic as : collection) {
            row = new String[3];
            row[0] = as.getAgentId();
            row[1] = as.getAgentStates().stream().map(objects -> String.valueOf(objects[1]).concat(" ")).reduce(" ", String::concat);
            row[2] = as.getCallsStatistic().stream().map(objects -> String.valueOf(objects[1])).reduce(" ", String::concat);
            l.add(row);
        }
        return l;
    }

}
