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

import java.util.Map;

/**
 * RegistryConfig
 *
 * @export
 */
public class RegistryConfig extends AbstractConfig {

    public static final String NO_AVAILABLE = "N/A";
    private static final long serialVersionUID = 5508512956753757169L;

    /**
     * 注册中心引用BeanId，可以在<dubbo:service registry="">或<dubbo:reference registry="">中引用此ID
     *
     * 可选
     * @see AbstractConfig.id
     */
    //id

    /**
     * 注册中心服务器地址，如果地址没有端口缺省为9090，同一集群内的多个地址用逗号分隔，如：ip:port,ip:port，不同集群的注册中心，请配置多个<dubbo:registry>标签
     *
     * 必填
     */
    // register center address
    @SuppressWarnings("JavadocReference")
    private String address;

    /**
     * 登录注册中心用户名，如果注册中心不需要验证可不填
     *
     * 可选
     */
    // username to login register center
    private String username;

    /**
     * 登录注册中心密码，如果注册中心不需要验证可不填
     *
     * 可选
     */
    // password to login register center
    private String password;

    /**
     * 注册中心缺省端口，当address没有带端口时使用此端口做为缺省值
     * def=9090
     * 可选
     */
    // default port for register center
    private Integer port;

    /**
     * 注同中心地址协议，支持dubbo, http, local三种协议，分别表示，dubbo地址，http地址，本地注册中心
     * def=dubbo
     * 可选
     */
    // protocol for register center
    private String protocol;

    /**
     * 网络传输方式，可选mina,netty
     * def=netty
     * 可选
     */
    // client impl
    private String transporter;

    private String server;

    private String client;

    private String cluster;

    private String group;

    private String version;

    /**
     * 注册中心请求超时时间(毫秒)
     * def=5000
     * 可选
     */
    // request timeout in milliseconds for register center
    private Integer timeout;

    /**
     * 注册中心会话超时时间(毫秒)，用于检测提供者非正常断线后的脏数据，比如用心跳检测的实现，此时间就是心跳间隔，不同注册中心实现不一样
     * def=60000
     * 可选
     */
    // session timeout in milliseconds for register center
    private Integer session;

    /**
     * 使用文件缓存注册中心地址列表及服务提供者列表，应用重启时将基于此文件恢复，注意：两个注册中心不能使用同一文件存储
     *
     * 可选
     */
    // file for saving register center dynamic list
    private String file;

    /**
     * 停止时等待通知完成时间(毫秒)
     * def=0
     * 可选
     */
    // wait time before stop
    private Integer wait;

    /**
     * 注册中心不存在时，是否报错
     * def=true
     * 可选
     */
    // whether to check if register center is available when boot up
    private Boolean check;

    /**
     * 服务是否动态注册，如果设为false，注册后将显示后disable状态，需人工启用，并且服务提供者停止时，也不会自动取消册，需人工禁用
     * def=true
     * 可选
     */
    // whether to allow dynamic service to register on the register center
    private Boolean dynamic;

    /**
     * 是否向此注册中心注册服务，如果设为false，将只订阅，不注册
     * def=true
     * 可选
     */
    // whether to export service on the register center
    private Boolean register;

    /**
     * 是否向此注册中心订阅服务，如果设为false，将只注册，不订阅
     * def=true
     * 可选
     */
    // whether allow to subscribe service on the register center
    private Boolean subscribe;

    // customized parameters
    private Map<String, String> parameters;

    // if it's default
    private Boolean isDefault;

    public RegistryConfig() {
    }

    public RegistryConfig(String address) {
        setAddress(address);
    }

    public RegistryConfig(String address, String protocol) {
        setAddress(address);
        setProtocol(protocol);
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        checkName("protocol", protocol);
        this.protocol = protocol;
    }

    @Parameter(excluded = true)
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        checkName("username", username);
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        checkLength("password", password);
        this.password = password;
    }

    /**
     * @return wait
     * @see org.apache.dubbo.config.ProviderConfig#getWait()
     * @deprecated
     */
    @Deprecated
    public Integer getWait() {
        return wait;
    }

    /**
     * @param wait
     * @see org.apache.dubbo.config.ProviderConfig#setWait(Integer)
     * @deprecated
     */
    @Deprecated
    public void setWait(Integer wait) {
        this.wait = wait;
        if (wait != null && wait > 0)
            System.setProperty(Constants.SHUTDOWN_WAIT_KEY, String.valueOf(wait));
    }

    public Boolean isCheck() {
        return check;
    }

    public void setCheck(Boolean check) {
        this.check = check;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        checkPathLength("file", file);
        this.file = file;
    }

    /**
     * @return transport
     * @see #getTransporter()
     * @deprecated
     */
    @Deprecated
    @Parameter(excluded = true)
    public String getTransport() {
        return getTransporter();
    }

    /**
     * @param transport
     * @see #setTransporter(String)
     * @deprecated
     */
    @Deprecated
    public void setTransport(String transport) {
        setTransporter(transport);
    }

    public String getTransporter() {
        return transporter;
    }

    public void setTransporter(String transporter) {
        checkName("transporter", transporter);
        /*if(transporter != null && transporter.length() > 0 && ! ExtensionLoader.getExtensionLoader(Transporter.class).hasExtension(transporter)){
            throw new IllegalStateException("No such transporter type : " + transporter);
        }*/
        this.transporter = transporter;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        checkName("server", server);
        /*if(server != null && server.length() > 0 && ! ExtensionLoader.getExtensionLoader(Transporter.class).hasExtension(server)){
            throw new IllegalStateException("No such server type : " + server);
        }*/
        this.server = server;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        checkName("client", client);
        /*if(client != null && client.length() > 0 && ! ExtensionLoader.getExtensionLoader(Transporter.class).hasExtension(client)){
            throw new IllegalStateException("No such client type : " + client);
        }*/
        this.client = client;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Integer getSession() {
        return session;
    }

    public void setSession(Integer session) {
        this.session = session;
    }

    public Boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(Boolean dynamic) {
        this.dynamic = dynamic;
    }

    public Boolean isRegister() {
        return register;
    }

    public void setRegister(Boolean register) {
        this.register = register;
    }

    public Boolean isSubscribe() {
        return subscribe;
    }

    public void setSubscribe(Boolean subscribe) {
        this.subscribe = subscribe;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        checkParameterName(parameters);
        this.parameters = parameters;
    }

    public Boolean isDefault() {
        return isDefault;
    }

    public void setDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

}