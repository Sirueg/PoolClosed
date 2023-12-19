# Pool Manager

Simple library to handle "poolable" resources

Made for Java 21, for the Thread.ofVirtual()

## Usage

Implement a poolable resource where the method ```execute(REQUEST request)``` should run a new thread, 
returning the response wrapped in a vavr ```Try<RESPONSE>```.

The recommended implementation on the operation, as it's probably an I/O bound operation, should be a virtual thread.
