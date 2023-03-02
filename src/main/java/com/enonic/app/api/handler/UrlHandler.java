package com.enonic.app.api.handler;

import java.util.function.Supplier;

import com.enonic.app.api.url.AssetUrlParams;
import com.enonic.app.api.url.UrlService;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;

public class UrlHandler
    implements ScriptBean
{
    private Supplier<PortalRequest> portalRequestSupplier;

    private Supplier<UrlService> urlServiceSupplier;

    @Override
    public void initialize( final BeanContext context )
    {
        this.portalRequestSupplier = context.getBinding( PortalRequest.class );
        this.urlServiceSupplier = context.getService( UrlService.class );
    }

    public String assetUrl( String path, String type )
    {
        final AssetUrlParams params = new AssetUrlParams();
        params.setPortalRequest( portalRequestSupplier.get() );
        params.setPath( path );
        params.setType( type );
        return urlServiceSupplier.get().assetUrl( params );
    }
}
