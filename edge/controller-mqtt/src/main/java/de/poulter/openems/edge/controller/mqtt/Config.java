package de.poulter.openems.edge.controller.mqtt;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
    name = "Mqtt",
    description = "Implements mqtt."
)
@interface Config {

    @AttributeDefinition(name = "Component-ID", description = "Unique ID of this component")
    String id() default "ctrlControllerApiWritableMqtt";

    @AttributeDefinition(name = "Alias", description = "Human-readable name of this component; defaults to component-id")
    String alias() default "";

    @AttributeDefinition(name = "Is enabled?", description = "Is this component enabled?")
    boolean enabled() default true;

    @AttributeDefinition(name = "Client-ID", description = "Client-ID for authentication at MQTT broker")
    String clientId() default "edge0writer";

    @AttributeDefinition(name = "Edge-ID", description = "Edge-ID for subscriptions")
    String edgeId() default "edge0";

    @AttributeDefinition(name = "Topic prefix", description = "Optional topic prefix (<topic_prefix>/edge/<edge_id>/...)")
    String topicPrefix() default "";

    @AttributeDefinition(name = "Username", description = "Username for authentication at MQTT broker")
    String username();

    @AttributeDefinition(name = "Password", description = "Password for authentication at MQTT broker", type = AttributeType.PASSWORD)
    String password();

    @AttributeDefinition(name = "Uri", description = "The connection Uri to MQTT broker.")
    String uri() default "tcp://localhost:1883";

    @AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
    boolean debugMode() default false;

    String webconsole_configurationFactory_nameHint() default "Controller Api Writable MQTT [{id}]";

}
