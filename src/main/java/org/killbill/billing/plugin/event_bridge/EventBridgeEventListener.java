package org.killbill.billing.plugin.event_bridge;

import java.util.Properties;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.killbill.billing.notification.plugin.api.ExtBusEvent;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillEventDispatcher.OSGIKillbillEventHandler;
import org.killbill.billing.util.callcontext.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient; // IMPORTANTE
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

public class EventBridgeEventListener implements OSGIKillbillEventHandler {

    private final OSGIKillbillAPI killbillAPI;
    private final ObjectMapper mapper = new ObjectMapper();

    public EventBridgeEventListener(OSGIKillbillAPI killbillAPI) {
        this.killbillAPI = killbillAPI;
    }

    @Override
    public void handleKillbillEvent(ExtBusEvent event) {
        if (event.getTenantId() == null) return;

        Properties props = getTenantConfig(event.getTenantId());
        
        if (props == null || props.isEmpty()) {
            // Este log te dirá si el plugin NO está encontrando tu configuración subida por API
            System.out.println("EB-PLUGIN: No se encontró configuración para el Tenant " + event.getTenantId());
            return;
        }

        String filter = props.getProperty("event_bridge.events_filter", "ALL");
        String currentEvent = event.getEventType().toString();

        if ("ALL".equalsIgnoreCase(filter.trim()) || Arrays.asList(filter.split(",")).contains(currentEvent)) {
            System.out.println("EB-PLUGIN: Intentando enviar evento a EventBridge: " + currentEvent);
            sendToEventBridge(event, props);
        }
    }

    private Properties getTenantConfig(final UUID tenantId) {
        try {
            TenantContext context = new TenantContext() {
                @Override public UUID getTenantId() { return tenantId; }
                @Override public UUID getAccountId() { return null; }
            };
            
            List<String> values = killbillAPI.getTenantUserApi().getTenantValuesForKey("PLUGIN_CONFIG_eventbridge-plugin", context);
            
            if (values == null || values.isEmpty()) return null;

            Properties props = new Properties();
            props.load(new StringReader(values.get(values.size() - 1)));
            return props;
        } catch (Exception e) {
            System.err.println("EB-PLUGIN: Error leyendo configuración del Tenant: " + e.getMessage());
            return null;
        }
    }

    private void sendToEventBridge(ExtBusEvent event, Properties props) {
        EventBridgeClient client = null;
        try {
            String region = props.getProperty("event_bridge.region", "us-east-1");
            String busName = props.getProperty("event_bridge.bus_name", "default");
            String accessKey = props.getProperty("event_bridge.access_key");
            String secretKey = props.getProperty("event_bridge.secret_key");

            if (accessKey == null || secretKey == null) {
                System.err.println("EB-PLUGIN: Faltan llaves AWS en la configuración.");
                return;
            }

            client = EventBridgeClient.builder()
                    .region(Region.of(region))
                    // ESTO SOLUCIONA EL ERROR "Unable to load an HTTP implementation"
                    .httpClient(UrlConnectionHttpClient.builder().build())
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .build();

            String payload = mapper.writeValueAsString(event);
            PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
                    .eventBusName(busName)
                    .source("killbill")
                    .detailType(event.getEventType().toString())
                    .detail(payload)
                    .build();

            client.putEvents(PutEventsRequest.builder().entries(entry).build());
            System.out.println("EB-PLUGIN: Evento enviado exitosamente a EventBridge.");
        } catch (Exception e) {
            System.err.println("EB-PLUGIN Error enviando a AWS: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (client != null) client.close();
        }
    }
}