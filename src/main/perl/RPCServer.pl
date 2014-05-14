=head
Copyright (c) 2014 Dennis Hoppe
www.dennis-hoppe.com

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
=cut

use strict;
use warnings;

use AnyEvent;
use DBI;
use Net::RabbitFoot;

use RunnerDB;

use Avro::Schema;
use Avro::BinaryEncoder;
use Avro::BinaryDecoder;
use IO::String;

use Data::Dumper;

# (1) Initialize the connection to RabbitMQ using the 'running_queue'
my $Connection = Net::RabbitFoot->new()->load_xml_spec()->connect(
    host  => 'localhost',
    port  => 5672,
    user  => 'guest',
    pass  => 'guest',
    vhost => '/',
);
my $Channel = $Connection->open_channel();
$Channel->declare_queue(queue => 'running_queue');

# (2) Handle incoming requests
#     - Note, that we don't perform any error handling for simplicity's sake
sub serve {
    print " [>] Incoming request ...\n";
    my $Data = shift;
    my $Payload = $Data->{body}->{payload};
    my $Properties = $Data->{header};

    # (a) Map the incoming request to the required RunRequest
    open FILEHANDLE, '../resources/RunRequest.avsc' or die $!;
    my $RunRequest = do { local $/; <FILEHANDLE> };
    close(FILEHANDLE);

    my $AvroSchema = Avro::Schema->parse($RunRequest);
    my $Reader = IO::String->new($Payload);
    my $Request = Avro::BinaryDecoder->decode(
        writer_schema => $AvroSchema,
        reader_schema => $AvroSchema,
        reader => $Reader,
    );
    my $UserId = $Request->{UserRef}->{id};

    # (b) Configure a (mock) database to retrieve runs done by $User
    my $dbh = DBI->connect('dbi:Mock:', '', '');
    my $Statement = <<"END_SQL";
SELECT r.alias, r.id, r.distanceMeters
FROM Run as r
WHERE r.userId = 1;
END_SQL

    my @ResultSet = (
        ['alias', 'id', 'distanceMeters'],
        ['Hamburg Run', 1, 3200],
        ['Alster Run', 2, 4600],
    );
    $dbh->{mock_add_resultset} = \@ResultSet;

    # (c) Perform database call to get a list of runs
    print " [-] Retrieve runs ...\n";
    my $Database = new RunnerDB($dbh);
    my $RunsFromDB = $Database->getRunsByUserId($UserId);

    # (d) Iterate over result set while building the response
    my @Runs;
    foreach my $Run (@{$RunsFromDB}) {
        my %RunAvro = (
            nameOrAlias => $Run->{'Alias'},
            distanceMeters => $Run->{'Distance'},
        );
        push @Runs, \%RunAvro;
    }
    my %Response = (
        runs => \@Runs,
    );

    # (e) Serialize the response object (binary format)
    $Payload = undef;
    open FILEHANDLE, '../resources/Run.avsc' or die $!;
    my $Run = do { local $/; <FILEHANDLE> };
    close(FILEHANDLE);

    my $AAvroSchema = Avro::Schema->parse($Run);
    Avro::BinaryEncoder->encode(
        schema => $AAvroSchema,
        data => \%Response,
        emit_cb => sub { $Payload .= ${ $_[0] } },
    );

    # (f) Publish the result to the queue
    print ' [<] Send response containing ' . @{$Response{runs}} . " runs \n";

    $Channel->publish(
        exchange => '',
        routing_key => $Properties->{reply_to},
        header => {
            correlation_id => $Properties->{correlation_id},
        },
        body => $Payload,
    );

    $Channel->ack();
}

$Channel->qos(prefetch_count => 1);
$Channel->consume(
    on_consume => \&serve,
);

print " [x] Awaiting RPC requests\n";

# Let's wait for requests ...
AnyEvent->condvar->recv;

1;