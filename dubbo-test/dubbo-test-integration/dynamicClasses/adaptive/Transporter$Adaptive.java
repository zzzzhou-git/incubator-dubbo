package org.apache.dubbo.remoting;

import org.apache.dubbo.common.extension.ExtensionLoader;

public class Transporter$Adaptive implements org.apache.dubbo.remoting.Transporter {

    private static final org.apache.dubbo.common.logger.Logger logger = org.apache.dubbo.common.logger.LoggerFactory.getLogger(ExtensionLoader.class);

    private java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);

    public org.apache.dubbo.remoting.Client connect(org.apache.dubbo.common.URL arg0, org.apache.dubbo.remoting.ChannelHandler arg1) throws org.apache.dubbo.remoting.RemotingException {
        if (arg0 == null) throw new IllegalArgumentException("url == null");
        org.apache.dubbo.common.URL url = arg0;
        String extName = url.getParameter("client", url.getParameter("transporter", "netty"));
        if (extName == null)
            throw new IllegalStateException("Fail to get extension(org.apache.dubbo.remoting.Transporter) name from url(" + url.toString() + ") use keys([client, transporter])");
        org.apache.dubbo.remoting.Transporter extension = null;
        try {
            extension = (org.apache.dubbo.remoting.Transporter) ExtensionLoader.getExtensionLoader(org.apache.dubbo.remoting.Transporter.class).getExtension(extName);
        } catch (Exception e) {
            if (count.incrementAndGet() == 1) {
                logger.warn("Failed to find extension named " + extName + " for type org.apache.dubbo.remoting.Transporter, will use default extension netty instead.", e);
            }
            extension = (org.apache.dubbo.remoting.Transporter) ExtensionLoader.getExtensionLoader(org.apache.dubbo.remoting.Transporter.class).getExtension("netty");
        }
        return extension.connect(arg0, arg1);
    }

    public org.apache.dubbo.remoting.Server bind(org.apache.dubbo.common.URL arg0, org.apache.dubbo.remoting.ChannelHandler arg1) throws org.apache.dubbo.remoting.RemotingException {
        if (arg0 == null) throw new IllegalArgumentException("url == null");
        org.apache.dubbo.common.URL url = arg0;
        String extName = url.getParameter("server", url.getParameter("transporter", "netty"));
        if (extName == null)
            throw new IllegalStateException("Fail to get extension(org.apache.dubbo.remoting.Transporter) name from url(" + url.toString() + ") use keys([server, transporter])");
        org.apache.dubbo.remoting.Transporter extension = null;
        try {
            extension = (org.apache.dubbo.remoting.Transporter) ExtensionLoader.getExtensionLoader(org.apache.dubbo.remoting.Transporter.class).getExtension(extName);
        } catch (Exception e) {
            if (count.incrementAndGet() == 1) {
                logger.warn("Failed to find extension named " + extName + " for type org.apache.dubbo.remoting.Transporter, will use default extension netty instead.", e);
            }
            extension = (org.apache.dubbo.remoting.Transporter) ExtensionLoader.getExtensionLoader(org.apache.dubbo.remoting.Transporter.class).getExtension("netty");
        }
        return extension.bind(arg0, arg1);
    }
}