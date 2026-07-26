package mchorse.bbs_mod.l10n.keys;

import mchorse.bbs_mod.BBSMod;

public class FormatKey implements IKey
{
    public IKey lang;
    public Object[] args;

    public FormatKey(IKey lang, Object... args)
    {
        this.lang = lang;
        this.args = args;
    }

    @Override
    public String get()
    {
        String format = this.lang.get();

        try
        {
            return this.args.length == 0 ? format : String.format(format, this.args);
        }
        catch (Exception e)
        {
            String key = this.lang instanceof LangKey ? ((LangKey) this.lang).key : this.lang.get();
            BBSMod.LOGGER.info("Failed to format string: " + key);
            BBSMod.LOGGER.error("Exception", e);

            return key;
        }
    }

    @Override
    public String toString()
    {
        return this.get();
    }
}