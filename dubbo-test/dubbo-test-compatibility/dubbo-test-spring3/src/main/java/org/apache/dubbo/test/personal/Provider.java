package org.apache.dubbo.test.personal;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.demo.DemoService;
import org.apache.dubbo.demo.provider.DemoServiceImpl;

import java.io.IOException;

/**
 * dubbo Service的初始化
 *
 * ReferenceConfig 的初始化，会多创建 ProtocolConfig 对象，设置到 ServiceConfig
 *
 * @author Yao.Zhou
 * @since 2018/8/17 16:32
 */
public class Provider {

    public static void main(String[] args) {
        DemoService demoService = new DemoServiceImpl();

        //application
        ApplicationConfig application = new ApplicationConfig();
        application.setName("demo-provider");

        //registry
        RegistryConfig registry = new RegistryConfig();
        registry.setTimeout(10000);
        registry.setGroup("group-dubbo-demo");

        /*registry.setAddress("multicast://224.5.6.7:1234");*/

        registry.setAddress("10.0.17.80:2181,10.0.17.81:2181,10.0.17.82:2181");
        registry.setProtocol("zookeeper");


        //protocol
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setId("dubbo");
        protocol.setName("dubbo");
        protocol.setHost("127.0.0.1");
        protocol.setPort(20880);
        protocol.setThreads(2);

        //需要暴露的服务
        ServiceConfig service = new ServiceConfig();
        service.setApplication(application);
        service.setRegistry(registry);
        service.setProtocol(protocol);
        service.setInterface(DemoService.class);
        service.setRef(demoService);
        service.setVersion("1.0.0");

        //暴露服务
        service.export();

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
