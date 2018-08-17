package org.apache.dubbo.test.personal;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.demo.DemoService;

/**
 * @author Yao.Zhou
 * @since 2018/8/15 23:45
 */
public class ConsumerConfig {

    public static void main(String[] args) {

        //application
        ApplicationConfig application = new ApplicationConfig();
        application.setName("demo-consumer");

        RegistryConfig registry = new RegistryConfig();
        /*registry.setAddress("multicast://224.5.6.7:1234");*/

        registry.setTimeout(10000);
        registry.setProtocol("zookeeper");
        registry.setAddress("10.0.17.80:2181,10.0.17.81:2181,10.0.17.82:2181");

/*        registry.setUsername("admin");
        registry.setPassword("admin");*/

        //引用远程服务
        ReferenceConfig<DemoService> reference = new ReferenceConfig<>();
        reference.setCheck(false);
        reference.setId("demoService");
        reference.setInterface(DemoService.class);
        reference.setApplication(application);
        reference.setRegistry(registry);
        reference.setVersion("1.0.0");

        reference.setProtocol("dubbo");
        reference.setUrl("dubbo://127.0.0.1:20880");

        DemoService demoService = reference.get();
        demoService.sayHello("hello ");

    }

}
