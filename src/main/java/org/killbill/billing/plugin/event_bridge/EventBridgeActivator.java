package org.killbill.billing.plugin.event_bridge;

import java.util.Hashtable;
import java.util.Dictionary;
import javax.servlet.Servlet;
import org.killbill.billing.osgi.api.Healthcheck;
import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillEventDispatcher.OSGIFrameworkEventHandler;
import org.osgi.framework.BundleContext;

public class EventBridgeActivator extends KillbillActivatorBase {

    public static final String PLUGIN_NAME = "eventbridge-plugin";
    private EventBridgeEventListener ebListener;

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        this.ebListener = new EventBridgeEventListener(killbillAPI);

        final EventBridgeHealthcheck healthcheck = new EventBridgeHealthcheck();
        final Hashtable<String, String> props = new Hashtable<>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        
        context.registerService(Healthcheck.class.getName(), healthcheck, (Dictionary) props);
        context.registerService(Servlet.class.getName(), new EventBridgeHealthcheckServlet(healthcheck), (Dictionary) props);

        registerHandlers();
    }

    private void registerHandlers() {
        dispatcher.registerEventHandlers((OSGIFrameworkEventHandler) () -> 
            dispatcher.registerEventHandlers(ebListener));
    }
}