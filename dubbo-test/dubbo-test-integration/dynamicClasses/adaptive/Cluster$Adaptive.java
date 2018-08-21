package org.apache.dubbo.rpc.cluster;

import org.apache.dubbo.common.extension.ExtensionLoader;

public class Cluster$Adaptive implements org.apache.dubbo.rpc.cluster.Cluster {

    private static final org.apache.dubbo.common.logger.Logger logger = org.apache.dubbo.common.logger.LoggerFactory.getLogger(ExtensionLoader.class);

    private java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);

    public org.apache.dubbo.rpc.Invoker join(org.apache.dubbo.rpc.cluster.Directory arg0) throws org.apache.dubbo.rpc.RpcException {
        if (arg0 == null)
            throw new IllegalArgumentException("org.apache.dubbo.rpc.cluster.Directory argument == null");

        if (arg0.getUrl() == null)
            throw new IllegalArgumentException("org.apache.dubbo.rpc.cluster.Directory argument getUrl() == null");org.apache.dubbo.common.URL url = arg0.getUrl();

        String extName = url.getParameter("cluster", "failover");
        if(extName == null)
            throw new IllegalStateException("Fail to get extension(org.apache.dubbo.rpc.cluster.Cluster) name from url(" + url.toString() + ") use keys([cluster])");

        org.apache.dubbo.rpc.cluster.Cluster extension = null;
        try {
            extension = (org.apache.dubbo.rpc.cluster.Cluster)ExtensionLoader.getExtensionLoader(org.apache.dubbo.rpc.cluster.Cluster.class).getExtension(extName);
        } catch(Exception e) {
            if (count.incrementAndGet() == 1) {
                logger.warn("Failed to find extension named " + extName + " for type org.apache.dubbo.rpc.cluster.Cluster, will use default extension failover instead.", e);
            }

            extension = (org.apache.dubbo.rpc.cluster.Cluster)ExtensionLoader.getExtensionLoader(org.apache.dubbo.rpc.cluster.Cluster.class).getExtension("failover");
        }

        return extension.join(arg0);
    }
}