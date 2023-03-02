package com.enonic.app.api;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.Scalars;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;

import com.enonic.xp.app.Application;
import com.enonic.xp.app.ApplicationService;
import com.enonic.xp.portal.script.PortalScriptService;
import com.enonic.xp.resource.ResourceKey;
import com.enonic.xp.resource.ResourceService;
import com.enonic.xp.script.ScriptExports;
import com.enonic.xp.script.ScriptValue;

@Component(immediate = true, service = SchemaProvider.class)
public class SchemaProviderImpl
    implements SchemaProvider
{
    private static final Logger LOG = LoggerFactory.getLogger( SchemaProviderImpl.class );

    private static final String METHOD_NAME = "getGraphQLSchema";

    private static final String SCRIPT_PATH = "api/api.js";

    private final ApplicationService applicationService;

    private final PortalScriptService scriptService;

    private final ResourceService resourceService;

    @Activate
    public SchemaProviderImpl( final @Reference ApplicationService applicationService, final @Reference PortalScriptService scriptService,
                               final @Reference ResourceService resourceService )
    {
        this.applicationService = applicationService;
        this.scriptService = scriptService;
        this.resourceService = resourceService;
    }

    @Override
    public GraphQLSchema getSchema()
    {
        GraphQLObjectType.Builder queryFieldBuilder = GraphQLObjectType.newObject().name( "Query" ).description( "Query" );

        GraphQLCodeRegistry.Builder codeRegisterBuilder = GraphQLCodeRegistry.newCodeRegistry();

        Set<GraphQLType> additionalTypes = new LinkedHashSet<>();

        getGraphQLSchemasFromApplications().forEach( externalSchema -> {
            queryFieldBuilder.fields( externalSchema.getQueryType().getFieldDefinitions() );
            codeRegisterBuilder.dataFetchers( externalSchema.getCodeRegistry() );
            codeRegisterBuilder.typeResolvers( externalSchema.getCodeRegistry() );
            additionalTypes.addAll( externalSchema.getAdditionalTypes() );
        } );

        queryFieldBuilder.field( GraphQLFieldDefinition.newFieldDefinition().
            name( "serverTime" ).
            description( "Returns current time on the server" ).
            type( Scalars.GraphQLString ).
            build()
        );

        codeRegisterBuilder.dataFetcher( FieldCoordinates.coordinates( "Query", "serverTime" ),
                                         (DataFetcher<String>) environment -> Instant.now().toString() );

        return GraphQLSchema.newSchema().query( queryFieldBuilder.build() ).codeRegistry( codeRegisterBuilder.build() ).additionalTypes(
            additionalTypes ).build();
    }

    private List<GraphQLSchema> getGraphQLSchemasFromApplications()
    {
        return applicationService.getInstalledApplications().stream().map( this::getGraphQLSchema ).filter( Objects::nonNull ).collect(
            Collectors.toList() );
    }

    private GraphQLSchema getGraphQLSchema( Application application )
    {
        ResourceKey resourceKey = ResourceKey.from( application.getKey(), SCRIPT_PATH );
        if ( resourceService.getResource( resourceKey ).exists() )
        {
            ScriptExports exports = scriptService.execute( resourceKey );
            if ( exports.hasMethod( METHOD_NAME ) )
            {
                try
                {
                    ScriptValue scriptValue = exports.executeMethod( METHOD_NAME );
                    return (GraphQLSchema) scriptValue.getValue();
                }
                catch ( Exception e )
                {
                    LOG.warn( "Schema can not be extracted from {}", resourceKey, e );
                }
            }
        }
        return null;
    }
}
