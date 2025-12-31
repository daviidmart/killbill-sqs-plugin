package org.killbill.billing.plugin.event_bridge;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.killbill.billing.osgi.api.Healthcheck.HealthStatus;

public class EventBridgeHealthcheckServlet extends HttpServlet {

    private final EventBridgeHealthcheck healthcheck;

    public EventBridgeHealthcheckServlet(EventBridgeHealthcheck healthcheck) {
        this.healthcheck = healthcheck;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Ejecutamos la lógica de salud
        HealthStatus status = healthcheck.getHealthStatus(null, null);
        
        if (status.isHealthy()) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().print("{\"status\": \"healthy\", \"details\": \"" + status.getDetails() + "\"}");
        } else {
            resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            resp.getWriter().print("{\"status\": \"unhealthy\"}");
        }
        
        resp.setContentType("application/json");
    }
}