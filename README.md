# TREZOR Bitcoin Wallet for Android

## Features
- Import both SegWit and legacy accounts, load transaction history and keep track of the balance
- Create a transaction, sign it with TREZOR and broadcast to the network
- Account, address and transaction labeling compatible with SLIP-0015 standard
- TREZOR One and TREZOR Model T support

## Build
Install the debug build:
`./gradlew installBtcDebug`

Run unit tests:
`./gradlew test`

## Screenshots
<img src="docs/screen_accounts.png" width="200"> <img src="docs/screen_transactions.png" width="200"> <img src="docs/screen_addresses.png" width="200"> <img src="docs/screen_send.png" width="200">

## Used Libraries
[trezor-android](https://github.com/MattSkala/trezor-android) – TREZOR communication

[Bouncy Castle](https://www.bouncycastle.org/) – cryptography API

[QRGen](https://github.com/kenglxn/QRGen) – QR code generation

[Socket.IO-client Java](https://github.com/socketio/socket.io-client-java) – WebSocket communication

[Kodein](https://github.com/Kodein-Framework/Kodein-DI/) – dependency injection

[Dropbox SDK](https://github.com/dropbox/dropbox-sdk-java) – labels synchronization

## APIs
[Blockbook](https://github.com/trezor/blockbook) – transactions fetching, broadcasting and fee estimation

[CoinMarketCap](https://coinmarketcap.com/api/) – exchange rate

