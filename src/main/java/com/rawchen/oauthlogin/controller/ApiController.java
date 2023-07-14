package com.rawchen.oauthlogin.controller;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rawchen.oauthlogin.entity.User;
import com.rawchen.oauthlogin.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author RawChen
 * @date 2023-07-12 13:33
 */
@Slf4j
@Controller
public class ApiController {

	@Value("${qq.redirect}")
	private String redirect;

	@Value("${qq.app-id}")
	private String clientId;

	@Value("${qq.app-key}")
	private String secret;

	@Autowired
	private UserMapper userMapper;

	/**
	 * 请求QQ登录，重定向登录页面
	 */
	@GetMapping("/login")
	public void login(HttpServletResponse response) throws Exception {
		// QQ认证服务器地址
		String url = "https://graph.qq.com/oauth2.0/authorize";
		// 请求QQ认证服务器
		response.sendRedirect(String.format("%s?response_type=code&client_id=%s&redirect_uri=%s", url, clientId, redirect));
	}

	/**
	 * QQ登录回调
	 * 1. 获取openId去数据库找如果存在说明帐号已创建并关联了qq
	 * 2. 如果不存在就创建用户并登录
	 */
	@GetMapping("/callback")
	public void callback(String code, Map<String, Object> map, HttpSession session, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// 向QQ认证服务器申请令牌
		String url = "https://graph.qq.com/oauth2.0/token";
		String param = String.format("grant_type=authorization_code&code=%s&redirect_uri=%s&client_id=%s&client_secret=%s",
				code, redirect, clientId, secret);
		String result = HttpUtil.post(url, param);
		System.out.println("result: " + result);
		Map<String, String> params = params2Map(result);
		String accessToken = params.get("access_token");
		if (accessToken == null || "".equals(accessToken)) {
			throw new RuntimeException("登录失败：未获取到AccessToken！");
		}
		// 获取QQ用户的openId
		String meUrl = "https://graph.qq.com/oauth2.0/me";
		String openIdResp = HttpUtil.get(String.format("%s?access_token=%s", meUrl, accessToken));
		System.out.println("openIdResp: " + openIdResp);
		JSON parse = JSONUtil.parse(openIdResp.substring(openIdResp.indexOf("{"), openIdResp.indexOf("}") + 1));
		String openId = (String) parse.getByPath("openid");
		if (openId == null || "".equals(openId)) {
			throw new RuntimeException("登录失败：未获取到OpenId！");
		}
		// 获取用户的信息
		String infoUrl = "https://graph.qq.com/user/get_user_info";
		String infoResp = HttpUtil.get(String.format("%s?access_token=%s&oauth_consumer_key=%s&openid=%s", infoUrl, accessToken, clientId, openId));
		System.out.println("infoResp: " + infoResp);
		JSON infoObj = JSONUtil.parse(infoResp);

		// 根据OpenID获取QQ用户
		User user = null;
		user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenId, openId).last("limit 1"));
		if (user == null) {
			String nickname = (String) infoObj.getByPath("nickname");
			String sex = (String) infoObj.getByPath("gender");
			String province = (String) infoObj.getByPath("province");
			String city = (String) infoObj.getByPath("city");
			String img = (String) infoObj.getByPath("figureurl_2");
			user = User.builder().img(img).name(nickname).openId(openId).build();
			userMapper.insert(user);
		}
		session.setAttribute("user", user);

		Cookie cookie = new Cookie("JSESSIONID", req.getSession().getId());
		cookie.setMaxAge(60 * 60 * 24 * 7);
		session.setMaxInactiveInterval(60 * 60 * 24 * 7);
		resp.addCookie(cookie);
		resp.sendRedirect("/");
	}

	public static Map<String, String> params2Map(String params) {
		Map<String, String> map = new HashMap<>();
		String[] tmp = params.trim().split("&");
		for (String param : tmp) {
			String[] kv = param.split("=");
			if (kv.length == 2) {
				map.put(kv[0], kv[1]);
			}
		}
		return map;
	}
}
