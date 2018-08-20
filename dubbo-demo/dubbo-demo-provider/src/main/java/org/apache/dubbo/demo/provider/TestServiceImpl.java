package org.apache.dubbo.demo.provider;

import org.apache.dubbo.demo.TestService;
import org.apache.dubbo.rpc.RpcContext;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Yao.Zhou
 * @since 2018/8/20 11:13
 */
public class TestServiceImpl implements TestService {

    @Override
    public String sayHi(String name) {
        System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] Hi " + name + ", request from consumer: " + RpcContext.getContext().getRemoteAddress());
        return "Hi " + name + ", response from provider: " + RpcContext.getContext().getLocalAddress();
    }

}
