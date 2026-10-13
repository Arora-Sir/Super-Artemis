# Privacy policy for Super Artemis

**Effective date:** 2026-10-08

Super Artemis is an open source game streaming client. It has no accounts, no ads, no analytics and no tracking, and the developer runs no server that the app talks to.

## What stays on your device

- Settings, menu layouts, custom keys, gesture bindings and profiles are stored in the app's private storage.
- The list of PCs you added, with their addresses and server certificates, is stored in the app's private storage.
- The pairing identity (a client certificate and private key) is created on your device and stays there.
- Backups are written only where you choose in the system file picker. Pairing keys are included only when you tick that option.

## Who the app talks to

- **Your PCs.** Pairing, the app list, streaming video and audio, input and the optional microphone stream go directly between your device and the PCs you add.
- **Moonlight STUN and connection test servers.** Inherited from Moonlight, the app may contact `stun.moonlight-stream.org` to find your public address for streaming over the Internet, and `android.conntest.moonlight-stream.org` when you run the network test. These requests carry no personal data.
- **Links you open.** Links in Settings and About (GitHub, PayPal, UPI apps, profile pages) open in your browser or the matching app, which has its own privacy policy.

## Microphone

The microphone is used only while a stream is running, only when "Enable microphone streaming" is on and the host supports it, and only while you have it unmuted. Audio goes only to the PC you are streaming from. Android shows its microphone indicator whenever capture is active.

## Performance logs

Local performance logging, when you turn it on, stays on your device. The upstream option that emails logs to the Artemis performance dataset is hidden in Super Artemis.

## Changes

Changes to this policy are published in this file in the [Super Artemis repository](https://github.com/Arora-Sir/Super-Artemis). For questions, open an issue there.
