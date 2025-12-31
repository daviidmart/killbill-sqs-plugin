package org.killbill.billing.plugin.event_bridge;

import org.killbill.billing.osgi.api.Healthcheck;
import org.killbill.billing.tenant.api.Tenant;
import java.util.Map;

public class EventBridgeHealthcheck implements Healthcheck {

    @Override
    public HealthStatus getHealthStatus(Tenant tenant, Map properties) {
        // Aquí podrías incluso intentar una llamada ligera a AWS para verificar conectividad
        // Por ahora, devolvemos que está sano si el plugin cargó.
        return HealthStatus.healthy("EventBridge Plugin is up and running");
    }
}