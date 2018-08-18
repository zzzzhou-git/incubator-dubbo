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
package org.apache.dubbo.rpc;

/**
 * Invoker 暴露服务在 Protocol 上的对象
 *
 * Exporter. (API/SPI, Prototype, ThreadSafe)
 *
 * @see org.apache.dubbo.rpc.Protocol#export(Invoker)
 * @see org.apache.dubbo.rpc.ExporterListener
 * @see org.apache.dubbo.rpc.protocol.AbstractExporter
 */
public interface Exporter<T> {

    /**
     * 获得对应的 Invoker
     * get invoker.
     *
     * @return invoker
     */
    Invoker<T> getInvoker();

    /**
     * 取消暴露
     *
     * Exporter 相比 Invoker 接口，多了 这个方法。通过实现该方法，使相同的 Invoker 在不同的 Protocol 实现的取消暴露逻辑
     *
     * unexport.
     * <p>
     * <code>
     * getInvoker().destroy();
     * </code>
     */
    void unexport();

}