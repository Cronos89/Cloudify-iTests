import java.util.concurrent.TimeUnit


service {
	name "groovy2"
	icon "icon.png"
	type "WEB_SERVER"
	elastic true
	lifecycle { 
	
		init { println "This is the init event" }
		preInstall {println "This is the preInstall event" }
		postInstall {println "This is the postInstall event"}
		preStart {println "This is the preStart event" }
		
		start "run.groovy" 
		
		postStart {println "This is the postStart event" }
		preStop {println "This is the preStop event" }
		postStop {println "This is the postStop event" }
		shutdown {println "This is the shutdown event" }
	}

	customCommands ([
				"echo" : {x ->
					return x
				},

				"contextInvoke": { x ->
					Object[] results =
							context.waitForService("groovy", 10, TimeUnit.SECONDS)
							.invoke("echo", x + " from " + context.instanceId )
					return java.util.Arrays.toString(results)
				}



			])
}