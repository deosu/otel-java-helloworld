package com.vmware;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sumit Deo (deosu@vmware.com)
 */
public class App {

    private final static Logger logger = LoggerFactory.getLogger(App.class);
    public static final String OTEL_RESOURCE_ATTRIBUTES_KEY = "otel.resource.attributes";
    public static final String OTEL_RESOURCE_ATTRIBUTES_VALUE = "service.name=HelloWorldJavaService";
    public static final String INSTRUMENTATION_LIBRARY_NAME = "instrumentation-library-name";
    public static final String INSTRUMENTATION_VERSION = "1.0.0";
    static Tracer tracer;

    public static void main(String[] args) throws InterruptedException {

        System.setProperty(OTEL_RESOURCE_ATTRIBUTES_KEY, OTEL_RESOURCE_ATTRIBUTES_VALUE);
        tracer = getTracer();

        for (int index = 0; index < 3; index++) {
            Span parentSpan = tracer.spanBuilder("parentSpan").startSpan();
            logger.info("In parent method. TraceID : {}", parentSpan.getSpanContext().getTraceIdAsHexString());
            try (Scope scope = parentSpan.makeCurrent()) {
                parentSpan.setAttribute("parentIndex", index);
                childMethod(parentSpan);
            } catch (Throwable throwable) {
                parentSpan.setStatus(StatusCode.ERROR, "Something wrong with the parent span");
            } finally {
                parentSpan.end();
            }
        }

        Thread.sleep(2000);
    }

    private static void childMethod(Span parentSpan) {

        tracer = getTracer();

        Span childSpan = tracer.spanBuilder("childSpan").setParent(Context.current().with(parentSpan))
                .startSpan();
        logger.info("In child method. TraceID : {}", childSpan.getSpanContext().getTraceIdAsHexString());
        try (Scope scope = childSpan.makeCurrent()) {
            Thread.sleep(1000);
        } catch (Throwable throwable) {
            childSpan.setStatus(StatusCode.ERROR, "Something wrong with the child span");
        } finally {
            childSpan.end();
        }
    }

    private static Tracer getTracer() {
        if (tracer == null) {
            OpenTelemetry openTelemetry = OTelConfig.initOpenTelemetry();

            tracer = openTelemetry.getTracer(INSTRUMENTATION_LIBRARY_NAME, INSTRUMENTATION_VERSION);
        }

        return tracer;
    }
}
