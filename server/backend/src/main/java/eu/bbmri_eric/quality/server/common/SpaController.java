package eu.bbmri_eric.quality.server.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/** Controller for redirecting HTTP requests to the SPA served by spring boot */
@Controller
class SpaController {
  @RequestMapping(value = "/{path:[^.]*}", method = RequestMethod.GET)
  String redirect() {
    return "forward:/index.html";
  }
}
