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
import org.apache.dubbo.common.status.StatusChecker;
import org.apache.dubbo.common.threadpool.ThreadPool;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.remoting.Dispatcher;
import org.apache.dubbo.remoting.Transporter;
import org.apache.dubbo.remoting.exchange.Exchanger;
import org.apache.dubbo.remoting.telnet.TelnetHandler;

import java.util.Arrays;

/**
 * ProviderConfig
 *
 * @export
 * @see org.apache.dubbo.config.ProtocolConfig
 * @see org.apache.dubbo.config.ServiceConfig
 */
public class ProviderConfig extends AbstractServiceConfig {

    private static final long serialVersionUID = 6913423882496634749L;

    // ======== protocol default values, it'll take effect when protocol's attributes are not set ========

    /**
     * 服务引用BeanId
     *
     * 必填
     * @see AbstractConfig.id
     */
    //id

    /**
     * 服务主机名，多网卡选择或指定VIP及域名时使用，为空则自动查找本机IP，建议不要配置，让Dubbo自动获取本机IP
     * def=自动查找本机IP
     * 可选
     */
    // service IP addresses (used when there are multiple network cards available)
    @SuppressWarnings("JavadocReference")
    private String host;

    // service port
    private Integer port;

    // context path
    private String contextpath;

    /**
     * 线程池类型，可选：fixed/cached
     * def=fixed
     * 可选
     */
    // thread pool
    private String threadpool;

    /**
     * 服务线程池大小(固定大小)
     * def=100
     * 可选
     */
    // thread pool size (fixed size)
    private Integer threads;

    /**
     * IO线程池，接收网络读写中断，以及序列化和反序列化，不处理业务，业务线程池参见threads配置，此线程池和CPU相关，不建议配置。
     * def=CPU + 1
     * 可选
     */
    // IO thread pool size (fixed size)
    private Integer iothreads;

    // thread pool queue length
    private Integer queues;

    /**
     * 服务提供者最大可接受连接数
     * def=0
     * 可选
     */
    // max acceptable connections
    private Integer accepts;

    /**
     * 协议编码方式
     * def=dubbo
     * 可选
     */
    // protocol codec
    private String codec;

    /**
     * 序列化编码
     * def=UTF-8
     * 可选
     */
    // charset
    private String charset;

    /**
     * 请求及响应数据包大小限制，单位：字节
     * def=88388608(=8M)
     * 可选
     */
    // payload max length
    private Integer payload;

    /**
     * 网络读写缓冲区大小
     * def=	8192
     * 可选
     */
    // buffer size
    private Integer buffer;

    // transporter
    private String transporter;

    // how information gets exchanged
    private String exchanger;

    // thread dispatching mode
    private String dispatcher;

    // networker
    private String networker;

    /**
     * 协议的服务器端实现类型，比如：dubbo协议的mina,netty等，http协议的jetty,servlet等
     * def=dubbo协议缺省为netty，http协议缺省为servlet
     * 可选
     */
    // server impl
    private String server;

    /**
     * 协议的客户端实现类型，比如：dubbo协议的mina,netty等
     * def=dubbo协议缺省为netty
     * 可选
     */
    // client impl
    private String client;

    /**
     * 所支持的telnet命令，多个命令用逗号分隔
     *
     * 可选
     */
    // supported telnet commands, separated with comma.
    private String telnet;

    // command line prompt
    private String prompt;

    // status check
    private String status;

    // wait time when stop
    private Integer wait;

    /**
     * 是否为缺省协议，用于多协议
     * def=false
     * 可选
     */
    // if it's default
    private Boolean isDefault;

    @Deprecated
    public void setProtocol(String protocol) {
        this.protocols = Arrays.asList(new ProtocolConfig[]{new ProtocolConfig(protocol)});
    }

    @Parameter(excluded = true)
    public Boolean isDefault() {
        return isDefault;
    }

