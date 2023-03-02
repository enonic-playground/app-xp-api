package com.enonic.app.api.handler.web;

import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.image.ImageService;
import com.enonic.xp.image.ScaleParamsParser;
import com.enonic.xp.media.MediaInfoService;
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
public class ImageHandler
    extends EndpointHandler
{
    private static final Pattern PATTERN = Pattern.compile( "([^/]+)/([^/]+)/([^/^:]+)(?::([^/]+))?/([^/]+)/([^/]+)" );

    private final ContentService contentService;

    private final ImageService imageService;

    private final MediaInfoService mediaInfoService;

    @Activate
    public ImageHandler( final @Reference ContentService contentService, final @Reference ImageService imageService,
                         final @Reference MediaInfoService mediaInfoService )
    {
        super( EnumSet.of( HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS ), "image" );
        this.contentService = contentService;
        this.imageService = imageService;
        this.mediaInfoService = mediaInfoService;
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
            throw WebException.notFound( "Not a valid image url pattern" );
        }

        final RepositoryId repo = RepositoryId.from( "com.enonic.cms." + matcher.group( 1 ) );
        final Branch branch = Branch.from( matcher.group( 2 ) );

        final PortalRequest request = new PortalRequest( webRequest );

        final ImageHandlerWorker worker = new ImageHandlerWorker( request, this.contentService, this.imageService, this.mediaInfoService );
        worker.id = ContentId.from( matcher.group( 3 ) );
        worker.fingerprint = matcher.group( 4 );
        worker.scaleParams = new ScaleParamsParser().parse( matcher.group( 5 ) );
        worker.name = matcher.group( 6 );

        return ContextBuilder.copyOf( ContextAccessor.current() ).repositoryId( repo ).branch( branch ).build().callWith( worker::execute );
    }
}
