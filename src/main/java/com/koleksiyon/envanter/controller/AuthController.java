package com.koleksiyon.envanter.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() {
        return "login"; // templates altındaki login.html'i çağırır
    }

    @Autowired
    private com.koleksiyon.envanter.service.UserService userService;

    @GetMapping("/register")
    public String showRegisterForm(org.springframework.ui.Model model) {
        model.addAttribute("user", new com.koleksiyon.envanter.entity.User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@org.springframework.web.bind.annotation.ModelAttribute("user") com.koleksiyon.envanter.entity.User user) {
        try {
            userService.saveUser(user);
        } catch (IllegalArgumentException e) {
            return "redirect:/register?error";
        }
        return "redirect:/login?registered=true";
    }
}