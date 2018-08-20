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
package org.apache.dubbo.common.extension;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.support.ActivateComparator;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.common.utils.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * 拓展加载器。这是 Dubbo SPI 的核心
 *
 * dubbo把扩展点分为了三类：
 *      可自动激活的扩展点（Activate）  ： getActivateExtension
 *      自适应的扩展点（Adaptive）     ： getAdaptiveExtension
 *      普通的扩展点                  ： getExtension
 *
 * Load dubbo extensions
 * <ul>
 * <li>auto inject dependency extension </li>
 * <li>auto wrap extension in wrapper </li>
 * <li>default extension is an adaptive instance</li>
 * </ul>
 *
 * @see <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jar/jar.html#Service%20Provider">Service Provider in Java 5</a>
 * @see org.apache.dubbo.common.extension.SPI
 * @see org.apache.dubbo.common.extension.Adaptive
 * @see org.apache.dubbo.common.extension.Activate
 */
public class ExtensionLoader<T> {
    private static final Logger logger = LoggerFactory.getLogger(ExtensionLoader.class);

    //在 META-INF/dubbo/internal/ 和 META-INF/dubbo/ 目录下，放置 接口全限定名 配置文件，每行内容为：拓展名=拓展实现类全限定名。
    //META-INF/dubbo/internal/ 目录下，用于 Dubbo 内部提供的拓展实现
    private static final String SERVICES_DIRECTORY = "META-INF/services/"; //META-INF/services目录说明兼容了JDK的SPI
    private static final String DUBBO_DIRECTORY = "META-INF/dubbo/";
    private static final String DUBBO_INTERNAL_DIRECTORY = DUBBO_DIRECTORY + "internal/";

    //NAME_SEPARATOR ，拓展名分隔符，使用逗号
    private static final Pattern NAME_SEPARATOR = Pattern.compile("\\s*[,]+\\s*");

    // ================================================静态属性
    /**
     * 静态属性说明
     *
     * 1. ExtensionLoader 是 ExtensionLoader 的管理容器。
     * 2. 一个拓展( 拓展接口 )对应一个 ExtensionLoader 对象。
     *      例如，Protocol 和 Filter 分别对应一个 ExtensionLoader 对象
     */

    /**
     * EXTENSION_LOADERS: 拓展加载器集合
     *
     * key: 拓展接口
     * value: ExtensionLoader
     */
    private static final ConcurrentMap<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<Class<?>, ExtensionLoader<?>>();

    /**
     * EXTENSION_INSTANCES: 拓展实现类集合
     * key：拓展实现类
     * value：拓展对象
     */
    private static final ConcurrentMap<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<Class<?>, Object>();

    // ================================================对象属性
    /**
     * 对象属性说明
     *
     * 1. 一个拓展通过其 ExtensionLoader 对象，加载它的拓展实现们，多个属性都是 “cached“ 开头
     * 2. ExtensionLoader 考虑到性能和资源的优化，读取拓展配置后，会首先进行缓存。等到 Dubbo 代码真正用到对应的拓展实现时，
     *      进行拓展实现的对象的初始化。并且，初始化完成后，也会进行缓存。即：缓存加载的拓展配置 -> 缓存创建的拓展实现对象
     */

    /**
     * 拓展接口
     * 比如：Protocol
     */
    private final Class<?> type;

    /**
     * 对象工厂
     * 用于调用 {@link #injectExtension(Object)} 方法，向拓展对象注入依赖属性
     * 例如，StubProxyFactoryWrapper 中有 `Protocol protocol` 属性
     */
    private final ExtensionFactory objectFactory;

    /**
     * 缓存的拓展名与拓展类的映射
     * 和 {@link #cachedClasses} 的 KV 对调。
     * 通过 {@link #loadExtensionClasses} 加载
     */
    private final ConcurrentMap<Class<?>, String> cachedNames = new ConcurrentHashMap<Class<?>, String>();

    /**
     * 缓存的 拓展实现类 集合。
     * 不包含如下两种类型：
     *      1. 自适应拓展实现类。例如 AdaptiveExtensionFactory，拓展 Adaptive 实现类，会添加到 cachedAdaptiveClass 属性中
     *      2. 带唯一参数为拓展接口的构造方法的实现类，或者说拓展 Wrapper 实现类。例如，ProtocolFilterWrapper， 拓展 Wrapper 实现类，会添加到 cachedWrapperClasses 属性中
     *      拓展 Wrapper 实现类，会添加到 {@link #cachedWrapperClasses} 中
     *
     *  通过 {@link #loadExtensionClasses} 加载
     */
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<Map<String, Class<?>>>();

    /**
     * 拓展名与 @Activate 的映射
     *
     * 例如，AccessLogFilter
     *
     * 用于 {@link #getActivateExtension(URL, String)}
     */
    private final Map<String, Object> cachedActivates = new ConcurrentHashMap<String, Object>();

    /**
     * 缓存的拓展对象集合
     * key：拓展名
     * value：拓展对象
     *
     * 例如，Protocol 拓展
     *      key：dubbo value：DubboProtocol
     *      key：injvm value：InjvmProtocol
     *
     *  通过 {@link #loadExtensionClasses} 加载
     */
    private final ConcurrentMap<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<String, Holder<Object>>();

    /**
     *  缓存的自适应( Adaptive )拓展对象
     */
    private final Holder<Object> cachedAdaptiveInstance = new Holder<Object>();

    /**
     * 缓存的自适应拓展对象的类
     * {@link #getAdaptiveExtensionClass()}
     */
    private volatile Class<?> cachedAdaptiveClass = null;

    /**
     * 缓存的默认拓展名
     * 通过 {@link SPI} 注解获得
     */
    private String cachedDefaultName;

