/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.demo.consumer;

import org.apache.dubbo.demo.DemoService;
import org.apache.dubbo.rpc.Invocation;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.lang.reflect.Method;

public class Consumer {

    /**
     * To get ipv6 address to work, add
     * System.setProperty("java.net.preferIPv6Addresses", "true");
     * before running your application.
     *
     * @see org.apache.dubbo.rpc.proxy.InvokerInvocationHandler#invoke(Object, Method, Object[])
     * @see org.apache.dubbo.rpc.protocol.dubbo.DubboInvoker#doInvoke(Invocation)
     * @see org.apache.dubbo.remoting.exchange.support.header.HeaderExchangeChannel#request(Object, int)
     * @see org.apache.dubbo.remoting.transport.AbstractClient#send(Object, boolean)
     * @see org.apache.dubbo.remoting.transport.netty4.NettyChannel#send(Object, boolean)
     */
    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-consumer.xml"});
        context.start();

        // get remote service proxy
        DemoService demoService = (DemoService) context.getBean("demoService");
        //TestService testService = TestService.class.cast(context.getBean(TestService.class));

        while (true) {
            try {
                Thread.sleep(5000);
                String hello = demoService.sayHello("world");
                System.out.println(hello);
                /*String hi = testService.sayHi("hi");
                System.out.println(hi);*/
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }

            //break;
        }
    }
}
