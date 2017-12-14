# File Recovery Helper

This is a neat little utility that indexes, copies and verifies your files when you have a slow and unreliable source, but a reliable and fast data source.

This program is intended to be used when you have a hard drive that has an issue with it's controler and only thansfers data very slow and/or unreliable.

It is by no means perfect, but I use it regularly when I need to grab a specific folder from a slow/unreliable hard drive.

## Features

 - It can recover from almost any crash 
 - It runs without human interaction until it's job is done or it retried 10 times * 
 - It verifies the copied files using CRC32

`*` -> Sometimes needs to be restarted, when it thinks the source has given up 
## How it works

1. It indexes the source recursively
2. It stores the indexed files in a json file
3. It copies file by file
4. It verifies that the source and target CRC32 checksums match
5. It shuts down

It will retry every failing action 10 times.
If an action still failed, it will mark the file as deffective and continue.

If the is restarted after it shuts down by itself, it will skip the successful files and retry the failed files.

# TODO
 - Replace Java's standard synchronous copying method with an asynchronous one.
 - Implement pausing
 - Implement partial copying 