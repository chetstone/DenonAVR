# DenonAVR
Hubitat device handler for non-HEOS Denon receivers.

The port to hubitat was done by [Scott Ellis](https://github.com/ScottESanDiego/Hubitat_DenonAVR)
from the excellent handler by sdbobrescu, which can be found at https://github.com/sbdobrescu/DenonAVR_OLD.
That handler in turn is based on on a previous release by Kristopher Kubicki, which is at https://github.com/KristopherKubicki/device-denon-avr

I restored some of the commands/attributes applicable to the 3312CI,
fixed some commands with info from  [here](https://github.com/subutux/denonavr/blob/master/CommandEndpoints.txt),
restored the MusicPlayer capability for the hubtitat dashboard and added a momentary switch that can turn zone2 on.
(In my home zone2 should always be on-- this is there for when it arbitrarily gets turned off).

The PDF in this directory, the (tho irrelevant for this handler) telnet commands for the 3312CI,
is [AVR3312CI_AVR3312_PROTOCOL_V7.6.0](http://openrb.com/wp-content/uploads/2012/02/AVR3312CI_AVR3312_PROTOCOL_V7.6.0.pdf)
