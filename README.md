# Simple Communication

—Simple messaging written in Java.

*This project is on hold.*

## Downloads

##### Client

* [[**Latest Release**](https://github.com/ed7n/simple-comms/raw/master/release/client/simplecomms.zip)] — Update 0 Revision 2, 09/09/2017.

##### Server

* [[**Latest Release**](https://github.com/ed7n/simple-comms/raw/master/release/server/simplecomms-server.zip)] — Update 0 Revision 2, 09/09/2017.
  * Configure server parameters by editing `SC_CFG`.

## Client Usage

`java -jar simplecomms.jar [host] <port>`

## Building

##### Client

    $ javac -d release --release 8 --source-path src src/eden/simplecomms/client/SimpleCommunicationClient.java && jar -c -f release/client/simplecomms.jar -e eden.simplecomms.client.SimpleCommunicationClient -C release eden && cp -r res/client/AUDIO release/client

##### Server

    $ javac -d release --release 8 --source-path src src/eden/simplecomms/server/SimpleCommunicationServer.java && jar -c -f release/server/simplecomms-server.jar -e eden.simplecomms.server.SimpleCommunicationServer -C release eden && cp res/server/SC_CFG release/server
