package org.apache.dubbo.test.personal.config;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.demo.DemoService;

/**
 * @author Yao.Zhou
 * @since 2018/8/15 23:45
 */
public class Apiconfig {

    public static void main(String[] args) {
        ApplicationConfig application = new ApplicationConfig();
        application.setName("demo-consumer");

        RegistryConfig registry = new RegistryConfig();
        registry.setAddress("multicast://224.5.6.7:1234");
        registry.setUsername("admin");
        registry.setPassword("admin");

        //引用远程服务
        ReferenceConfig<DemoService> reference = new ReferenceConfig<>();
        reference.setApplication(application);
        reference.setRegistry(registry);
        reference.setInterface(DemoService.class);
        reference.setVersion("1.1.0");

        //DemoService demoService = reference.get();
    }

}
