# WaveLinkPluginTouchPortal
Wave Link Touch Portal Plugin

## Settings
- IP in Plugin settings pane inside touch portal is to set the IP address where wave link is located. Keep localhost if wave link software is on the same computer as touch portal. 
  - Will have to restart plugin to make changes load. (I have had to restart Touch Portal entirely one time, so unsure if that will continue to be a problem). 

## Actions: 
- Mute/Unmute output: 
  - Mute/unmute or toggle; for monitor mix or stream mix. 
  - Target either the Monitor Mix, Stream Mix or both.
  
- Set Input Volume:
  - Sets the volume of an input to a specific number.
  - Target either the Monitor Mix, Stream Mix or both.
  - If in wave link software you have the monitor mix and stream mix locked for an input then setting one will also set the other, I believe.
  
- Mute Input:
  - Mute/unmute or toggle; for specific input.
  - Target either the monitor mix, stream mix or both.

- Select Monitor Mix Output:
  - Switch to a specific output for your monitor mix.
  - Sometimes if you attempt to switch to an output that is already selected in Wave Link it will show that it is blank in Wave Link and not work.
    - I think I may have fixed this, but will continue to monitor just in case. 
  - Was working and now not working?
    - Changed a little, possibly working better?

## Connectors:
- Input Volume: 
  - Connects the slider to the what input you choose. 
  - Target either the monitor mix, stream mix or both.
  - ~~For some reason if you try to use multiple sliders with different inputs then one slider may control both sliders (but it does not change both inputs volume in wave link)~~
    - Apparently, if you add any sliders you want and then restart the Wave Link plugin the sliders start to act normally?

- Output Volume:
  -  Connects the slider to monitor mix, stream mix or both.
  -  ~~For some reason if you try to use multiple sliders with different outputs then one slider may control both sliders (but it does not change both outputs volume in wave link)~~
      -  Apparently, if you add any sliders you want and then restart the Wave Link plugin the sliders start to act normally?

## FAQ
- "Local" in the actions/connectors is equal to Monitor Mix. May change in future.
- "Stream" in the actions/connectors is equal to Stream Mix. 
- I do not think States work, the states were created just to use with the actions, so for example you try to use a state with an if/else it will be blank. 
  - it may be possible to manually type in the if/else statement to correctly match what the state will be set to, but have not tried. may look into this later. 
- If you add/remove anything from Wave Link (New input or new output), then you will either have to restart Touch Portal or the plugin for it to receive the new inputs/outputs.

## Features I would like to add:
- Toggle monitoring between Monitor Mix to Stream Mix.
- Toggle filters on/off for inputs. 
- Controls for wave mic (clipguard, gain, lowcut filter, etc)
- Automatically load new changes in inputs/outputs.
