import java.util.concurrent.TimeUnit

println "get_instance_context_property"

def context = com.gigaspaces.cloudify.dsl.context.ServiceContextFactory.getServiceContext()

context.attributes.thisInstance["myKey"];
