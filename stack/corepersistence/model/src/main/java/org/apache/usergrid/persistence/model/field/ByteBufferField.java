package org.apache.usergrid.persistence.model.field;


import java.nio.ByteBuffer;


/** A field for storing byte buffers */
public class ByteBufferField extends AbstractField<ByteBuffer>
{


    /** Creates an immutable copy of the byte buffer */
    public ByteBufferField( String name, ByteBuffer value )
    {
        //always return a duplicate so we don't mess with the markers
        super( name, value.duplicate() );
    }

    public ByteBufferField() {

        }

    @Override
    public ByteBuffer getValue()
    {
        //always return a duplicate so we don't mess with the markers
        return value.duplicate();
    }
}
