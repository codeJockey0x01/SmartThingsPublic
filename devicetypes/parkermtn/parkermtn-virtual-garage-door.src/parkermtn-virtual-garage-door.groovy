metadata {
	definition (name: "ParkerMtn Virtual Garage Door", namespace: "ParkerMtn", author: "Scott Cronshaw") {
		capability "Actuator"
		capability "Door Control"
        capability "Garage Door Control"
		capability "Contact Sensor"
		capability "Refresh"
		capability "Sensor"
        command "SetState", ["string"]
	}

	simulator {
		
	}

	tiles {
		standardTile("toggle", "device.door", width: 2, height: 2) {
			state("closed", label:'${name}', action:"door control.open", icon:"st.doors.garage.garage-closed", backgroundColor:"#79b821", nextState:"opening")
			state("open", label:'${name}', action:"door control.close", icon:"st.doors.garage.garage-open", backgroundColor:"#ffa81e", nextState:"closing")
			state("opening", label:'${name}', icon:"st.doors.garage.garage-closed", backgroundColor:"#ffe71e")
			state("closing", label:'${name}', icon:"st.doors.garage.garage-open", backgroundColor:"#ffe71e")
			
		}
		standardTile("open", "device.door", inactiveLabel: false, decoration: "flat") {
			state "default", label:'open', action:"door control.open", icon:"st.doors.garage.garage-opening"
		}
		standardTile("close", "device.door", inactiveLabel: false, decoration: "flat") {
			state "default", label:'close', action:"door control.close", icon:"st.doors.garage.garage-closing"
		}

		main "toggle"
		details(["toggle", "open", "close"])
	}
    
    preferences {
    	input name: "delay", type: "number", title: "State change delay", description: "Enter Seconds", required: true
	}
}

def parse(String description) {
	log.trace "parse(${description})"
}

def open() {
    if( state.status == "closed" || state.status == "closing") {
		log.debug "Requesting Open: approved"
        SetState( "opening" )
    }
    else
		log.debug "Requesting Open: denied"
}

def close() {
	if( state.status == "open" || state.status == "opening") {
		log.debug "Requesting Close: approved"
    	SetState( "closing" )
     }
     else
   		log.debug "Requesting Close: denied"

}

def SetState( status )
{
	if( state.status != status )
    {
	    log.debug "Changing VGD.State from ${state.status} to ${status}"
    
    	state.status = status
    	sendEvent(name: "door", value: status)
    }
}

