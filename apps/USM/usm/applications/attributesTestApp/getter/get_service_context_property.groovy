import java.util.concurrent.TimeUnit

println "get_service_context_property"

def context = com.gigaspaces.cloudify.dsl.context.ServiceContextFactory.getServiceContext()

context.attributes.thisService["myKey"];
