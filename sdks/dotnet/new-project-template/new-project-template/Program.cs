// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


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
            var client = new Usergrid.Sdk.Client( <org name>, "sandbox");
            
            //Uncomment this line if you are not using your App Services 'sandbox' application
            //client.Login(<login id>, <password>, Usergrid.Sdk.Model.AuthType.User);

            //
            // Print out the books from the books collection
            //

            Console.WriteLine("Book Number\tBook Title");
            Console.WriteLine("===========\t==========");
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
