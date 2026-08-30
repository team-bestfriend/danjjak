package com.bestfriend.danjjak.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.bestfriend.danjjak.health.controller.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

class SpringContextSmokeTest {

    @Test
    void loadsRootAndServletContexts() {
        try (AnnotationConfigApplicationContext rootContext =
                new AnnotationConfigApplicationContext(RootConfig.class)) {
            try (AnnotationConfigWebApplicationContext servletContext =
                    new AnnotationConfigWebApplicationContext()) {
                servletContext.setParent(rootContext);
                servletContext.setServletContext(new MockServletContext());
                servletContext.register(WebConfig.class);
                servletContext.refresh();

                assertNotNull(servletContext.getBean(HealthController.class));
            }
        }
    }
}
