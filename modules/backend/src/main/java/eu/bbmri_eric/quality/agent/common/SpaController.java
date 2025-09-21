package eu.bbmri_eric.quality.agent.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
class SpaController {
  @RequestMapping(value = "/{path:[^.]*}", method = RequestMethod.GET)
  String redirect() {
    return "forward:/index.html";
  }
}
