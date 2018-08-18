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
package org.apache.dubbo.config;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import java.util.Map;

/**
 * AbstractMethodConfig
 *
 * @export
 */
public abstract class AbstractMethodConfig extends AbstractConfig {

    private static final long serialVersionUID = 1L;

    /**
     * 方法调用超时时间(毫秒)
     * 	缺省为的timeout
     * 	可选
     */
    // timeout for remote invocation in milliseconds
    protected Integer timeout;

    /**
     * 远程服务调用重试次数，不包括第一次调用，不需要重试请设为0
     * 缺省为<dubbo:reference>的retries
     * 可选
     */
    // retry times
    protected Integer retries;

    /**
     * 每服务消费者最大并发调用限制
     * def=0
     * 可选
     */
    // max concurrent invocations
    protected Integer actives;

    /**
     * 负载均衡策略，可选值：random,roundrobin,leastactive，分别表示：随机，轮循，最少活跃调用
     * def=random
     *
     * 可选
     */
    // load balance
    protected String loadbalance;

    /**
     * 是否异步执行，不可靠异步，只是忽略返回值，不阻塞执行线程
     * 缺省为<dubbo:reference>或<dubbo:consumer>的async
     * 可选
     */
    // whether to async
    protected Boolean async;

    /**
     * 异步调用时，标记sent=true时，表示网络已发出数据
     * def=true
     * 可选
     */
    // whether to ack async-sent
    protected Boolean sent;

    /**
     * 服务接口调用失败Mock实现类名，该Mock类必须有一个无参构造函数，与Local的区别在于，Local总是被执行，而Mock只在出现非业务异常(比如超时，网络异常等)时执行，Local在远程调用之前执行，Mock在远程调用后执行
     *
     * 可选
     */
    // the name of mock class which gets called when a service fails to execute
    protected String mock;

    // merger
    protected String merger;

    /**
     * 以调用参数为key，缓存返回结果，可选：lru, threadlocal, jcache等
     *
     * 可选
     */
    // cache
    protected String cache;

    /**
     * 是否启用JSR303标准注解验证，如果启用，将对方法参数上的注解进行校验
     *
     * 可选
     */
    // validation
    protected String validation;

    // customized parameters
    protected Map<String, String> parameters;

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    public String getLoadbalance() {
        return loadbalance;
    }

    public void setLoadbalance(String loadbalance) {
        checkExtension(LoadBalance.class, "loadbalance", loadbalance);
        this.loadbalance = loadbalance;
    }

    public Boolean isAsync() {
        return async;
    }

    public void setAsync(Boolean async) {
        this.async = async;
    }

    public Integer getActives() {
        return actives;
    }

    public void setActives(Integer actives) {
        this.actives = actives;
    }

    public Boolean getSent() {
        return sent;
    }

    public void setSent(Boolean sent) {
        this.sent = sent;
    }

    @Parameter(escaped = true)
    public String getMock() {
        return mock;
    }

    public void setMock(Boolean mock) {
        if (mock == null) {
            setMock((String) null);
        } else {
            setMock(String.valueOf(mock));
        }
    }

    public void setMock(String mock) {
        if (mock != null && mock.startsWith(Constants.RETURN_PREFIX)) {
            checkLength("mock", mock);
        } else {
            checkName("mock", mock);
        }
        this.mock = mock;
    }

    public String getMerger() {
        return merger;
    }

    public void setMerger(String merger) {
        this.merger = merger;
    }

    public String getCache() {
        return cache;
    }

    public void setCache(String cache) {
        this.cache = cache;
    }

    public String getValidation() {
        return validation;
    }

    public void setValidation(String validation) {
        this.validation = validation;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        checkParameterName(parameters);
        this.parameters = parameters;
    }

}