package com.enonic.app.api.url;

import java.util.concurrent.Callable;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.resource.ResourceService;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.auth.AuthenticationInfo;

@Component(immediate = true)
public class UrlServiceImpl
    implements UrlService
{
    private final ResourceService resourceService;

    @Activate
    public UrlServiceImpl( final @Reference ResourceService resourceService )
    {
        this.resourceService = resourceService;
    }

    @Override
    public String assetUrl( final AssetUrlParams urlParams )
    {
        return runWithAdminRole( () -> new AssetUrlBuilder( urlParams, resourceService ).buildUrl() );
    }

    private <T> T runWithAdminRole( final Callable<T> callable )
    {
        final Context context = ContextAccessor.current();
        final AuthenticationInfo authenticationInfo =
            AuthenticationInfo.copyOf( context.getAuthInfo() ).principals( RoleKeys.ADMIN ).build();
        return ContextBuilder.from( context ).authInfo( authenticationInfo ).build().callWith( callable );
    }
}
