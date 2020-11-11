/**
 *      Denon Network Receiver
 *        Based on Denon/Marantz receiver by Kristopher Kubicki

ScottE
Original from https://github.com/sbdobrescu/DenonAVR/blob/master/devicetypes/sb/denon-avr.src/denon-avr.groovy

ScottE
Updated for Hubitat (physicalgraph->hubitat)
Removed Simulator section
Removed Media Player stuff
Removed Tiles
*/

metadata {
    definition (name: 'Denon AVR HTTP', namespace: 'ScottE',
        author: 'Scott Ellis') {
        capability 'Actuator'
        capability 'Switch'
        capability 'Polling'
        capability 'Switch Level'

        attribute 'mute', 'string'
        attribute 'input', 'string'
        attribute 'cbl', 'string'
        attribute 'bd', 'string'
        attribute 'mp', 'string'
        attribute 'zone2', 'string'

        command 'mute'
        command 'unmute'
        command 'toggleMute'
        command 'inputSelect', ['string']
        command 'inputNext'
        command 'cbl'
        command 'bd'
        command 'mp'
        command 'z2on'
        command 'z2off'
        }

    preferences {
        input('destIp', 'text', title: 'IP', description: 'The device IP')
        input('destPort', 'number', title: 'Port', description: 'The port you wish to connect', defaultValue: 80)
        input(title: "Denon AVR version: ${getVersionTxt()}" ,description: null, type : 'paragraph')
    }
}

def parse(String description) {
    //log.debug "Parsing '${description}'"
    def map = stringToMap(description)
    if (!map.body || map.body == 'DQo=') { return }
    def body = new String(map.body.decodeBase64())
    def statusrsp = new XmlSlurper().parseText(body)

    //POWER STATUS
    def power = statusrsp.Power.value.text()
    if (power == 'ON') {
        sendEvent(name: 'status', value: 'playing')
    }
    if (power != '' && power != 'ON') {
        sendEvent(name: 'status', value: 'paused')
    }

    //VOLUME STATUS
    def muteLevel = statusrsp.Mute.value.text()
    if (muteLevel == 'on') {
        sendEvent(name: 'mute', value: 'muted')
    }
    if (muteLevel != '' && muteLevel != 'on') {
        sendEvent(name: 'mute', value: 'unmuted')
    }
    if (statusrsp.MasterVolume.value.text()) {
        int volLevel = (int) statusrsp.MasterVolume.value.toFloat() ?: -40.0
        volLevel = (volLevel + 80)
        log.debug "Adjusted volume is ${volLevel}"
        int curLevel = 36
        try {
            curLevel = device.currentValue('level')
            log.debug "Current volume is ${curLevel}"
        } catch (NumberFormatException nfe) {
            curLevel = 36
        }
        if (curLevel != volLevel) {
            sendEvent(name: 'level', value: volLevel)
        }
    }

    //INPUT STATUS
    def inputCanonical = statusrsp.InputFuncSelect.value.text()
    sendEvent(name: 'input', value: inputCanonical)
    log.debug "Current Input is: ${inputCanonical}"

    def inputSurr = statusrsp.selectSurround.value.text()
    sendEvent(name: 'sound', value: inputSurr)
    log.debug "Current Surround is: ${inputSurr}"
    def inputZone = statusrsp.RenameZone.value.text()
    //sendEvent(name: "sound", value: inputSurr)
    log.debug "Current Active Zone is: ${inputZone}"
}

//ACTIONS
def setLevel(val) {
    sendEvent(name: 'mute', value: 'unmuted')
    sendEvent(name: 'level', value: val)
    int scaledVal = val - 80
    request("cmd0=PutMasterVolumeSet%2F$scaledVal")
}
def on() {
    sendEvent(name: 'status', value: 'playing')
    request('cmd0=PutZone_OnOff%2FON')
}
def off() {
    sendEvent(name: 'status', value: 'paused')
    request('cmd0=PutZone_OnOff%2FOFF')
}
def z2on() {
    log.debug 'Turning on Zone 2'
    sendEvent(name: 'zone2', value: 'ON')
    request2('cmd0=PutZone_OnOff%2FON')
}
def z2off() {
    log.debug 'Turning off Zone 2'
    sendEvent(name: 'zone2', value: 'OFF')
    request2('cmd0=PutZone_OnOff%2FOFF')
}
def mute() {
    sendEvent(name: 'mute', value: 'muted')
    request('cmd0=PutVolumeMute%2FON')
}
def unmute() {
    sendEvent(name: 'mute', value: 'unmuted')
    request('cmd0=PutVolumeMute%2FOFF')
}
def toggleMute() {
    if (device.currentValue('mute') == 'muted') { unmute() }
        else { mute() }
}
def cbl() {
    def cmd = 'SAT/CBL'
    syncInputs(cmd)
    log.debug "Setting input to ${cmd}"
    request('cmd0=PutZone_InputFunction%2F' + cmd)
}
def bd() {
    def cmd = 'BD'
    syncInputs(cmd)
    log.debug "Setting input to ${cmd}"
    request('cmd0=PutZone_InputFunction%2F' + cmd)
}
def mp() {
    def cmd = 'MPLAY'
    syncInputs(cmd)
    log.debug "Setting input to '${cmd}'"
    request('cmd0=PutZone_InputFunction%2F' + cmd)
}
def poll() {
    //log.debug "Polling requested"
    refresh()
}
def syncInputs(cmd) {
    if (cmd == 'SAT/CBL') sendEvent(name: 'cbl', value: 'ON')
            else sendEvent(name: 'cbl', value: 'OFF')
    if (cmd == 'BD') sendEvent(name: 'bd', value: 'ON')
            else sendEvent(name: 'bd', value: 'OFF')
    if (cmd == 'MPLAY') sendEvent(name: 'mp', value: 'ON')
            else sendEvent(name: 'mp', value: 'OFF')
}

def refresh() {
    def hosthex = convertIPtoHex(destIp)
    def porthex = convertPortToHex(destPort)
    device.deviceNetworkId = "$hosthex:$porthex"

    def hubAction = new hubitat.device.HubAction(
                'method': 'GET',
                'path': '/goform/formMainZone_MainZoneXml.xml',
                'headers': [ HOST: "$destIp:$destPort" ]
            )
    hubAction
}

def request(body) {
    def hosthex = convertIPtoHex(destIp)
    def porthex = convertPortToHex(destPort)
    device.deviceNetworkId = "$hosthex:$porthex"

    def hubAction = new hubitat.device.HubAction(
                'method': 'POST',
                'path': '/MainZone/index.put.asp',
                'body': body,
                'headers': [ HOST: "$destIp:$destPort" ]
            )

    hubAction
}
def request2(body) {
    def hosthex = convertIPtoHex(destIp)
    def porthex = convertPortToHex(destPort)
    device.deviceNetworkId = "$hosthex:$porthex"

    def hubAction = new hubitat.device.HubAction(
                'method': 'POST',
                'path': '/Zone2/index.put.asp',
                'body': body,
                'headers': [ HOST: "$destIp:$destPort" ]
            )

    hubAction
}

private String convertIPtoHex(ipAddress) {
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02X', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
    String hexport = port.toString().format( '%04X', port.toInteger() )
    return hexport
}

def getVersionTxt() {
    return '2.2'
}
