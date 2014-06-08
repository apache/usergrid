#!/usr/bin/perl
use Usergrid::Client;

# Create a client object for Usergrid that's used for subsequent activity
my $client = Usergrid::Client->new(
  organization => 'test-organization',
  application  => 'test-app',
  api_url      => 'http://localhost:8080',
  trace        => 0
);

# Logs the user in. The security token is maintained by the library in memory
$client->login('johndoe', 'Johndoe123$');

# Add two entities to the "books" collection
$client->add_entity("books", { name => "Ulysses", author => "James Joyce" });
$client->add_entity("books", { name => "Neuromancer", author => "William Gibson" });

# Retrieve a handle to the collection
my $books = $client->get_collection("books");

# Add a new attribute for quantity in stock
while ($books->has_next_entity()) {
  my $book = $books->get_next_entity();

  print "Name: "   . $book->get('name')   . ", ";
  print "Author: " . $book->get('author') . "\n";

  # Create a new attribute and update the entity
  $book->set("in-stock", 0);
  $client->update_entity($book);
}
