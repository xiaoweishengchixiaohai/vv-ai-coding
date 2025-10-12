package edu.ncu.vvaicoding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication
public class VvAiCodingApplication {

    public static void main(String[] args) {
        SpringApplication.run(VvAiCodingApplication.class, args);
    }

}
