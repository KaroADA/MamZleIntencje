package com.example.mamzleintencje.monitor;

interface IUserService {
    void destroy() = 16777114; // https://github.com/RikkaApps/Shizuku-API#UserService
    String runCommand(String cmd) = 1;
}