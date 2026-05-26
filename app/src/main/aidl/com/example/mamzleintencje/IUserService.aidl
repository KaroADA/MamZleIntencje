package com.example.mamzleintencje;

interface IUserService {
    void destroy() = 16777114; // https://github.com/RikkaApps/Shizuku-API#UserService
    String runCommand(String cmd) = 1;
}