using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace new_project_template
{
    class Book : Usergrid.Sdk.Model.UsergridEntity
    {
        public string title { get; set; }
    }

    class Program
    {
        static void Main(string[] args)
        {
            //Start by replacing <org name> with your App Services organization name!
            var client = new Usergrid.Sdk.Client(<org name>, "sandbox");
            
            //Uncomment this line if you are not using your App Services 'sandbox' application
            //client.Login(<login id>, <password>, Usergrid.Sdk.Model.AuthType.User);

            //
            // Print out the books from the books collection
            //

            Console.WriteLine("Book Number\tBook Title");
            Console.WriteLine("===========\t==========\t\t\t==========");
            var books = client.GetEntities<Book>("books");
            int j = 1;
            do
            {
                for (int i = 0; i < books.Count; i++)
                {
                    Book b = books[i];
                    Console.WriteLine(j + "\t\t" + b.title);
                    j++;
                }
                books = client.GetNextEntities<Book>("books");
            } while (books.Count > 0);

            //
            // Create a new book and add it to the database
            //
            Book newBook = new Book();
            newBook.title = "The Old Man and the Sea";
            client.CreateEntity<Book>("books", newBook);

            //
            // Change/Update Entities
            //
            books = client.GetEntities<Book>("books", 10, null);
            for (int k = 0; k < books.Count; k++)
            {
                Book b = books[k];
                Book updatedBook = new Book();
                updatedBook.title = "Another Title";
                client.UpdateEntity<Book>("books", b.Uuid, updatedBook);
            }

            //
            // Delete Entities
            //
            books = client.GetEntities<Book>("books", 10, "where title contains 'Another'");
            for (int k = 0; k < books.Count; k++)
            {
                Book b = books[k];
                client.DeleteEntity("books", b.Uuid);
            }

            Console.Read();
        }
    }
}
