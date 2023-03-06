package com.enonic.app.api.handler.web;

import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.handler.WebHandlerHelper;
import com.enonic.xp.repository.RepositoryId;
import com.enonic.xp.web.HttpMethod;
import com.enonic.xp.web.WebException;
import com.enonic.xp.web.WebRequest;
import com.enonic.xp.web.WebResponse;
import com.enonic.xp.web.handler.WebHandler;
import com.enonic.xp.web.handler.WebHandlerChain;

@Component(immediate = true, service = WebHandler.class, configurationPid = "com.enonic.xp.portal")
public class AttachmentHandler
    extends EndpointHandler
{
    private static final Pattern PATTERN = Pattern.compile( "([^/]+)/([^/]+)/([^/]+)/([^/^:]+)(?::([^/]+))?/([^/]+)" );

    private final ContentService contentService;

    private volatile String privateCacheControlHeaderConfig;

    private volatile String publicCacheControlHeaderConfig;

    private volatile String contentSecurityPolicy;

    private volatile String contentSecurityPolicySvg;

    @Activate
    public AttachmentHandler( final @Reference ContentService contentService )
    {
        super( EnumSet.of( HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS ), "attachment" );

        this.contentService = contentService;
    }

    @Activate
    @Modified
    public void activate( final PortalConfig config )
    {
        privateCacheControlHeaderConfig = config.media_private_cacheControl();
        publicCacheControlHeaderConfig = config.media_public_cacheControl();
        contentSecurityPolicy = config.media_contentSecurityPolicy();
        contentSecurityPolicySvg = config.media_contentSecurityPolicy_svg();
    }

    @Override
    protected WebResponse doHandle( final WebRequest webRequest, final WebResponse webResponse, final WebHandlerChain webHandlerChain )
        throws Exception
    {
        WebHandlerHelper.checkAdminAccess( webRequest );

        final String restPath = findRestPath( webRequest );
        final Matcher matcher = PATTERN.matcher( restPath );

        if ( !matcher.find() )
        {
            throw WebException.notFound( "Not a valid attachment url pattern" );
        }

        final RepositoryId repo = RepositoryId.from( "com.enonic.cms." + matcher.group( 1 ) );
        final Branch branch = Branch.from( matcher.group( 2 ) );

        final AttachmentHandlerWorker worker = new AttachmentHandlerWorker( new PortalRequest( webRequest ), this.contentService );
        worker.download = "download".equals( matcher.group( 3 ) );
        worker.id = ContentId.from( matcher.group( 4 ) );
        worker.fingerprint = matcher.group( 5 );
        worker.name = matcher.group( 6 );
        worker.privateCacheControlHeaderConfig = this.privateCacheControlHeaderConfig;
        worker.publicCacheControlHeaderConfig = this.publicCacheControlHeaderConfig;
        worker.contentSecurityPolicy = this.contentSecurityPolicy;
        worker.contentSecurityPolicySvg = this.contentSecurityPolicySvg;

        return ContextBuilder.copyOf( ContextAccessor.current() ).repositoryId( repo ).branch( branch ).build().callWith( worker::execute );
    }
}
