
service {
	name "tomcat"
	type "WEB_SERVER"
	numInstances 1
	lifecycle{

		init "tomcat_install.groovy"
		start "tomcat_start.groovy"
		preStop "tomcat_stop.groovy"
	}

	customCommands ([
				"updateWar" : "update_war.groovy"
			])

	plugins([
		plugin {
			name "jmx"
			className "org.cloudifysource.usm.jmx.JmxMonitor"
			config([
						"Current Http Threads Busy": [
							"Catalina:type=ThreadPool,name=\"http-bio-8080\"",
							"currentThreadsBusy"
						],
						"Current Http Threads count": [
							"Catalina:type=ThreadPool,name=\"http-bio-8080\"",
							"currentThreadCount"
						],
						"Backlog": [
							"Catalina:type=ProtocolHandler,port=8080",
							"backlog"
						],
						"Active Sessions":[
							"Catalina:type=Manager,context=/travel,host=localhost",
							"activeSessions"
						],
						port: 9999
					])
		},
		plugin {
            name "http"
            className "org.cloudifysource.usm.liveness.HttpLivenessDetector"
            config([
             		    "url": "http://127.0.0.1:8080"
             		])
        }
	])

	userInterface {

		metricGroups = ([
			metricGroup {

				name "process"

				metrics([
					"Process Cpu Usage",
					"Total Process Virtual Memory",
					"Num Of Active Threads"
				])
			} ,
			metricGroup {

				name "http"

				metrics([
					"Current Http Threads Busy",
					"Current Http Threads count",
					"Backlog"
				])
			} ,

		]
		)

		widgetGroups = ([
			widgetGroup {
				name "Process Cpu Usage"
				widgets ([
					balanceGauge{metric = "Process Cpu Usage"},
					barLineChart{
						metric "Process Cpu Usage"
						axisYUnit Unit.PERCENTAGE
					}
				])
			},
			widgetGroup {
				name "Total Process Virtual Memory"
				widgets([
					balanceGauge{metric = "Total Process Virtual Memory"},
					barLineChart {
						metric "Total Process Virtual Memory"
						axisYUnit Unit.MEMORY
					}
				])
			},
			widgetGroup {
				name "Num Of Active Threads"
				widgets ([
					balanceGauge{metric = "Num Of Active Threads"},
					barLineChart{
						metric "Num Of Active Threads"
						axisYUnit Unit.REGULAR
					}
				])
			}     ,
			widgetGroup {

				name "Current Http Threads Busy"
				widgets([
					balanceGauge{metric = "Current Http Threads Busy"},
					barLineChart {
						metric "Current Http Threads Busy"
						axisYUnit Unit.REGULAR
					}
				])
			} ,
			widgetGroup {

				name "Request Backlog"
				widgets([
					balanceGauge{metric = "Backlog"},
					barLineChart {
						metric "Backlog"
						axisYUnit Unit.REGULAR
					}
				])
			}  ,
			widgetGroup {
				name "Active Sessions"
				widgets([
					balanceGauge{metric = "Active Sessions"},
					barLineChart {
						metric "Active Sessions"
						axisYUnit Unit.REGULAR
					}
				])
			}
		]
		)
	}

	network {
		port = 8080
		protocolDescription ="HTTP"
	}
}
