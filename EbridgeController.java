package ebridge.automation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class EbridgeController {

    @Autowired
    private EbridgeService ebridgeService;

    @GetMapping("/start")
    @ResponseBody
    public String startAutomation() {
        return ebridgeService.startAutomation();
//    	return "Gokila";
    }
    
//    public String index() {
//    	return 
//    }
    
    @GetMapping("/jk")
    public String index() {
    	return "static/index.html;
    }
}
