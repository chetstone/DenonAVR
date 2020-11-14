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

/*
 chetstone
 Fixed some commands with info from
 https://github.com/subutux/denonavr/blob/master/CommandEndpoints.txt
*/

metadata {
    definition (name: 'Denon AVR HTTP', namespace: 'chetstone',
        author: 'Chester Wood') {
        capability 'Actuator'
        capability 'Polling'
        capability 'MusicPlayer'
        capability 'Refresh'
        capability 'Switch'

        attribute 'switch', 'enum'
        attribute 'level', 'number'
        attribute 'trackDescription', 'string'
        attribute 'mute', 'string'
        attribute 'input', 'string'
        attribute 'sound', 'string'
        attribute 'cbl', 'string'
        attribute 'cd', 'string'
/* From orig */
        attribute 'tv', 'string'
        attribute 'dvd', 'string'
        attribute 'net', 'string'
        attribute 'dock', 'string'
        attribute 'tuner', 'string'
        attribute 'sMovie', 'string'
        attribute 'sMusic', 'string'
        attribute 'sPure', 'string'
        attribute 'q1', 'string'
        attribute 'q2', 'string'
        attribute 'q3', 'string'
        attribute 'q4', 'string'
/* From Scotte */
        attribute 'bd', 'string'
        attribute 'mp', 'string'
/* end */
        attribute 'zone2', 'string'

        command 'mute'
        command 'unmute'
        command 'toggleMute'
        /* command 'inputSelect', ['string'] */
        command 'cbl'
        command 'tv'
        command 'cd'
        command 'bd'
/* From orig */
        command 'dvd'
        command 'net'
        command 'dock'
        command 'tuner'
        command 'sMovie'
        command 'sMusic'
        command 'sPure'
        command 'q1'
        command 'q2'
        command 'q3'
        command 'q4'
        command 'sPure'
/* From scotte */
        command 'mp'
/* ======= end */
        command 'on'
        command 'off'
        }

    preferences {
        input('destIp', 'text', title: 'IP', description: 'The device IP')
        input('destPort', 'number', title: 'Port', description: 'The port you wish to connect', defaultValue: 80)
            // input('zone', 'enum', title: 'Zone', description: 'The zone this driver controls', defaultValue: 'Main', options: ['Main', 'Zone2'])
        input(title: "Denon AVR version: ${getVersionTxt()}" ,description: null, type : 'paragraph')
    }
}

