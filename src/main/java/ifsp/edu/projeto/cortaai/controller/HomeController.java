package ifsp.edu.projeto.cortaai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "redirect:/swagger-ui/index.html";
    }

}
