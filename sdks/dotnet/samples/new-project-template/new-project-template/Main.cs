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
        public string author { get; set; }
    }

    class Program
    {
        static void Main(string[] args)
        {
            var client = new Usergrid.Sdk.Client(<org name>, <app name>);
            client.Login(<login id>, <password>, Usergrid.Sdk.Model.AuthType.User);

            //
            // Print out the books from the books collection
            //

            Console.WriteLine("Book Number\tBook Title\t\t\tBookAuthor");
            Console.WriteLine("===========\t==========\t\t\t==========");
            var books = client.GetEntities<Book>("books");
            int j = 1;
            do
            {
                for (int i = 0; i < books.Count; i++)
                {
                    Book b = books[i];
                    Console.WriteLine(j + "\t\t" + b.title + "\t\t\t" + b.author);
                    j++;
                }
                books = client.GetNextEntities<Book>("books");
            } while (books.Count > 0);

            //
            // Create a new book and add it to the database
            //
            Book newBook = new Book();
            newBook.title = "A Sample Title";
            newBook.author = "A Sample Author";
            client.CreateEntity<Book>("books", newBook);

            //
            // Change/Update Entities
            //
            books = client.GetEntities<Book>("books", 10, "where title contains 'Sample'");
            for (int k = 0; k < books.Count; k++)
            {
                Book b = books[k];
                Book updatedBook = new Book();
                updatedBook.title = "Another Title";
                updatedBook.author = "Another Author";
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
