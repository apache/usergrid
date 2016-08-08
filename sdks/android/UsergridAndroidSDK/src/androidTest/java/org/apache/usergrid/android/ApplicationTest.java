package org.apache.usergrid.android;

import android.app.Application;
import android.test.ApplicationTestCase;

import org.apache.usergrid.android.callbacks.UsergridResponseCallback;

import org.apache.usergrid.java.client.Usergrid;
import org.apache.usergrid.java.client.model.UsergridEntity;
import org.apache.usergrid.java.client.response.UsergridResponse;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CountDownLatch;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {

    Book finishedBook;
    String newBookTitle = "A new title again at time: " + System.currentTimeMillis();

    public ApplicationTest() {
        super(Application.class);
    }

    @Override
    protected void setUp() throws Exception {
        Usergrid.initSharedInstance("rwalsh","sandbox");
        UsergridEntity.mapCustomSubclassToType("book",Book.class);
    }

    @Override
    protected void tearDown() throws Exception {
        Usergrid.reset();
    }

    public void testGET() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);

        Usergrid.initSharedInstance("rwalsh","sandbox");
        UsergridAsync.GET("books", new UsergridResponseCallback() {
            @Override
            public void onResponse(@NotNull UsergridResponse response) {
                if (response.ok()) {
                    final Book book = (Book) response.first();
                    assertNotNull(book);
                    assertNotNull(book.getUuid());
                    book.setTitle(newBookTitle);
                    UsergridEntityAsync.save(book, new UsergridResponseCallback() {
                        @Override
                        public void onResponse(@NotNull UsergridResponse response) {
                            final Book book = (Book) response.first();
                            assertNotNull(book);
                            assertNotNull(book.getUuid());
                            assertEquals(book.getTitle(),newBookTitle);
                            UsergridAsync.GET("book", book.getUuid(), new UsergridResponseCallback() {
                                @Override
                                public void onResponse(@NotNull UsergridResponse response) {
                                    assertNotNull(response.getEntities());
                                    assertNotNull(response.first());
                                    finishedBook = (Book) response.first();
                                    signal.countDown();
                                }
                            });
                        }
                    });
                }
            }
        });
        signal.await();
        assertNotNull(finishedBook);
        assertEquals(finishedBook.getTitle(),newBookTitle);
    }
}