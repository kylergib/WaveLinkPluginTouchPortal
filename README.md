# WaveLinkPluginTouchPortal
Wave Link Touch Portal Plugin

## Java 8 may need to be installed for it to work properly.

## Actions:
### Input Actions:
- Set Input Volume:
  - Sets the volume of an input to a specific number.
  - Target either the Monitor Mix, Stream Mix or both.
  - If in wave link software you have the monitor mix and stream mix locked for an input then setting one will also set the other, I believe.
  
- Mute Input:
  - Mute/unmute or toggle; for specific input.
  - Target either the monitor mix, stream mix or both.

- Input Filter Bypass
  - Enable/Disable or toggle filter bypass for a specific input.
  - true means that the input will bypass the filter

### Output Actions:
- Mute/Unmute output:
  - Mute/unmute or toggle; for monitor mix or stream mix.
  - Target either the Monitor Mix, Stream Mix or both.

- Select Monitor Mix Output:
  - Switch to a specific output for your monitor mix.
  - Sometimes if you attempt to switch to an output that is already selected in Wave Link it will show that it is blank in Wave Link and not work.
    - I think I may have fixed this, but will continue to monitor just in case.
  - Was working and now not working?
    - Changed a little, possibly working better?

- Set Output Volume:
  - Sets the volume of an output to a specific number.
  - Target either the Monitor Mix, Stream Mix or both.
- Switch Monitoring Mix
  - Switch which mix you are monitoring (Local or Stream)
  - The active output being monitored is highlighted in green ot the bottom right of Wave Link


### Mic Actions:
- Set Mic Gain
  - Sets, increases or decreases mic gain
- Set Mic Output Volume
  - Sets, increases or decreases mic output volume
  - This is headphone volume of the headphone jack on the wave mic itself.
- Set PC/Mic Mix
  - Sets, increases or decreases PC/Mic mix
  - This is the mix that is used for the headphone jack. 
    - Not sure how this will affect the wave 1. If anyone has wave 1 probably should not use this as I do not believe it has PC/Mic mix.
- Toggle Mic Clip Guard
  - Turns mic clip guard on and off
  - Only option is toggle
- Toggle Mic Enhanced Low Cut Filter
  - Turns low cut filter on and off
  - Only option is toggle

### Other:
- Update inputs/outputs/mics lists
  - This button will update all lists so that you can select something in the button settings. 
  - Instead of restarting plugin or Touch Portal to refresh the list you can use this.


## Connectors:
#### Known issue with Touch Portal. Refreshing the page works most of the time, but have had to restart Touch Portal completely to fix.
- Input Volume: 
  - Connects the slider to the what input you choose. 
  - Target either the monitor mix, stream mix or both.
- Output Volume:
  -  Connects the slider to monitor mix, stream mix or both.
- Mic Gain Volume
- Mic Output Volume
- Pc/Mic Mix

## States:
- input list, output list and mic list
  - just a list of all inputs, outputs and mics in wave link, but is a dummy state as you can not use it in actions or if/else statements
- Selected Output
  - What the current selected output is
  - Example: External headphones or airpods
  - Can be used in if else statements, but you need the exact name it shows in Wave Link and is case-sensitive.
  - IF YOU USE IN AN IF STATEMENT YOU HAVE AN APOSTROPHE TYPING IT WILL NOT WORK. THE EASIEST WAY IS TO GO TO THE LOGS AND COPY AN APOSTROPHE AND PASTE WHERE IT GOES IN THE BUTTON STATEMENT.
    - Might be able to fix in future by just removing all apostrophes when inserting it. Will look into it
- Local Mix Out
  - If the local (monitor) mix is muted or not
  - Can be "muted" or "unmuted"
  - If statements work, but when using them check if it is equals to "muted" or "unmuted" (without the quotations)
- Stream Mix Out
  - If the local (monitor) mix is muted or not
  - Can be "muted" or "unmuted"
  - If statements work, but when using them check if it is equals to "muted" or "unmuted" (without the quotations and all lowercase)
- Monitored Mix
  - Local Mix or Stream mix being monitored currently
    - The active output being monitored is highlighted in green ot the bottom right of Wave Link
  - Only toggles between local and stream
  - If statements work with "local" or "stream" (all lowercase)
- Every input also has its own local and stream state.
  - The values are either "muted" or "unmuted" (all lowercase)
  - Can be used in if statements. 

## Settings:
- IP:
  - IP address of PC where Wave Link is running. Defaulted to localhost (the same PC that Touch Portal is running).
    - Works on a different PC as long as firewall is not blocking. May have to allow Wave Link and Touch Portal to communicate over private and public networks on windows. 
  - Will have to restart plugin to make changes load. (I have had to restart Touch Portal entirely one time, so unsure if that will continue to be a problem).

## FAQ:
- "Local" in the actions/connectors is equal to Monitor Mix. 
  - May change in future, but Wave Link show local as "Monitor Mix", but also call the switch to toggle, between listening to "Monitor Mix" and "Stream Mix", Monitor Switch. So, it is a little confusing, so I settled for calling it local instead.
- "Stream" in the actions/connectors is equal to Stream Mix. 
- ~~If you add/remove anything from Wave Link (New input or new output), then you will either have to restart Touch Portal or the plugin for it to receive the new inputs/outputs.~~
  - added an action to update inputs/outputs/mics
  - added an automatic refresh of mics/inputs when a change is detected. Outputs still need to be refreshed by the button or by restarting plugin.
- Plugin has not been tested with Wave XLR because I do not have one.
- Wave 1 is also untested, but I think it is the same as Wave 3 except Wave 1 does not have PC/Mic mix and a hard mute.
- If you installed WaveLinkTP 101 and you update to 102 or 103 you will need to redo your actions as I seperated the inputs/outputs/mics into their own categories for ease of finding the right action. 
- Toggle Mute for mic cannot be done with this plugin. It cannot even be controlled on Stream Deck through the official Wave Link plugin. On stream deck you need a third party plugin called "Audio Devices" to toggle.
  - Will research to attempt to find a way though. 
