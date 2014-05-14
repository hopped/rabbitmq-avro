# rabbitmq-avro

> Demonstrating the use of Apache Avro for serialization in order
to interchange data between Perl and Java via [RabbitMQ][rabbitmq] in RPC mode. This code is based on the excellent [RPC tutorial][rpc] by RabbitMQ.


## Motivation
Since one hardly finds any examples of using [RabbitMQ][rabbitmq] in connection with [Apache Avro][avro] for Perl, I decided to write a small example of using RabbitMQ in [RPC mode][rpc] to interchange data serialized with Avro between Perl and Java.

This tutorial is part of a ''greater'' series using other data-interchange
formats such as [Google Protocol Buffers][protobuf] and [Apache Thrift][thrift].


## Prerequisites

> Please skip this section, if you've already installed [Gradle][gradle], [RabbitMQ][rabbitmq], Perl including [AnyEvent][anyevent], [Net::RabbitFoot][rabbitfoot], [DBD::Mock][mock], and [Apache Avro][perlavro] for Perl.

It should be noted, that the following instructions assume Mac OS X to be used as an operating system. The OS X version the installation is tested on is 10.9. Please adapt the commands to satisfy your needs, if needed.


### Gradle

Download [Gradle][gradle] via the following link

```bash
https://services.gradle.org/distributions/gradle-1.12-all.zip
```

unpack, and set the desired environment variable. Please replace {username} and {path-to-gradle}:

```bash
GRADLE_HOME=/Users/{username}/{path-to-gradle}/gradle-1.12
export GRADLE_HOME
export PATH=$PATH:$GRADLE_HOME/bin
```

### RabbitMQ

The easiest way to install RabbitMQ on Mac OS X is via __Homebrew__, the ''missing
package manager for OS X''. Open a terminal, and install [Homebrew][homebrew] as follows:

```bash
ruby -e "$(curl -fsSL https://raw.github.com/Homebrew/homebrew/go/install)"
```

Next, install RabbitMQ (currently v3.2.4), and add the path to your $PATH variable:

```bash
brew update
brew install rabbitmq
export PATH=$PATH:/usr/local/sbin
```

Enable the management plugin (optional):

```bash
rabbitmq-plugins enable rabbitmq_management
```

Start the server:

```bash
rabbitmq-server
```

You can now browse to http://localhost:15762 in order to monitor your running RabbitMQ instance (if you previously installed the management plugin).


### Perl

I advice to install Perl via __perlbrew__. If you don't have a running installation of [perlbrew][perlbrew], then just execute the following line in your command line:

```bash
\curl -L http://install.perlbrew.pl | bash
```

Next, install a current version of Perl. It should be noted, that 5.16.0 has a bug when compiling the Protobuf definitions for Perl. Hence, you might want to use another version, e.g. 5.18.2:

```bash
perlbrew install perl-5.18.2
perlbrew switch perl-5.18.2
```

Now, we need to install some dependencies (use cpan or cpanminus):

```bash
cpanm install --notest AnyEvent
cpanm install --notest Net::RabbitFoot
cpanm install --notest DBD::Mock
```

Please note, that there are some errors while running the tests for each of the packages. Thus, we have to use the ''no test'' option.


### Apache Avro for Perl

Download and install the Perl libraries in order to use Avro as follows:

```bash
git clone https://github.com/yannk/perl-avro/
cd perl-avro
perl Makefile.PL
make
sudo make install
```


## Installation

This section assumes that you've successfully installed RabbitMQ and the necessary Perl libraries.

First, clone the repository:

```bash
git clone git://github.com/hopped/rabbitmq-thrift.git
```

Next, you can build the project using the Gradle build file:

```bash
# Current directory is the project root
gradle build
```

## Run the example

Since I don't have written a suitable Gradle task yet, you have to execute the following commands to run the default client/server scenario (ideally you can run each command in its own shell):

```bash
# Current directory is the project root

# (1) Start the RabbitMQ Server
rabbitmq-server
# (2) Start the server written in Perl
cd src/main/perl
perl RPCServer.pl
# (3) Run the client written in Java
gradle run
```


## Data

What data was actually interchanged? For this example, I wrote some Avro schemas that might be used by a running website such as [Strava](http://www.strava.com) or [SmashRun](http://www.smashrun.com) in order to store runs for users. Let's have a look
at 'User.avsc' as an example. All files are kept in [src/main/resources](src/main/resources).

```avro
{
    "namespace": "com.hopped.runner.avro",
    "name": "User",
    "type": "record",
    "fields": [
        {
            "name": "nameOrAlias",
            "type": [ "null", "string" ],
            "default": null
        },
        {
            "name": "id",
            "type": [ "null", "int" ],
            "default": null
        },
        {
            "name": "birthdate",
            "type": [ "null", "int" ],
            "default": null
        },
        {
            "name": "totalDistanceMeters",
            "type": [ "null", "double" ],
            "default": null
        },
        {
            "name": "eMail",
            "type": [ "null", "string" ],
            "default": null
        },
        {
            "name": "firstName",
            "type": [ "null", "string" ],
            "default": null
        },
        {
            "name": "gender",
            "type": [ "null", "string" ],
            "default": null
        },
        {
            "name": "height",
            "type": [ "null", "int" ],
            "default": null
        },
        {
            "name": "lastName",
            "type": [ "null", "string" ],
            "default": null
        },
        {
            "name": "weight",
            "type": [ "null", "int" ],
            "default": null
        }
    ]
}
```

## Contributing
Find a bug? Have a feature request?
Please [create](https://github.com/hopped/rabbitmq-thrift/issues) an issue.


## Authors

**Dennis Hoppe**

+ [github/hopped](https://github.com/hopped)


## Release History

| Date        | Version | Comment          |
| ----------- | ------- | ---------------- |
| 2014-05-14  | 0.1.0   | Initial release. |


## TODO

- Test cases


## License
Copyright 2014 Dennis Hoppe.

[MIT License](LICENSE).


[anyevent]: http://search.cpan.org/dist/AnyEvent/
[avro]: http://avro.apache.org/
[gradle]: http://www.gradle.org/
[homebrew]: http://brew.sh/
[mock]: http://search.cpan.org/~dichi/DBD-Mock-1.45/lib/DBD/Mock.pm
[perlbrew]: http://perlbrew.pl/
[perlavro]: https://github.com/yannk/perl-avro/
[protobuf]: https://code.google.com/p/protobuf/
[rabbitmq]: http://www.rabbitmq.com
[rabbitfoot]: http://search.cpan.org/~ikuta/Net-RabbitFoot-1.03/lib/Net/RabbitFoot.pm
[rpc]: http://www.rabbitmq.com/tutorials/tutorial-six-java.html
[thrift]: http://thrift.apache.org/
