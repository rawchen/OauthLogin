package com.rawchen.oauthlogin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;

/**
 * @author RawChen
 * @date 2023-07-12 14:00
 */
@Controller
public class IndexController {

	@RequestMapping({"/", "/index"})
	public String toIndex() {
		return "index";
	}

	@RequestMapping("/logout")
	public String toLogout(HttpSession session) {
		session.invalidate();
		return "index";
	}


}
