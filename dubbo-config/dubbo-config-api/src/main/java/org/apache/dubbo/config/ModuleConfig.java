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

import org.apache.dubbo.config.support.Parameter;

import java.util.ArrayList;
import java.util.List;

/**
 * ModuleConfig
 *
 * @export
 */
public class ModuleConfig extends AbstractConfig {

    private static final long serialVersionUID = 5508512956753757169L;

    /**
     * 当前模块名称，用于注册中心计算模块间依赖关系
     *
     * 必填
     */
    // module name
    private String name;

    /**
     * 当前模块的版本
     *
     * 可选
     */
    // module version
    private String version;

    /**
     * 模块负责人，用于服务治理，请填写负责人公司邮箱前缀
     *
     * 可选
     */
    // module owner
    private String owner;

    /**
     * 组织名称(BU或部门)，用于注册中心区分服务来源，此配置项建议不要使用autoconfig，直接写死在配置中，比如china,intl,itu,crm,asc,dw,aliexpress等
     *
     * 可选
     */
    // module's organization
    private String organization;

    // registry centers
    private List<RegistryConfig> registries;

    // monitor center
    private MonitorConfig monitor;

    // if it's default
    private Boolean isDefault;

    public ModuleConfig() {
    }

    public ModuleConfig(String name) {
        setName(name);
    }

    @Parameter(key = "module", required = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        checkName("name", name);
        this.name = name;
        if (id == null || id.length() == 0) {
            id = name;
        }
    }

    @Parameter(key = "module.version")
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        checkName("owner", owner);
        this.owner = owner;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        checkName("organization", organization);
        this.organization = organization;
    }

    public RegistryConfig getRegistry() {
        return registries == null || registries.isEmpty() ? null : registries.get(0);
    }

    public void setRegistry(RegistryConfig registry) {
        List<RegistryConfig> registries = new ArrayList<RegistryConfig>(1);
        registries.add(registry);
        this.registries = registries;
    }

    public List<RegistryConfig> getRegistries() {
        return registries;
    }

    @SuppressWarnings({"unchecked"})
    public void setRegistries(List<? extends RegistryConfig> registries) {
        this.registries = (List<RegistryConfig>) registries;
    }

    public MonitorConfig getMonitor() {
        return monitor;
    }

    public void setMonitor(String monitor) {
        this.monitor = new MonitorConfig(monitor);
    }

    public void setMonitor(MonitorConfig monitor) {
        this.monitor = monitor;
    }

    public Boolean isDefault() {
        return isDefault;
    }

    public void setDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

}