    /**
     * 创建 {@link #cachedAdaptiveInstance} 时发生的异常
     * 发生异常后，不再创建，参见 {@link #createAdaptiveExtension()}
     */
    private volatile Throwable createAdaptiveInstanceError;

    /**
     * 拓展 Wrapper 实现类集合
     *
     * 带唯一参数为拓展接口的构造方法的实现类
     *
     * 通过 {@link #loadExtensionClasses} 加载
     */
    private Set<Class<?>> cachedWrapperClasses;

    /**
     * 拓展名 与 加载对应拓展类发生的异常 的 映射
     * key：拓展名
     * value：异常
     *
     * 在 {@link #loadDirectory(Map, String, String)} 时，记录
     */
    private Map<String, IllegalStateException> exceptions = new ConcurrentHashMap<String, IllegalStateException>();

    // ================================================构造方法

    private ExtensionLoader(Class<?> type) {
        this.type = type;

        //如果type是ExtensionFactory, 则什么不用做
        //如果type不是ExtensionFactory, 获得 ExtensionFactory 拓展接口的自适应拓展实现对象
        objectFactory = (type == ExtensionFactory.class ? null : ExtensionLoader.getExtensionLoader(ExtensionFactory.class).getAdaptiveExtension());
    }

    // ================================================静态方法

    private static <T> boolean withExtensionAnnotation(Class<T> type) {
        return type.isAnnotationPresent(SPI.class);
    }

    /**
     * 根据拓展点的接口, 获得拓展加载器（ExtensionLoader）
     * @param type 接口
     * @param <T> 泛型
     * @return ExtensionLoader
     */
    @SuppressWarnings("unchecked")
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        if (type == null)
            throw new IllegalArgumentException("Extension type == null");

