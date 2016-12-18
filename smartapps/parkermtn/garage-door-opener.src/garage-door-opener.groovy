definition(
    name: "Garage Door Opener",
    description: "Garage Door Opener",
    namespace: "ParkerMtn",
    author: "Scott Cronshaw",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png"
)

preferences {
	section("Choose the relay that opens and closes the garage door"){
		input "opener", "capability.switch", title: "Physical Garage Opener?", required: true
	}
	section("Choose the sensor that detects if the garage is open or closed"){
		input "sensor", "capability.contactSensor", title: "Physical Garage Door Sensor?", required: true
	}
    
	section("Choose the ParkerMtn Virtual Garage Door Device"){
		input "virtualgd", "capability.doorControl", title: "ParkerMtn Virtual Garage Door?", required: true
	}
    
    section( "Notifications" ) {
        input("recipients", "contact", title: "Send notifications to") {
            input "sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false
            input "phone1", "phone", title: "Send a Text Message?", required: false
        }
    }

}

def installed()
{
	initialize()
}

def updated()
{
	unsubscribe()
    initialize()
}

def initialize()
{
	def realgdstate = sensor.currentContact
	def virtualgdstate = virtualgd.currentContact

	subscribe(sensor, "contact", contactHandler)
	subscribe(virtualgd, "door", virtualgdButtonHandler)
    subscribe(opener, "switch", openerHandler)
    
	if (realgdstate != virtualgdstate)
	{
		if (realgdstate == "open")
			virtualgd.SetState("open")
		else 
			virtualgd.SetState("closed")
	}
}

def openerHandler(evt)
{
    log.debug "Opener Event: ${evt.value}"
}

def virtualgdButtonHandler(evt) 
{
	def realgdstate = sensor.currentContact

    log.debug "Virtual Garage Door Event: ${evt.value}"

	switch( evt.value)
    {
	   	case "opening":
            if (realgdstate != "open")
            {
                Notify("${virtualgd.displayName} Opening")
                ToggleOpener()
            }
        	break
            
           
        case "closing":
            if (realgdstate != "closed")
            {
                Notify("${virtualgd.displayName} Closing")
                ToggleOpener()
            }
        	break
            
		default:
        	break
    }
}

def ToggleOpener()
{
	log.debug "Turning Opener On"
    opener.on()
    runIn(1, TurnOpenerOff)
}

def TurnOpenerOff()
{
	log.debug "Turning Opener Off"
	opener.off()
    runIn(3, ValidateOpenerOff)
}

def ValidateOpenerOff()
{
    def openerState = opener.currentSwitch
	log.debug "Checking for Opener Off: ${openerState}"
    if( openerState == "on" ) {
    	TurnOpenerOff()
    }
}

def contactHandler(evt) 
{
	def virtualgdstate = virtualgd.currentContact
    
    log.debug "Sensor Event: ${evt.value}"
    
    switch( evt.value )
    {
    	case "open":
			if (virtualgdstate != "open")
			{
            	runIn(delay, finishOpening)
			}
        	break
            
        case "closed":
            if (virtualgdstate != "closed")
            {
                virtualgd.SetState("closed")
            }
        	break
            
		default:
        	break
    }
}

def finishOpening()
{
	virtualgd.SetState("open")
}

private Notify(msg) 
{
    if (location.contactBookEnabled) {
        log.debug("Sending notifications to: ${recipients?.size()}")
        sendNotificationToContacts(msg, recipients)
    }
    else {
        if (sendPushMessage != "No") {
            log.debug("sending push message")
            sendPush(msg)
        }

        if (phone1) {
            log.debug("sending text message")
            sendSms(phone1, msg)
        }
    }

    log.debug msg
}


