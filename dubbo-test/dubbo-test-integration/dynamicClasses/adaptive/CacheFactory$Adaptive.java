package org.apache.dubbo.cache;

import org.apache.dubbo.common.extension.ExtensionLoader;

public class CacheFactory$Adaptive implements org.apache.dubbo.cache.CacheFactory {

    private static final org.apache.dubbo.common.logger.Logger logger = org.apache.dubbo.common.logger.LoggerFactory.getLogger(ExtensionLoader.class);

    private java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);

    public org.apache.dubbo.cache.Cache getCache(org.apache.dubbo.common.URL arg0, org.apache.dubbo.rpc.Invocation arg1) {
        if (arg0 == null)
            throw new IllegalArgumentException("url == null");

        org.apache.dubbo.common.URL url = arg0;
        if (arg1 == null)
            throw new IllegalArgumentException("invocation == null");

        String methodName = arg1.getMethodName();
        String extName = url.getMethodParameter(methodName, "cache", "lru");
        if(extName == null)
            throw new IllegalStateException("Fail to get extension(org.apache.dubbo.cache.CacheFactory) name from url(" + url.toString() + ") use keys([cache])");

        org.apache.dubbo.cache.CacheFactory extension = null;
        try {
            extension = (org.apache.dubbo.cache.CacheFactory)ExtensionLoader.getExtensionLoader(org.apache.dubbo.cache.CacheFactory.class).getExtension(extName);
        } catch(Exception e) {
            if (count.incrementAndGet() == 1) {
                logger.warn("Failed to find extension named " + extName + " for type org.apache.dubbo.cache.CacheFactory, will use default extension lru instead.", e);
            }

            extension = (org.apache.dubbo.cache.CacheFactory)ExtensionLoader.getExtensionLoader(org.apache.dubbo.cache.CacheFactory.class).getExtension("lru");
        }

        return extension.getCache(arg0, arg1);
    }
}