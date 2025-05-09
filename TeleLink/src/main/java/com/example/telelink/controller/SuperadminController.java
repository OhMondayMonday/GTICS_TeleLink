package com.example.telelink.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/superadmin")
public class SuperadminController {

    @GetMapping("")
    public String listarUsuarios() {
        return "Superadmin/Dashboard";
    }
}