def parse(String description) {
    def map = stringToMap(description)
    if (!map.body || map.body == 'DQo=' || map.body == '}')
    {  log.debug "request body is empty in Parse, refreshing..."
        return refresh()
    }
    log.debug "Base64 says ${map.body}"
    def body = new String(map.body.decodeBase64())
        //log.debug "Body is ${body}"
    def statusrsp = new XmlSlurper().parseText(body)
    log.debug "StatusRSP is ${statusrsp}"
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
        try {
        int volLevel = (int) statusrsp.MasterVolume.value.toFloat() ?: -40.0
        volLevel = (volLevel + 80)
        log.debug "Adjusted volume is ${volLevel}"
        int curLevel = 36
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
    sendEvent(name: 'trackDescription', value: inputSurr)
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
def play(zone = 'Main') {
    log.debug "Turning on ${zone}"
    sendEvent(name: 'status', value: 'playing')
    if ( zone == 'Main') {
        request('cmd0=PutSystem_OnStandby%2FON')
     } else {
        sendEvent(name: 'zone2', value: 'ON')
        sendEvent(name: 'switch', value: 'on')
        request('cmd0=PutZone_OnOff%2FON&cmd1=aspMainZone_WebUpdateStatus%2F&ZoneName=ZONE2')
    }
}
def pause(zone = 'Main') {
    log.debug "Turning off ${zone}"
    if ( zone == 'Main') {
        sendEvent(name: 'status', value: 'paused')
        request('cmd0=PutSystem_OnStandby%2FSTANDBY')
    } else {
        sendEvent(name: 'zone2', value: 'OFF')
        sendEvent(name: 'switch', value: 'off')
        request('cmd0=PutZone_OnOff%2FOFF&cmd1=aspMainZone_WebUpdateStatus%2F&ZoneName=ZONE2')
    }

}
def on() { //z2
    play('Main')
}
def off() { //z2
    pause('Main')
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
def tv() {
    def cmd = 'TV'
    syncInputs(cmd)
    log.debug "Setting input to ${cmd}"
    request('cmd0=PutZone_InputFunction%2F' + cmd)
}
def cd() {
    def cmd = 'CD'
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
def dvd() {
    def cmd = 'DVD'
    syncInputs(cmd)
    log.debug "Setting input to ${cmd}"
    request('cmd0=PutZone_InputFunction%2F' + cmd)
}
def net() {
    def cmd = 'NET/USB'
    syncInputs(cmd)
    log.debug "Setting input to '${cmd}'"
    request('cmd0=PutZone_InputFunction%2F' + cmd)
}
def dock() {
    def cmd = 'DOCK'
    syncInputs(cmd)
    log.debug "Setting input to '${cmd}'"
    request('cmd0=PutZone_InputFunction%2F' + cmd)
}
def tuner() {
    def cmd = 'HDRADIO'
    syncInputs(cmd)
    log.debug "Setting input to '${cmd}'"
    request('cmd0=PutZone_InputFunction%2F' + cmd)
}
def mp() {
    def cmd = 'MPLAY'
    syncInputs(cmd)
    log.debug "Setting input to '${cmd}'"
    request('cmd0=PutZone_InputFunction%2F' + cmd)
}

//SOUND MODES
def sMusic() {
    def cmd = 'MUSIC'
    log.debug "Setting input to '${cmd}'"
    request('cmd0=PutSurroundMode%2F' + cmd)
}
def sMovie() {
    def cmd = 'MOVIE'
    log.debug "Setting input to '${cmd}'"
    request('cmd0=PutSurroundMode%2F' + cmd)
}
def sGame() {
    def cmd = 'GAME'
    log.debug "Setting input to '${cmd}'"
    request('cmd0=PutSurroundMode%2F' + cmd)
}
def sPure() {
    def cmd = 'PURE DIRECT'
    log.debug "Setting input to '${cmd}'"
    request('cmd0=PutSurroundMode%2FPURE+DIRECT&cmd1=aspMainZone_WebUpdateStatus%2F')
}
//QUICK MODES
def q1() {
    def cmd = 'Quick1'
    log.debug "Setting quick input to '${cmd}'"
    syncInputs(cmd)
    request2('cmd0=PutUserMode%2F' + cmd + '&cmd1=aspMainZone_WebUpdateStatus%2F&ZoneName=MAIN+ZONE')
}
def q2() {
    def cmd = 'Quick2'
    log.debug "Setting quick input to '${cmd}'"
    syncInputs(cmd)
    request2('cmd0=PutUserMode%2F' + cmd + '&cmd1=aspMainZone_WebUpdateStatus%2F&ZoneName=MAIN+ZONE')
}
def q3() {
    def cmd = 'Quick3'
    log.debug "Setting quick input to '${cmd}'"
    syncInputs(cmd)
    request2('cmd0=PutUserMode%2F' + cmd + '&cmd1=aspMainZone_WebUpdateStatus%2F&ZoneName=MAIN+ZONE')
}
def q4() {
    def cmd = 'Quick4'
    log.debug "Setting quick input to '${cmd}'"
    syncInputs(cmd)
    request2('cmd0=PutUserMode%2F' + cmd + '&cmd1=aspMainZone_WebUpdateStatus%2F&ZoneName=MAIN+ZONE')
}
def poll() {
    //log.debug "Polling requested"
    refresh()
}
def syncInputs(cmd) {
    if (cmd == 'SAT/CBL') sendEvent(name: 'cbl', value: 'ON')
    else sendEvent(name: 'cbl', value: 'OFF')
    if (cmd == 'TV') sendEvent(name: 'tv', value: 'ON')
    else sendEvent(name: 'tv', value: 'OFF')
    if (cmd == 'CD') sendEvent(name: 'cd', value: 'ON')
    else sendEvent(name: 'cd', value: 'OFF')
    if (cmd == 'BD') sendEvent(name: 'bd', value: 'ON')
    else sendEvent(name: 'bd', value: 'OFF')
    if (cmd == 'DVD') sendEvent(name: 'dvd', value: 'ON')
    else sendEvent(name: 'dvd', value: 'OFF')
    if (cmd == 'NET/USB') sendEvent(name: 'net', value: 'ON')
    else sendEvent(name: 'net', value: 'OFF')
    if (cmd == 'DOCK') sendEvent(name: 'dock', value: 'ON')
    else sendEvent(name: 'dock', value: 'OFF')
    if (cmd == 'HDRADIO') sendEvent(name: 'tuner', value: 'ON')
    else sendEvent(name: 'tuner', value: 'OFF')
}
def syncQTiles(cmd) {
    if (cmd == '1') sendEvent(name: 'q1', value: 'ON')
    else sendEvent(name: 'q1', value: 'OFF')
    if (cmd == '2') sendEvent(name: 'q2', value: 'ON')
    else sendEvent(name: 'q2', value: 'OFF')
    if (cmd == '3') sendEvent(name: 'q3', value: 'ON')
    else sendEvent(name: 'q3', value: 'OFF')
    if (cmd == '4') sendEvent(name: 'q4', value: 'ON')
    else sendEvent(name: 'q4', value: 'OFF')
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
    log.debug "Refresh gets ${hubAction}"
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
    log.debug "Request gets ${hubAction}"
    hubAction
}
def request2(body) {
    def hosthex = convertIPtoHex(destIp)
    def porthex = convertPortToHex(destPort)
    device.deviceNetworkId = "$hosthex:$porthex"

    def hubAction = new hubitat.device.HubAction(
                'method': 'POST',
                'path': '/QuickSelect/index.put.asp',
                'body': body,
                'headers': [ HOST: "$destIp:$destPort" ]
            )

    log.debug "Request2 gets ${hubAction}"
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
    return '2.3'
}
