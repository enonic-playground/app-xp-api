package com.enonic.app.api;

import com.enonic.xp.script.ScriptValue;

public class Synchronizer
{
    public static synchronized void sync( final ScriptValue callbackScriptValue )
    {
        callbackScriptValue.call();
    }
}