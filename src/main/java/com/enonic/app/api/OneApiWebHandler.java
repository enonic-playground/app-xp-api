package com.enonic.app.api;

import java.util.EnumSet;
import java.util.regex.Pattern;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalResponse;
import com.enonic.xp.portal.controller.ControllerScript;
import com.enonic.xp.portal.controller.ControllerScriptFactory;
import com.enonic.xp.resource.ResourceKey;
import com.enonic.xp.web.HttpMethod;
import com.enonic.xp.web.WebRequest;
import com.enonic.xp.web.WebResponse;
import com.enonic.xp.web.handler.BaseWebHandler;
import com.enonic.xp.web.handler.WebHandler;
import com.enonic.xp.web.handler.WebHandlerChain;
import com.enonic.xp.web.websocket.WebSocketConfig;
import com.enonic.xp.web.websocket.WebSocketContext;
import com.enonic.xp.web.websocket.WebSocketEndpoint;

@Component(immediate = true, service = WebHandler.class)
public class OneApiWebHandler
    extends BaseWebHandler
{
    private static final Pattern URL_PATTERN = Pattern.compile( "^(/api)([/]*)$" );

    private static final ApplicationKey APPLICATION_KEY = ApplicationKey.from( "com.enonic.app.api" );

    private final ControllerScriptFactory controllerScriptFactory;

    @Activate
    public OneApiWebHandler( final @Reference ControllerScriptFactory controllerScriptFactory )
    {
        super( -49, EnumSet.of( HttpMethod.GET, HttpMethod.POST, HttpMethod.OPTIONS ) );
        this.controllerScriptFactory = controllerScriptFactory;
    }

    @Override
    protected boolean canHandle( final WebRequest webRequest )
    {
        final String path = webRequest.getRawPath();
        return URL_PATTERN.matcher( path ).matches();
    }

    @Override
    public WebResponse handle( final WebRequest webRequest, final WebResponse webResponse, final WebHandlerChain webHandlerChain )
        throws Exception
    {
        final PortalResponse.Builder builder = PortalResponse.create( super.handle( webRequest, webResponse, webHandlerChain ) );
        builder.header( "Access-Control-Allow-Origin", "*" );
        builder.header( "Access-Control-Allow-Methods", "POST, OPTIONS" );
        builder.header( "Access-Control-Allow-Headers", "Content-Type" );
        return builder.build();
    }

    @Override
    protected WebResponse doHandle( final WebRequest webRequest, final WebResponse webResponse, final WebHandlerChain webHandlerChain )
        throws Exception
    {
        final PortalRequest portalRequest = castToPortalRequest( webRequest );
        portalRequest.setContextPath( portalRequest.getBaseUri() );
        portalRequest.setApplicationKey( APPLICATION_KEY );

        if ( webRequest.getMethod() == HttpMethod.GET && !webRequest.isWebSocket() )
        {
            final ResourceKey script = ResourceKey.from( APPLICATION_KEY, "headless/headless.js" );
            final ControllerScript controllerScript = controllerScriptFactory.fromScript( script );
            return controllerScript.execute( portalRequest );
        }
        else
        {
            final ResourceKey script = ResourceKey.from( APPLICATION_KEY, "graphql/graphql.js" );
            final ControllerScript controllerScript = controllerScriptFactory.fromScript( script );

            return createContext( portalRequest ).callWith( () -> {
                final PortalResponse portalResponse = controllerScript.execute( portalRequest );
                final WebSocketConfig webSocketConfig = portalResponse.getWebSocket();
                final WebSocketContext webSocketContext = portalRequest.getWebSocketContext();
                if ( webSocketContext != null && webSocketConfig != null )
                {
                    WebSocketEndpoint webSocketEndpoint = new WebSocketEndpointImpl( webSocketConfig, () -> controllerScript );
                    webSocketContext.apply( webSocketEndpoint );
                }
                return portalResponse;
            } );
        }
    }

    protected PortalRequest castToPortalRequest( final WebRequest webRequest )
    {
        PortalRequest portalRequest;
        if ( webRequest instanceof PortalRequest )
        {
            portalRequest = (PortalRequest) webRequest;
        }
        else
        {
            portalRequest = new PortalRequest( webRequest );
        }
        return portalRequest;
    }

    private static Context createContext( final PortalRequest portalRequest )
    {
        return ContextBuilder.from( ContextAccessor.current() ).repositoryId( portalRequest.getRepositoryId() ).branch(
            portalRequest.getBranch() ).build();
    }
}
