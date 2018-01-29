package com.amazon.fusion;

import static com.amazon.fusion.FusionString.checkRequiredStringArg;

final class JavaClassProc
    extends Procedure1
{
    @Override
    Object doApply(Evaluator eval, Object arg)
        throws FusionException
    {
        String className = checkRequiredStringArg(eval, this, 0, arg);

        return determineClass(className);
    }

    private Class<?> determineClass(String className)
        throws FusionException
    {
        try
        {
            return Class.forName(className);
        }
        catch (ClassNotFoundException e)
        {
            throw contractFailure("Java class isn't found: " + className, e);
        }
    }
}
