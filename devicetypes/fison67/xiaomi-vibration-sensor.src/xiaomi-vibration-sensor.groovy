/**
 *  Xiaomi Vibration Sensor (v.0.0.1)
 *
 * MIT License
 *
 * Copyright (c) 2018 fison67@nate.com
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
*/
 
import groovy.json.JsonSlurper

metadata {
	definition (name: "Xiaomi Vibration Sensor", namespace: "fison67", author: "fison67") {
        capability "Sensor"						
        capability "Button"
        capability "Configuration"
        capability "Refresh"
         
        attribute "battery", "string"
        attribute "final_tilt_angle", "string"
        attribute "coordination", "string"
        attribute "bed_activity", "string"
        
        attribute "lastCheckin", "Date"
        
	}


	simulator {
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"button", type: "generic", width: 6, height: 4){
			tileAttribute ("device.button", key: "PRIMARY_CONTROL") {
                attributeState "click", label:'\nButton', icon:"https://github.com/fison67/mi_connector/blob/master/icons/xiaomi-vibration-sensor.png?raw=true", backgroundColor:"#8CB8C9"
			}
            tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
    			attributeState("default", label:'Last Update: ${currentValue}',icon: "st.Health & Wellness.health9")
            }
		}
        
        valueTile("battery", "device.battery", width: 2, height: 2) {
            state "val", label:'${currentValue}%', defaultState: true
        }
        
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh", icon:"st.secondary.refresh"
        }
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

def setInfo(String app_url, String id) {
	log.debug "${app_url}, ${id}"
	state.app_url = app_url
    state.id = id
}

def setStatus(params){
	log.debug "Mi Connector >> ${params.key} : ${params.data} : ${params.subData}"
    
 	switch(params.key){
    case "action":
    	if(params.data == "vibrate") {
        	buttonEvent(1, "pushed")
        } else if(params.data == "tilt") {
       	 	buttonEvent(2, "pushed")
        } else if(params.data == "final_tilt_angle"){
       	 	buttonEvent(3, "pushed")
            sendEvent(name: "final_tilt_angle", value: params.subData as int)
        } else if(params.data == "coordination"){
       	 	buttonEvent(4, "pushed")
            sendEvent(name: "coordination", value: params.subData)
        } else if(params.data == "bed_activity"){
       	 	buttonEvent(5, "pushed")
            sendEvent(name: "bed_activity", value: params.subData as int)
        }
    	break;
    case "batteryLevel":
    	sendEvent(name:"battery", value: params.data)
    	break;
    }
    
    updateLastTime()
}

def buttonEvent(Integer button, String action) {
    sendEvent(name: "button", value: action, data: [buttonNumber: button], descriptionText: "$device.displayName button $button was $action", isStateChange: true)
}

def updateLastTime(){
	def now = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
    sendEvent(name: "lastCheckin", value: now)
}

def refresh(){
	log.debug "Refresh"
    def options = [
     	"method": "GET",
        "path": "/devices/get/${state.id}",
        "headers": [
        	"HOST": parent._getServerURL(),
            "Content-Type": "application/json"
        ]
    ]
    sendCommand(options, callback)
}

def callback(physicalgraph.device.HubResponse hubResponse){
	def msg
    try {
        msg = parseLanMessage(hubResponse.description)
		def jsonObj = new JsonSlurper().parseText(msg.body)
        
     //  	sendEvent(name:"battery", value: jsonObj.properties.batteryLevel)
        
        updateLastTime()
    } catch (e) {
        log.error "Exception caught while parsing data: "+e;
    }
}

def updated() {
}

def sendCommand(options, _callback){
	def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: _callback])
    sendHubCommand(myhubAction)
}

def makeCommand(body){
	def options = [
     	"method": "POST",
        "path": "/control",
        "headers": [
        	"HOST": parent._getServerURL(),
            "Content-Type": "application/json"
        ],
        "body":body
    ]
    return options
}
