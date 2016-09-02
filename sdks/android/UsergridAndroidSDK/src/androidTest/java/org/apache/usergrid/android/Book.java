package org.apache.usergrid.android;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.usergrid.java.client.model.UsergridEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by Robert Walsh on 4/12/16.
 */
public class Book extends UsergridEntity {
    @Nullable private String title;

    public Book(@JsonProperty("type") @NotNull String type) {
        super(type);
    }

    public void setTitle(@NotNull final String title) {
        this.title = title;
    }
    public String getTitle() {
        return this.title;
    }

}
