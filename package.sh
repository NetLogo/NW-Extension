#!/bin/sh
cd ~/git/NetLogo-5.0.1/extensions/nw
make
cd ../..
bin/sbt "te nw"
zip -9r ~/git/NetLogo-5.0.1/extensions/nw-ext-$1.zip ~/programs/NetLogo-5.0.1/extensions/nw

