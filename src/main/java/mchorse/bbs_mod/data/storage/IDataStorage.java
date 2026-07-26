package mchorse.bbs_mod.data.storage;

import mchorse.bbs_mod.data.types.BaseType;

import java.io.IOException;
import mchorse.bbs_mod.BBSMod;

public interface IDataStorage
{
    public BaseType read() throws IOException;

    public default BaseType readSilently()
    {
        try
        {
            return this.read();
        }
        catch (IOException e)
        {
            BBSMod.LOGGER.error("Exception", e);
        }

        return null;
    }

    public void write(BaseType type) throws IOException;

    public default boolean writeSilently(BaseType type)
    {
        try
        {
            this.write(type);

            return true;
        }
        catch (IOException e)
        {
            BBSMod.LOGGER.error("Exception", e);
        }

        return false;
    }
}