#Requires AutoHotkey v2.0
#SingleInstance Force

; Windows ignores a Win+L that Moonlight, Sunshine or Apollo inject, so the Lock key of Super Artemis does nothing on its own.
; AutoHotkey sees injected keys, so this script catches Win+L and locks the session with the API call that Windows itself uses.
#l::DllCall("user32\LockWorkStation")