    @Deprecated
    public void setDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    @Parameter(excluded = true)
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Parameter(excluded = true)
    public Integer getPort() {
        return port;
    }

    @Deprecated
    public void setPort(Integer port) {
        this.port = port;
    }

    @Deprecated
    @Parameter(excluded = true)
    public String getPath() {
        return getContextpath();
    }

    @Deprecated
    public void setPath(String path) {
        setContextpath(path);
    }

    @Parameter(excluded = true)
    public String getContextpath() {
        return contextpath;
    }

    public void setContextpath(String contextpath) {
        checkPathName("contextpath", contextpath);
        this.contextpath = contextpath;
    }

    public String getThreadpool() {
        return threadpool;
    }

    public void setThreadpool(String threadpool) {
        checkExtension(ThreadPool.class, "threadpool", threadpool);
        this.threadpool = threadpool;
    }

    public Integer getThreads() {
        return threads;
    }

    public void setThreads(Integer threads) {
        this.threads = threads;
    }

    public Integer getIothreads() {
        return iothreads;
    }

    public void setIothreads(Integer iothreads) {
        this.iothreads = iothreads;
    }

    public Integer getQueues() {
        return queues;
    }

    public void setQueues(Integer queues) {
        this.queues = queues;
    }

    public Integer getAccepts() {
        return accepts;
    }

    public void setAccepts(Integer accepts) {
        this.accepts = accepts;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public Integer getPayload() {
        return payload;
    }

    public void setPayload(Integer payload) {
        this.payload = payload;
    }

    public Integer getBuffer() {
        return buffer;
    }

    public void setBuffer(Integer buffer) {
        this.buffer = buffer;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getTelnet() {
        return telnet;
    }

    public void setTelnet(String telnet) {
        checkMultiExtension(TelnetHandler.class, "telnet", telnet);
        this.telnet = telnet;
    }

    @Parameter(escaped = true)
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        checkMultiExtension(StatusChecker.class, "status", status);
        this.status = status;
    }

    @Override
    public String getCluster() {
        return super.getCluster();
    }

    @Override
    public Integer getConnections() {
        return super.getConnections();
    }

    @Override
    public Integer getTimeout() {
        return super.getTimeout();
    }

    @Override
    public Integer getRetries() {
        return super.getRetries();
    }

    @Override
    public String getLoadbalance() {
        return super.getLoadbalance();
    }

    @Override
    public Boolean isAsync() {
        return super.isAsync();
    }

    @Override
    public Integer getActives() {
        return super.getActives();
    }

    public String getTransporter() {
        return transporter;
    }

    public void setTransporter(String transporter) {
        checkExtension(Transporter.class, "transporter", transporter);
        this.transporter = transporter;
    }

    public String getExchanger() {
        return exchanger;
    }

    public void setExchanger(String exchanger) {
        checkExtension(Exchanger.class, "exchanger", exchanger);
        this.exchanger = exchanger;
    }

    /**
     * typo, switch to use {@link #getDispatcher()}
     *
     * @deprecated {@link #getDispatcher()}
     */
    @Deprecated
    @Parameter(excluded = true)
    public String getDispather() {
        return getDispatcher();
    }

    /**
     * typo, switch to use {@link #getDispatcher()}
     *
     * @deprecated {@link #setDispatcher(String)}
     */
    @Deprecated
    public void setDispather(String dispather) {
        setDispatcher(dispather);
    }

    public String getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(String dispatcher) {
        checkExtension(Dispatcher.class, Constants.DISPATCHER_KEY, exchanger);
        checkExtension(Dispatcher.class, "dispather", exchanger);
        this.dispatcher = dispatcher;
    }

    public String getNetworker() {
        return networker;
    }

    public void setNetworker(String networker) {
        this.networker = networker;
    }

    public Integer getWait() {
        return wait;
    }

    public void setWait(Integer wait) {
        this.wait = wait;
    }

}