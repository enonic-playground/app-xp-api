package com.enonic.app.api;

import graphql.schema.GraphQLSchema;

public interface SchemaProvider
{
    GraphQLSchema getSchema();
}
