# manikuit

Cryptocurrency based payment gateway

This project aims to provide a uniform interface for generating and tracking cryptocurrency based payments using
the following backends:

- Bitcoin (via bitcoind). Versions 0.16 through 0.18.
- Ethereum (via parity). Version v2.4.5-stable-76d4064-20190408.
- Monero (in progress).

For each backend, you must install and configure each backend package, and enable its RPC interface in order for this
payment gateway to communicate with it correctly.

# Disclaimer

> This project is not production ready, and still requires security and code correctness audits.
> You use this software at your own risk. Vaccove Crana, LLC., its affiliates and subsidiaries waive
> any and all liability for any damages caused to you by your usage of this software.
