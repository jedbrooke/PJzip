# PJzip
Parallel Java Gzip compressor

## Usage:
build with maven:
```
mvn compile package
```

run:
PJzip takes in data from stdin and writes the compressed stream to stdout

select number of threads with `-p` arg (default all threads)
```
cat file | java -jar target/pjzip-0.1.0.jar > file.gz
```

check file:
```
gzip -tv file.gz

#should output:
#file.gz:        OK
```

`file.gz` can be unzipped by any tool that supports standard gzip
```
gunzip file.gz
```

## Architecture
PJzip uses a producer-consumer multithreading model. A producer will read in chunks from stdin and send them as block to a queue, where worker threads will consume the blocks and compress the data and send them to the output queue. The blocks are then written to stdout.