package com.enonic.app.api;

import java.util.function.Supplier;

import graphql.schema.GraphQLSchema;

import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;

public class OneApiScriptBean
    implements ScriptBean
{
    private Supplier<SchemaProvider> schemaProviderSupplier;

    @Override
    public void initialize( final BeanContext context )
    {
        this.schemaProviderSupplier = context.getService( SchemaProvider.class );
    }

    public GraphQLSchema getGraphQLSchema()
    {
        return schemaProviderSupplier.get().getSchema();
    }
}
