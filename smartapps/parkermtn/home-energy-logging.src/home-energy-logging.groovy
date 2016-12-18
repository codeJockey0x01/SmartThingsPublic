definition(
        name: "Home Energy Logging",
        namespace: "ParkerMtn",
        author: "Scott Cronshaw",
        description: "Log Home Energy to Google Sheets",
        category: "Green Living",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving@3x.png")

preferences {
    
    section("Log Power Meter") {
        input "powerMeters", "capability.powerMeter", title: "Power Meters", required: true, multiple: true
    }

    section ("Google Sheets") {
        input "urlKey", "text", title: "Script URL key", required: true
    }
    
    section ("Technical settings") {
        input "queueTime", "enum", title:"Time to queue events before pushing to Google (in seconds)", options: ["5", "8", "10", "15", "30", "60"], defaultValue:"10"
    }
    
    section ("Push to Google") {
    	input "pushToGoogle", "bool", title: "Push to Google?", required: true
    }
}

def installed() {
    setOriginalState()
    initialize()
}

def updated() {
    log.debug "Updated"
    unsubscribe()
    setOriginalState()
    initialize()
}

def initialize() {
    log.debug "Initializing"
    subscribe(powerMeters, "power", handlePowerEvent)
    scheduleQueue()
//    subscribe(powerMeters, "power_1", handlePowerEvent)
//    subscribe(powerMeters, "power_2", handlePowerEvent)
}

def setOriginalState() {
    log.debug "Set original state"
    unschedule(processQueue)
    atomicState.queue = [:]
//    atomicState.failureCount=0
//    atomicState.scheduled=false
//    atomicState.lastSchedule=0
//    atomicState.processingQueue = false
}


private resetState() {
    atomicState.queue = [:]
//    atomicState.failureCount=0
//    atomicState.scheduled=false
}


def handlePowerEvent(evt) {

	if( pushToGoogle ) {
		queueValue(evt) { it.toString() }
    }
}

private queueValue(evt, Closure convert) {

//    checkAndProcessQueue()
    
    if ( evt?.value ) { //&& atomicState.processingQueue != true) {
  
//  	log.debug "Data: ${evt?.data}"

		def slurper = new groovy.json.JsonSlurper()
 		def dataObj = slurper.parseText(evt.data)

          def keyId = dataObj.clamp //URLEncoder.encode(" ${evt.displayName.trim()} ${evt.name} ${dataObj.clamp}")

//        def keyId = URLEncoder.encode(evt.displayName.trim()+ " " +evt.name)
        def value = URLEncoder.encode(convert(evt.value))
    
        log.debug "Logging to queue ${keyId} = ${value}"
        
        if ( atomicState.queue == [:] ) {
            // format time in the same wasy as sheets does
            def eventTime = URLEncoder.encode(evt.date.format( 'M/d/yyyy HH:mm:ss', location.timeZone ))
            addToQueue("Time", eventTime)
        }
        
        addToQueue(keyId, value)
        
//        log.debug(atomicState.queue)

//        scheduleQueue()
    }
}

/*
 * atomicState acts differently from state, so we have to get the map, put the new item and copy the map back to the atomicState
 */
private addToQueue(key, value) {
//    if( atomicState.processingQueue != true ) {
        def queue = atomicState.queue
        queue.put(key, value)
        atomicState.queue = queue
//    }
}

/*
private checkAndProcessQueue() {
    if (atomicState.scheduled && ((now() - atomicState.lastSchedule) > (settings.queueTime.toInteger()*2000))) {
        // if event has been queued for twice the amount of time it should be, then we are probably stuck
        sendEvent(name: "scheduleFailure", value: now())
        unschedule(processQueue)
        processQueue()
    }
}
*/

def scheduleQueue() {
/*
    if (atomicState.failureCount >= 3) {
        log.debug "Too many failures, clearing queue"
        sendEvent(name: "queueFailure", value: now())
        resetState()
    }
*/    
//    if (!atomicState.scheduled) {
//        atomicState.scheduled=true
//        atomicState.lastSchedule=now()
        runIn(settings.queueTime.toInteger(), processQueue)
//    } 
}



def processQueue() {

//	if( atomicState.processingQueue == true )
//		return

//	atomicState.processingQueue = true;

    if (atomicState.queue != [:]) {

        def success = false

        log.debug "Processing Queue"

        def url = "https://script.google.com/macros/s/${urlKey}/exec?"
        for ( e in atomicState.queue ) 
        { 
        	def fmtKey = e.key
        	if( fmtKey.length() == 1 )
            	fmtKey = URLEncoder.encode("Clamp ${fmtKey}")
            
        	url+="${fmtKey}=${e.value}&" 
        }
        
        url = url[0..-2]
        log.debug(url)
        try {
            def putParams = [
                uri: url
            ]

            httpGet(putParams) { response ->
                log.debug("Google Response: ${response.status}")
                if (response.status == 200 ) {
                    success = true
                }
            }

        } catch(e) {
            def errorInfo = "Error sending value: ${e}"
            log.error errorInfo
        }

/*
        if( success ) {
        	resetState()
        }
        else {
            atomicState.failureCount = atomicState.failureCount+1
            atomicState.scheduled = false
            scheduleQueue()
        }
*/
		resetState()
    }

	scheduleQueue()

//    atomicState.processingQueue = false
}