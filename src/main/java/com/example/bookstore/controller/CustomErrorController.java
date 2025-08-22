package com.example.bookstore.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class CustomErrorController implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(CustomErrorController.class);

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        String errorMessage = (String) request.getAttribute("jakarta.servlet.error.message");
        Throwable exception = (Throwable) request.getAttribute("jakarta.servlet.error.exception");
        String requestUri = (String) request.getAttribute("jakarta.servlet.error.request_uri");

        if (statusCode != null) {
            if (exception != null) {
                log.error("Handle /error - status: {}, uri: {}, message: {}", statusCode, requestUri, errorMessage, exception);
            } else {
                log.error("Handle /error - status: {}, uri: {}, message: {}", statusCode, requestUri, errorMessage);
            }
        }

        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorMessage", errorMessage != null ? errorMessage : "未知錯誤");

        if (statusCode != null) {
            if (statusCode == 404) {
                model.addAttribute("title", "頁面未找到");
                model.addAttribute("message", "您訪問的頁面不存在");
            } else if (statusCode == 500) {
                model.addAttribute("title", "服務器內部錯誤");
                model.addAttribute("message", "服務器發生內部錯誤，請稍後再試");
            } else {
                model.addAttribute("title", "發生錯誤");
                model.addAttribute("message", "系統發生未知錯誤");
            }
        }

        // 統一返回到 login 頁面，並顯示錯誤信息
        return "login";
    }
}
