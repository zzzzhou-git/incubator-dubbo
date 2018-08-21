package org.apache.dubbo.validation;

import org.apache.dubbo.common.extension.ExtensionLoader;

public class Validation$Adaptive implements org.apache.dubbo.validation.Validation {

    private static final org.apache.dubbo.common.logger.Logger logger = org.apache.dubbo.common.logger.LoggerFactory.getLogger(ExtensionLoader.class);

    private java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);

    public org.apache.dubbo.validation.Validator getValidator(org.apache.dubbo.common.URL arg0) {
        if (arg0 == null)
            throw new IllegalArgumentException("url == null");

        org.apache.dubbo.common.URL url = arg0;
        String extName = url.getParameter("validation", "jvalidation");
        if(extName == null)
            throw new IllegalStateException("Fail to get extension(org.apache.dubbo.validation.Validation) name from url(" + url.toString() + ") use keys([validation])");

        org.apache.dubbo.validation.Validation extension = null;
        try {
            extension = (org.apache.dubbo.validation.Validation)ExtensionLoader.getExtensionLoader(org.apache.dubbo.validation.Validation.class).getExtension(extName);
        } catch(Exception e) {
            if (count.incrementAndGet() == 1) {
                logger.warn("Failed to find extension named " + extName + " for type org.apache.dubbo.validation.Validation, will use default extension jvalidation instead.", e);
            }
            extension = (org.apache.dubbo.validation.Validation)ExtensionLoader.getExtensionLoader(org.apache.dubbo.validation.Validation.class).getExtension("jvalidation");
        }

        return extension.getValidator(arg0);
    }
}