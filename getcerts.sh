#!/bin/bash
keytool -printcert -sslserver btc-e.com -rfc > conf/btce.crt
keytool -printcert -sslserver btc-trade.com.ua -rfc > conf/btctradecomua.crt
