package com.elavon.migrator.mcp;

import com.elavon.migrator.mcp.core.JsonRpcServer;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(McpServerApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setLogStartupInfo(false);
        app.run(args);
        JsonRpcServer server = new JsonRpcServer();
        server.runEventLoop();
    }
}