        //必须是接口
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type(" + type + ") is not interface!");
        }

        //必须包含SPI注解, 表明是一个拓展点
        if (!withExtensionAnnotation(type)) {
            throw new IllegalArgumentException("Extension type(" + type +
                    ") is not extension, because WITHOUT @" + SPI.class.getSimpleName() + " Annotation!");
        }

        // 获得接口对应的拓展点加载器（ExtensionLoader）
        ExtensionLoader<T> loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        if (loader == null) {
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<T>(type));
            loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        }
        return loader;
    }

    private static ClassLoader findClassLoader() {
        return ExtensionLoader.class.getClassLoader();
    }

    // ================================================对象方法

    public String getExtensionName(T extensionInstance) {
        return getExtensionName(extensionInstance.getClass());
    }

    public String getExtensionName(Class<?> extensionClass) {
        getExtensionClasses();// load class
        return cachedNames.get(extensionClass);
    }

    /**
     * This is equivalent to {@code getActivateExtension(url, key, null)}
     *
     * @param url url
     * @param key url parameter key which used to get extension point names
     * @return extension list which are activated.
     * @see #getActivateExtension(org.apache.dubbo.common.URL, String, String)
     */
    public List<T> getActivateExtension(URL url, String key) {
        return getActivateExtension(url, key, null);
    }

    /**
     * This is equivalent to {@code getActivateExtension(url, values, null)}
     *
     * @param url    url
     * @param values extension point names
     * @return extension list which are activated
     * @see #getActivateExtension(org.apache.dubbo.common.URL, String[], String)
     */
    public List<T> getActivateExtension(URL url, String[] values) {
        return getActivateExtension(url, values, null);
    }

    /**
     * 获得符合自动激活条件的拓展对象数组
     *
     * This is equivalent to {@code getActivateExtension(url, url.getParameter(key).split(","), null)}
     *
     * @param url   url
     * @param key   url parameter key which used to get extension point names
     *              Dubbo URL 参数名
     *
     * @param group group
     *              过滤分组名
     *
     * @return extension list which are activated.
     * @see #getActivateExtension(org.apache.dubbo.common.URL, String[], String)
     */
    public List<T> getActivateExtension(URL url, String key, String group) {
        // 从 Dubbo URL 获得参数值
        String value = url.getParameter(key);

        // 获得符合自动激活条件的拓展对象数组
        return getActivateExtension(url, value == null || value.length() == 0 ? null : Constants.COMMA_SPLIT_PATTERN.split(value), group);
    }

    /**
     * 获得符合自动激活条件的拓展对象数组
     *
     * Get activate extensions.
     *
     * @param url    url
     * @param values extension point names
     * @param group  group
     * @return extension list which are activated
     * @see org.apache.dubbo.common.extension.Activate
     */
    public List<T> getActivateExtension(URL url, String[] values, String group) {
        List<T> exts = new ArrayList<T>();
        List<String> names = values == null ? new ArrayList<String>(0) : Arrays.asList(values);

        // 处理自动激活的拓展对象们
        // 判断不存在配置 `"-name"` 。例如，<dubbo:service filter="-default" /> ，代表移除所有默认过滤器。
        if (!names.contains(Constants.REMOVE_VALUE_PREFIX + Constants.DEFAULT_KEY)) {

            // 获得拓展实现类数组
            getExtensionClasses();
            for (Map.Entry<String, Object> entry : cachedActivates.entrySet()) {
                String name = entry.getKey();
                Object activate = entry.getValue();

                String[] activateGroup, activateValue;

                if (activate instanceof Activate) {
                    activateGroup = ((Activate) activate).group();  // 匹配分组
                    activateValue = ((Activate) activate).value();
                } else if (activate instanceof com.alibaba.dubbo.common.extension.Activate) {
                    activateGroup = ((com.alibaba.dubbo.common.extension.Activate) activate).group();
                    activateValue = ((com.alibaba.dubbo.common.extension.Activate) activate).value();
                } else {
                    continue;
                }
                if (isMatchGroup(group, activateGroup)) {

                    // 获得拓展对象
                    T ext = getExtension(name);
                    if (!names.contains(name)  // 不包含在自定义配置里。如果包含，会在下面的代码处理。
                            && !names.contains(Constants.REMOVE_VALUE_PREFIX + name) // 判断是否配置移除。例如 <dubbo:service filter="-monitor" />，则 MonitorFilter 会被移除
                            && isActive(activateValue, url)) { // 判断是否激活
                        exts.add(ext);
                    }
                }
            }

            // 排序
            Collections.sort(exts, ActivateComparator.COMPARATOR);
        }

        // 处理自定义配置的拓展对象们。例如在 <dubbo:service filter="demo" /> ，代表需要加入 DemoFilter
        List<T> usrs = new ArrayList<T>();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            if (!name.startsWith(Constants.REMOVE_VALUE_PREFIX)
                    && !names.contains(Constants.REMOVE_VALUE_PREFIX + name)) { // 判断非移除的

                // 将配置的自定义在自动激活的拓展对象们前面。例如，<dubbo:service filter="demo,default,demo2" /> ，则 DemoFilter 就会放在默认的过滤器前面。
                if (Constants.DEFAULT_KEY.equals(name)) {
                    if (!usrs.isEmpty()) {
                        exts.addAll(0, usrs);
                        usrs.clear();
                    }
                } else {

                    // 获得拓展对象
                    T ext = getExtension(name);
                    usrs.add(ext);
                }
            }
        }

        // 添加到结果集
        if (!usrs.isEmpty()) {
            exts.addAll(usrs);
        }
        return exts;
    }

    private boolean isMatchGroup(String group, String[] groups) {
        if (group == null || group.length() == 0) {
            return true;
        }
        if (groups != null && groups.length > 0) {
            for (String g : groups) {
                if (group.equals(g)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isActive(String[] keys, URL url) {
        if (keys.length == 0) {
            return true;
        }
        for (String key : keys) {
            for (Map.Entry<String, String> entry : url.getParameters().entrySet()) {
                String k = entry.getKey();
                String v = entry.getValue();
                if ((k.equals(key) || k.endsWith("." + key))
                        && ConfigUtils.isNotEmpty(v)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get extension's instance. Return <code>null</code> if extension is not found or is not initialized. Pls. note
     * that this method will not trigger extension load.
     * <p>
     * In order to trigger extension load, call {@link #getExtension(String)} instead.
     *
     * @see #getExtension(String)
     */
    @SuppressWarnings("unchecked")
    public T getLoadedExtension(String name) {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("Extension name == null");
        Holder<Object> holder = cachedInstances.get(name);
        if (holder == null) {
            cachedInstances.putIfAbsent(name, new Holder<Object>());
            holder = cachedInstances.get(name);
        }
        return (T) holder.get();
    }

    /**
     * Return the list of extensions which are already loaded.
     * <p>
     * Usually {@link #getSupportedExtensions()} should be called in order to get all extensions.
     *
     * @see #getSupportedExtensions()
     */
    public Set<String> getLoadedExtensions() {
        return Collections.unmodifiableSet(new TreeSet<String>(cachedInstances.keySet()));
    }

    /**
     *
     *
     * Find the extension with the given name. If the specified name is not found, then {@link IllegalStateException}
     * will be thrown.
     */
    /**
     * 返回指定名字的扩展对象。如果指定名字的扩展不存在，则抛异常 {@link IllegalStateException}.
     *
     * Find the extension with the given name. If the specified name is not found, then {@link IllegalStateException} will be thrown.
     *
     * @param name 拓展名
     * @return 拓展对象
     */
    @SuppressWarnings("unchecked")
    public T getExtension(String name) {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("Extension name == null");

        // 查找 默认的 拓展对象
        if ("true".equals(name)) {
            return getDefaultExtension();
        }

        // 从 缓存中 获得对应的拓展对象
        Holder<Object> holder = cachedInstances.get(name);
        if (holder == null) {
            cachedInstances.putIfAbsent(name, new Holder<Object>());
            holder = cachedInstances.get(name);
        }
        Object instance = holder.get();
        if (instance == null) {
            synchronized (holder) {
                instance = holder.get();

                // 从 缓存中 未获取到，进行创建缓存对象。
                if (instance == null) {
                    instance = createExtension(name);

                    // 设置创建对象到缓存中
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }

    /**
     * Return default extension, return <code>null</code> if it's not configured.
     */
    public T getDefaultExtension() {
        getExtensionClasses();
        if (null == cachedDefaultName || cachedDefaultName.length() == 0
                || "true".equals(cachedDefaultName)) {
            return null;
        }
        return getExtension(cachedDefaultName);
    }

    public boolean hasExtension(String name) {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("Extension name == null");
        try {
            this.getExtensionClass(name);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public Set<String> getSupportedExtensions() {
        Map<String, Class<?>> clazzes = getExtensionClasses();
        return Collections.unmodifiableSet(new TreeSet<String>(clazzes.keySet()));
    }

    /**
     * Return default extension name, return <code>null</code> if not configured.
     */
    public String getDefaultExtensionName() {
        getExtensionClasses();
        return cachedDefaultName;
    }

    /**
     * Register new extension via API
     *
     * @param name  extension name
     * @param clazz extension class
     * @throws IllegalStateException when extension with the same name has already been registered.
     */
    public void addExtension(String name, Class<?> clazz) {
        getExtensionClasses(); // load classes

        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Input type " +
                    clazz + "not implement Extension " + type);
        }
        if (clazz.isInterface()) {
            throw new IllegalStateException("Input type " +
                    clazz + "can not be interface!");
        }

        if (!clazz.isAnnotationPresent(Adaptive.class)) {
            if (StringUtils.isBlank(name)) {
                throw new IllegalStateException("Extension name is blank (Extension " + type + ")!");
            }
            if (cachedClasses.get().containsKey(name)) {
                throw new IllegalStateException("Extension name " +
                        name + " already existed(Extension " + type + ")!");
            }

            cachedNames.put(clazz, name);
            cachedClasses.get().put(name, clazz);
        } else {
            if (cachedAdaptiveClass != null) {
                throw new IllegalStateException("Adaptive Extension already existed(Extension " + type + ")!");
            }

            cachedAdaptiveClass = clazz;
        }
    }

    /**
     * Replace the existing extension via API
     *
     * @param name  extension name
     * @param clazz extension class
     * @throws IllegalStateException when extension to be placed doesn't exist
     * @deprecated not recommended any longer, and use only when test
     */
    @Deprecated
    public void replaceExtension(String name, Class<?> clazz) {
        getExtensionClasses(); // load classes

        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Input type " +
                    clazz + "not implement Extension " + type);
        }
        if (clazz.isInterface()) {
            throw new IllegalStateException("Input type " +
                    clazz + "can not be interface!");
        }

        if (!clazz.isAnnotationPresent(Adaptive.class)) {
            if (StringUtils.isBlank(name)) {
                throw new IllegalStateException("Extension name is blank (Extension " + type + ")!");
            }
            if (!cachedClasses.get().containsKey(name)) {
                throw new IllegalStateException("Extension name " +
                        name + " not existed(Extension " + type + ")!");
            }

            cachedNames.put(clazz, name);
            cachedClasses.get().put(name, clazz);
            cachedInstances.remove(name);
        } else {
            if (cachedAdaptiveClass == null) {
                throw new IllegalStateException("Adaptive Extension not existed(Extension " + type + ")!");
            }

            cachedAdaptiveClass = clazz;
            cachedAdaptiveInstance.set(null);
        }
    }

    /**
     * 获得自适应拓展对象
     *
     *
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public T getAdaptiveExtension() {
        //获得自适应拓展对象
        Object instance = cachedAdaptiveInstance.get();
        if (instance == null) {

            if (createAdaptiveInstanceError == null) {
                synchronized (cachedAdaptiveInstance) {
                    instance = cachedAdaptiveInstance.get();
                    if (instance == null) {
                        try {
                            // 创建自适应拓展对象
                            instance = createAdaptiveExtension();

                            // 设置到缓存
                            cachedAdaptiveInstance.set(instance);
                        } catch (Throwable t) {

                            // 记录异常
                            createAdaptiveInstanceError = t;
                            throw new IllegalStateException("fail to create adaptive instance: " + t.toString(), t);
                        }
                    }
                }

            // 若之前创建报错，则抛出异常 IllegalStateException
            } else {
                throw new IllegalStateException("fail to create adaptive instance: " + createAdaptiveInstanceError.toString(), createAdaptiveInstanceError);
            }
        }

        return (T) instance;
    }

    private IllegalStateException findException(String name) {
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (entry.getKey().toLowerCase().contains(name.toLowerCase())) {
                return entry.getValue();
            }
        }
        StringBuilder buf = new StringBuilder("No such extension " + type.getName() + " by name " + name);


        int i = 1;
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (i == 1) {
                buf.append(", possible causes: ");
            }

            buf.append("\r\n(");
            buf.append(i++);
            buf.append(") ");
            buf.append(entry.getKey());
            buf.append(":\r\n");
            buf.append(StringUtils.toString(entry.getValue()));
        }
        return new IllegalStateException(buf.toString());
    }

    /**
     * 创建拓展名的拓展对象，并缓存
     *
     * @param name 拓展名
     * @return 拓展对象
     */
    @SuppressWarnings("unchecked")
    private T createExtension(String name) {
        // 获得拓展名对应的拓展实现类
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null) {
            throw findException(name);
        }
        try {
            // 从缓存中，获得拓展对象
            T instance = (T) EXTENSION_INSTANCES.get(clazz);
            if (instance == null) {
                // 当缓存不存在时，创建拓展对象，并添加到缓存中。
                EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            }

            // 注入依赖的属性
            injectExtension(instance);

            /**
             * 创建 Wrapper 拓展对象，扩展点自动包装
             *
             * Wrapper 类同样实现了扩展点接口，但是 Wrapper 不是扩展点的真正实现。它的用途主要是用于从 ExtensionLoader 返回扩展点时，
             * 包装在真正的扩展点实现外。即从 ExtensionLoader 中返回的实际上是 Wrapper 类的实例，Wrapper 持有了实际的扩展点实现类。
             *
             * 扩展点的 Wrapper 类可以有多个，也可以根据需要新增。
             *
             * 通过 Wrapper 类可以把所有扩展点公共逻辑移至 Wrapper 中。新加的 Wrapper 在所有的扩展点上添加了逻辑，有些类似 AOP，即 Wrapper 代理了扩展点。
             *
             * 各种wrapper和顺序无关
             */
            Set<Class<?>> wrapperClasses = cachedWrapperClasses;
            if (wrapperClasses != null && !wrapperClasses.isEmpty()) {
                for (Class<?> wrapperClass : wrapperClasses) {

                    //把实际的拓展点service做了一层包装
                    instance = injectExtension((T) wrapperClass.getConstructor(type).newInstance(instance));
                }
            }
            return instance;
        } catch (Throwable t) {
            throw new IllegalStateException("Extension instance(name: " + name + ", class: " +
                    type + ")  could not be instantiated: " + t.getMessage(), t);
        }
    }

    /**
     * 注入依赖的属性
     *
     * @param instance 拓展对象
     * @return 拓展对象
     */
    private T injectExtension(T instance) {
        try {
            if (objectFactory != null) {
                for (Method method : instance.getClass().getMethods()) {
                    if (method.getName().startsWith("set")
                            && method.getParameterTypes().length == 1
                            && Modifier.isPublic(method.getModifiers())) {

                        // 获得属性的类型
                        Class<?> pt = method.getParameterTypes()[0];
                        try {

                            // 获得属性
                            String property = method.getName().length() > 3 ? method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4) : "";

                            // 获得属性值
                            Object object = objectFactory.getExtension(pt, property);

                            // 设置属性值
                            if (object != null) {
                                method.invoke(instance, object);
                            }
                        } catch (Exception e) {
                            logger.error("fail to inject via method " + method.getName()
                                    + " of interface " + type.getName() + ": " + e.getMessage(), e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return instance;
    }

    private Class<?> getExtensionClass(String name) {
        if (type == null)
            throw new IllegalArgumentException("Extension type == null");
        if (name == null)
            throw new IllegalArgumentException("Extension name == null");
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null)
            throw new IllegalStateException("No such extension \"" + name + "\" for " + type.getName() + "!");
        return clazz;
    }

    /**
     * 获得拓展实现类数组， 加载type对应的所有class，缓存到cachedClass中
     *
     * @return 拓展实现类数组
     */
    private Map<String, Class<?>> getExtensionClasses() {
        // 从缓存中，获得拓展实现类数组
        Map<String, Class<?>> classes = cachedClasses.get();
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {

                    // 从配置文件中，加载拓展实现类数组
                    classes = loadExtensionClasses();

                    // 设置到缓存中
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    /**
     * 加载拓展实现类数组
     * 无需声明 synchronized ，因为唯一调用该方法的 {@link #getExtensionClasses()} 已经声明。
     *
     * @return 拓展实现类数组
     */
    // synchronized in getExtensionClasses
    private Map<String, Class<?>> loadExtensionClasses() {

        // 通过 @SPI 注解，获得默认的拓展实现类名
        final SPI defaultAnnotation = type.getAnnotation(SPI.class);
        if (defaultAnnotation != null) {
            String value = defaultAnnotation.value();
            if ((value = value.trim()).length() > 0) {
                String[] names = NAME_SEPARATOR.split(value);
                if (names.length > 1) {
                    throw new IllegalStateException("more than 1 default extension name on extension " + type.getName()
                            + ": " + Arrays.toString(names));
                }
                if (names.length == 1) cachedDefaultName = names[0];
            }
        }

        // 从配置文件中，加载拓展实现类数组
        // 加载先后顺序： DUBBO_INTERNAL_DIRECTORY -> DUBBO_DIRECTORY -> SERVICES_DIRECTORY
        Map<String, Class<?>> extensionClasses = new HashMap<String, Class<?>>();
        loadDirectory(extensionClasses, DUBBO_INTERNAL_DIRECTORY, type.getName());
        loadDirectory(extensionClasses, DUBBO_INTERNAL_DIRECTORY, type.getName().replace("org.apache", "com.alibaba"));
        loadDirectory(extensionClasses, DUBBO_DIRECTORY, type.getName());
        loadDirectory(extensionClasses, DUBBO_DIRECTORY, type.getName().replace("org.apache", "com.alibaba"));
        loadDirectory(extensionClasses, SERVICES_DIRECTORY, type.getName());
        loadDirectory(extensionClasses, SERVICES_DIRECTORY, type.getName().replace("org.apache", "com.alibaba"));
        return extensionClasses;
    }

    /**
     * 从一个配置文件目录中，加载拓展实现类数组。
     * @param extensionClasses 拓展类名数组
     * @param dir 文件名
     * @param type 类型
     */
    private void loadDirectory(Map<String, Class<?>> extensionClasses, String dir, String type) {
        // 完整的文件名
        String fileName = dir + type;
        try {
            Enumeration<java.net.URL> urls;

            // 获得文件名对应的所有文件数组
            ClassLoader classLoader = findClassLoader();
            if (classLoader != null) {
                urls = classLoader.getResources(fileName);
            } else {
                urls = ClassLoader.getSystemResources(fileName);
            }

            // 遍历文件数组
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    java.net.URL resourceURL = urls.nextElement();
                    loadResource(extensionClasses, classLoader, resourceURL);
                }
            }
        } catch (Throwable t) {
            logger.error("Exception when load extension class(interface: " +
                    type + ", description file: " + fileName + ").", t);
        }
    }

    /**
     * 从一个配置文件中，加载拓展实现类数组
     * @param extensionClasses
     * @param classLoader
     * @param resourceURL
     */
    private void loadResource(Map<String, Class<?>> extensionClasses, ClassLoader classLoader, java.net.URL resourceURL) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(resourceURL.openStream(), "utf-8"));
            try {
                String line;
                while ((line = reader.readLine()) != null) {

                    // 跳过当前被注释掉的情况，例如 #spring=xxxxxxxxx
                    final int ci = line.indexOf('#');
                    if (ci >= 0) line = line.substring(0, ci);
                    line = line.trim();
                    if (line.length() > 0) {
                        try {

                            // 拆分，key=value 的配置格式
                            String name = null;
                            int i = line.indexOf('=');
                            if (i > 0) {
                                name = line.substring(0, i).trim();
                                line = line.substring(i + 1).trim();
                            }
                            if (line.length() > 0) {
                                loadClass(extensionClasses, resourceURL, Class.forName(line, true, classLoader), name);
                            }
                        } catch (Throwable t) {
                            IllegalStateException e = new IllegalStateException("Failed to load extension class(interface: " + type + ", class line: " + line + ") in " + resourceURL + ", cause: " + t.getMessage(), t);
                            exceptions.put(line, e);
                        }
                    }
                }
            } finally {
                reader.close();
            }
        } catch (Throwable t) {
            logger.error("Exception when load extension class(interface: " +
                    type + ", class file: " + resourceURL + ") in " + resourceURL, t);
        }
    }

    /**
     * 加载 extensionClass
     * @param extensionClasses
     * @param resourceURL
     * @param clazz
     * @param name
     * @throws NoSuchMethodException
     */
    private void loadClass(Map<String, Class<?>> extensionClasses, java.net.URL resourceURL, Class<?> clazz, String name) throws NoSuchMethodException {

        // 判断拓展实现，是否实现拓展接口
        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Error when load extension class(interface: " +
                    type + ", class line: " + clazz.getName() + "), class "
                    + clazz.getName() + "is not subtype of interface.");
        }

        // 如果类标注有@Adaptive, 说明是自适应拓展点, 缓存自适应拓展对象的类到 `cachedAdaptiveClass`
        /**
         * 这里说明：
         *      1. 当Adaptive标注到Class， 我们去使用AdaptiveExtension时，可以从cachedAdaptiveClass中获得对应的Extension，而不用去动态生成Class对象
         *      2. 当Adaptive标注到Method，我们使用AdaptiveExtension时，动态生成Class对象，生成对象时，遍历type的Method，当method标注有Adaptive时，
         *          根据URL的类型再次获取相关的Extension（这种获取过程可能需要多次，但是这是直线型的调用），最后再调用相关方法；
         *          当Method上没有标注Adaptive时，如果调用该方法抛出异常；
         *      3. 优先使用class上的Adaptive注解，因为加载AdaptiveExtension时，也会loadClass，也会执行这个流程，再判断cachedAdaptiveClass是否为空，选择是否动态写类
         */
        if (clazz.isAnnotationPresent(Adaptive.class)) {
            if (cachedAdaptiveClass == null) {
                cachedAdaptiveClass = clazz;
            } else if (!cachedAdaptiveClass.equals(clazz)) {
                throw new IllegalStateException("More than 1 adaptive class found: "
                        + cachedAdaptiveClass.getClass().getName()
                        + ", " + clazz.getClass().getName());
            }

            /**
             * 缓存拓展 Wrapper 实现类到 `cachedWrapperClasses`
             *
             * 如果该类有拷贝构造函数，就是wrapper类
             *
             */
        } else if (isWrapperClass(clazz)) {
            Set<Class<?>> wrappers = cachedWrapperClasses;
            if (wrappers == null) {
                cachedWrapperClasses = new ConcurrentHashSet<Class<?>>();
                wrappers = cachedWrapperClasses;
            }
            wrappers.add(clazz);

        // 缓存拓展实现类到 `extensionClasses`
        } else {
            clazz.getConstructor();

            // 未配置拓展名，自动生成。例如，DemoFilter 为 demo 。主要用于兼容 Java SPI 的配置
            if (name == null || name.length() == 0) {
                name = findAnnotationName(clazz);
                if (name.length() == 0) {
                    throw new IllegalStateException("No such extension name for the class " + clazz.getName() + " in the config " + resourceURL);
                }
            }

            // 获得拓展名，可以是数组，有多个拓展名
            String[] names = NAME_SEPARATOR.split(name);
            if (names != null && names.length > 0) {

                // 缓存 @Activate 到 `cachedActivates`
                Activate activate = clazz.getAnnotation(Activate.class);
                if (activate != null) {
                    cachedActivates.put(names[0], activate);
                } else {
                    // support com.alibaba.dubbo.common.extension.Activate
                    com.alibaba.dubbo.common.extension.Activate oldActivate = clazz.getAnnotation(com.alibaba.dubbo.common.extension.Activate.class);
                    if (oldActivate != null) {
                        cachedActivates.put(names[0], oldActivate);
                    }
                }

                for (String n : names) {
                    // 缓存到 `cachedNames`
                    if (!cachedNames.containsKey(clazz)) {
                        cachedNames.put(clazz, n);
                    }

                    // 缓存拓展实现类到 `extensionClasses`
                    // 相同拓展名，不能对应多个不同的拓展实现。
                    Class<?> c = extensionClasses.get(n);
                    if (c == null) {
                        extensionClasses.put(n, clazz);
                    } else if (c != clazz) {
                        throw new IllegalStateException("Duplicate extension " + type.getName() + " name " + n + " on " + c.getName() + " and " + clazz.getName());
                    }
                }
            }
        }
    }

    /**
     * 判断当前配置的拓展实现类为拓展 Wrapper 实现类
     *
     * 通过反射的方式，参数为拓展接口
     *
     * 若成功（未抛出异常），则代表符合条件, 例如，ProtocolFilterWrapper(Protocol protocol) 这个构造方法
     *
     * 如果当前类有拷贝构造方法，则表明是一个包装类
     *
     * @param clazz
     * @return
     */
    private boolean isWrapperClass(Class<?> clazz) {
        try {
            clazz.getConstructor(type);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    private String findAnnotationName(Class<?> clazz) {
        org.apache.dubbo.common.Extension extension = clazz.getAnnotation(org.apache.dubbo.common.Extension.class);
        if (extension == null) {
            String name = clazz.getSimpleName();
            if (name.endsWith(type.getSimpleName())) {
                name = name.substring(0, name.length() - type.getSimpleName().length());
            }
            return name.toLowerCase();
        }
        return extension.value();
    }

    /**
     * 创建自适应拓展对象
     * @return 拓展对象
     */
    @SuppressWarnings("unchecked")
    private T createAdaptiveExtension() {
        try {
            //生成AdaptiveExtension类，缓存到cachedAdaptiveClass，并生成一个实例
            T target = (T) getAdaptiveExtensionClass().newInstance();

            return injectExtension(target);
        } catch (Exception e) {
            throw new IllegalStateException("Can not create adaptive extension " + type + ", cause: " + e.getMessage(), e);
        }
    }

    /**
     * 1. 加载type对应的所有拓展点的class对象到cachedClass Holder中
     * 2. 如果没有自适应的扩展点，调用createAdaptiveExtensionClass生成一个自适应的类
     * 3. 生成的自适应类名称规范为：typeName$Adaptive
     * 4. 实际上，自适应类只是一个包装类，它实现了对应SPI接口的方法；
     *      每当调用typeName$Adaptive类的方法时，如果SPI接口方法没有@Adaptive注解，表示不能被代理，抛出异常
     *          如果SPI接口方法有@Adaptive注解，表示可以代理，再从URL中拿到名称（比如dubbo协议拓展实现）extName，
     *              再调用ExtensionLoader.getExtensionLoader(org.apache.dubbo.rpc.cluster.Cluster.class).getExtension(extName)，获得具体的服务
     *              再调用该服务的该接口方法，完成调用链
     *
     *  5. 对于URL的获取，要么参数有URL，要么传入参数对象中有URL
     *
     *  6. URL贯穿整个项目，从URL中可以拿到某个拓展点名称，不管是默认名称，还是我们配置的名称，这是一个规定
     *
     * @return 自适应拓展类
     */
    private Class<?> getAdaptiveExtensionClass() {
        //扫描配置文件，获取所有type的实现类，缓存到cachedClass Holder中
        //一个type对应的所有扩展点均已加载完毕
        getExtensionClasses();

        if (cachedAdaptiveClass != null) {
            //若 cachedAdaptiveClass 已存在，直接返回
            return cachedAdaptiveClass;
        }

        //若 cachedAdaptiveClass 不存在, 调用 #createAdaptiveExtensionClass() 方法，自动生成自适应拓展的代码实现，并编译后返回该类
        return cachedAdaptiveClass = createAdaptiveExtensionClass();
    }

    /**
     * 自动生成自适应拓展的代码实现的字符串， 编译, 加载
     * @return
     */
    private Class<?> createAdaptiveExtensionClass() {
        //生成AdaptiveExtension代码
        String code = createAdaptiveExtensionClassCode();

        ClassLoader classLoader = findClassLoader();

        org.apache.dubbo.common.compiler.Compiler compiler = ExtensionLoader.getExtensionLoader(org.apache.dubbo.common.compiler.Compiler.class).getAdaptiveExtension();
        return compiler.compile(code, classLoader);
    }

    /**
     * 自动生成自适应拓展的代码实现的字符串
     * @return
     */
    private String createAdaptiveExtensionClassCode() {
        StringBuilder codeBuilder = new StringBuilder();
        Method[] methods = type.getMethods();
        boolean hasAdaptiveAnnotation = false;
        for (Method m : methods) {
            if (m.isAnnotationPresent(Adaptive.class)) {
                hasAdaptiveAnnotation = true;
                break;
            }
        }
        // no need to generate adaptive class since there's no adaptive method found.
        if (!hasAdaptiveAnnotation)
            throw new IllegalStateException("No adaptive method on extension " + type.getName() + ", refuse to create the adaptive class!");

        codeBuilder.append("package ").append(type.getPackage().getName()).append(";");
        codeBuilder.append("\nimport ").append(ExtensionLoader.class.getName()).append(";");
        codeBuilder.append("\npublic class ").append(type.getSimpleName()).append("$Adaptive").append(" implements ").append(type.getCanonicalName()).append(" {");

        codeBuilder.append("\nprivate static final org.apache.dubbo.common.logger.Logger logger = org.apache.dubbo.common.logger.LoggerFactory.getLogger(ExtensionLoader.class);");
        codeBuilder.append("\nprivate java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);\n");

        for (Method method : methods) {
            Class<?> rt = method.getReturnType();
            Class<?>[] pts = method.getParameterTypes();
            Class<?>[] ets = method.getExceptionTypes();

            Adaptive adaptiveAnnotation = method.getAnnotation(Adaptive.class);
            StringBuilder code = new StringBuilder(512);
            if (adaptiveAnnotation == null) {
                code.append("throw new UnsupportedOperationException(\"method ")
                        .append(method.toString()).append(" of interface ")
                        .append(type.getName()).append(" is not adaptive method!\");");
            } else {
                int urlTypeIndex = -1;
                for (int i = 0; i < pts.length; ++i) {
                    if (pts[i].equals(URL.class)) {
                        urlTypeIndex = i;
                        break;
                    }
                }
                // found parameter in URL type
                if (urlTypeIndex != -1) {
                    // Null Point check
                    String s = String.format("\nif (arg%d == null) throw new IllegalArgumentException(\"url == null\");",
                            urlTypeIndex);
                    code.append(s);

                    s = String.format("\n%s url = arg%d;", URL.class.getName(), urlTypeIndex);
                    code.append(s);
                }
                // did not find parameter in URL type
                else {
                    String attribMethod = null;

                    // find URL getter method
                    LBL_PTS:
                    for (int i = 0; i < pts.length; ++i) {
                        Method[] ms = pts[i].getMethods();
                        for (Method m : ms) {
                            String name = m.getName();
                            if ((name.startsWith("get") || name.length() > 3)
                                    && Modifier.isPublic(m.getModifiers())
                                    && !Modifier.isStatic(m.getModifiers())
                                    && m.getParameterTypes().length == 0
                                    && m.getReturnType() == URL.class) {
                                urlTypeIndex = i;
                                attribMethod = name;
                                break LBL_PTS;
                            }
                        }
                    }
                    if (attribMethod == null) {
                        throw new IllegalStateException("fail to create adaptive class for interface " + type.getName()
                                + ": not found url parameter or url attribute in parameters of method " + method.getName());
                    }

                    // Null point check
                    String s = String.format("\nif (arg%d == null) throw new IllegalArgumentException(\"%s argument == null\");",
                            urlTypeIndex, pts[urlTypeIndex].getName());
                    code.append(s);
                    s = String.format("\nif (arg%d.%s() == null) throw new IllegalArgumentException(\"%s argument %s() == null\");",
                            urlTypeIndex, attribMethod, pts[urlTypeIndex].getName(), attribMethod);
                    code.append(s);

                    s = String.format("%s url = arg%d.%s();", URL.class.getName(), urlTypeIndex, attribMethod);
                    code.append(s);
                }

                String[] value = adaptiveAnnotation.value();
                // value is not set, use the value generated from class name as the key
                if (value.length == 0) {
                    char[] charArray = type.getSimpleName().toCharArray();
                    StringBuilder sb = new StringBuilder(128);
                    for (int i = 0; i < charArray.length; i++) {
                        if (Character.isUpperCase(charArray[i])) {
                            if (i != 0) {
                                sb.append(".");
                            }
                            sb.append(Character.toLowerCase(charArray[i]));
                        } else {
                            sb.append(charArray[i]);
                        }
                    }
                    value = new String[]{sb.toString()};
                }

                boolean hasInvocation = false;
                for (int i = 0; i < pts.length; ++i) {
                    if (pts[i].getName().equals("org.apache.dubbo.rpc.Invocation")) {
                        // Null Point check
                        String s = String.format("\nif (arg%d == null) throw new IllegalArgumentException(\"invocation == null\");", i);
                        code.append(s);
                        s = String.format("\nString methodName = arg%d.getMethodName();", i);
                        code.append(s);
                        hasInvocation = true;
                        break;
                    }
                }

                String defaultExtName = cachedDefaultName;
                String getNameCode = null;
                for (int i = value.length - 1; i >= 0; --i) {
                    if (i == value.length - 1) {
                        if (null != defaultExtName) {
                            if (!"protocol".equals(value[i]))
                                if (hasInvocation)
                                    getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName);
                                else
                                    getNameCode = String.format("url.getParameter(\"%s\", \"%s\")", value[i], defaultExtName);
                            else
                                getNameCode = String.format("( url.getProtocol() == null ? \"%s\" : url.getProtocol() )", defaultExtName);
                        } else {
                            if (!"protocol".equals(value[i]))
                                if (hasInvocation)
                                    getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName);
                                else
                                    getNameCode = String.format("url.getParameter(\"%s\")", value[i]);
                            else
                                getNameCode = "url.getProtocol()";
                        }
                    } else {
                        if (!"protocol".equals(value[i]))
                            if (hasInvocation)
                                getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName);
                            else
                                getNameCode = String.format("url.getParameter(\"%s\", %s)", value[i], getNameCode);
                        else
                            getNameCode = String.format("url.getProtocol() == null ? (%s) : url.getProtocol()", getNameCode);
                    }
                }
                code.append("\nString extName = ").append(getNameCode).append(";");
                // check extName == null?
                String s = String.format("\nif(extName == null) " +
                                "throw new IllegalStateException(\"Fail to get extension(%s) name from url(\" + url.toString() + \") use keys(%s)\");",
                        type.getName(), Arrays.toString(value));
                code.append(s);

                code.append(String.format("\n%s extension = null;\n try {\nextension = (%<s)%s.getExtensionLoader(%s.class).getExtension(extName);\n}catch(Exception e){\n",
                        type.getName(), ExtensionLoader.class.getSimpleName(), type.getName()));
                code.append(String.format("if (count.incrementAndGet() == 1) {\nlogger.warn(\"Failed to find extension named \" + extName + \" for type %s, will use default extension %s instead.\", e);\n}\n",
                        type.getName(), defaultExtName));
                code.append(String.format("extension = (%s)%s.getExtensionLoader(%s.class).getExtension(\"%s\");\n}",
                        type.getName(), ExtensionLoader.class.getSimpleName(), type.getName(), defaultExtName));

                // return statement
                if (!rt.equals(void.class)) {
                    code.append("\nreturn ");
                }

                s = String.format("extension.%s(", method.getName());
                code.append(s);
                for (int i = 0; i < pts.length; i++) {
                    if (i != 0)
                        code.append(", ");
                    code.append("arg").append(i);
                }
                code.append(");");
            }

            codeBuilder.append("\npublic ").append(rt.getCanonicalName()).append(" ").append(method.getName()).append("(");
            for (int i = 0; i < pts.length; i++) {
                if (i > 0) {
                    codeBuilder.append(", ");
                }
                codeBuilder.append(pts[i].getCanonicalName());
                codeBuilder.append(" ");
                codeBuilder.append("arg").append(i);
            }
            codeBuilder.append(")");
            if (ets.length > 0) {
                codeBuilder.append(" throws ");
                for (int i = 0; i < ets.length; i++) {
                    if (i > 0) {
                        codeBuilder.append(", ");
                    }
                    codeBuilder.append(ets[i].getCanonicalName());
                }
            }
            codeBuilder.append(" {");
            codeBuilder.append(code.toString());
            codeBuilder.append("\n}");
        }
        codeBuilder.append("\n}");
        if (logger.isDebugEnabled()) {
            logger.debug(codeBuilder.toString());
        }
        return codeBuilder.toString();
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "[" + type.getName() + "]";
    }

}