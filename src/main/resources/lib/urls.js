exports.assetUrl = function (path, type) {
    const bean = __.newBean('com.enonic.app.api.handler.UrlHandler');
    return __.toNativeObject(bean.assetUrl(path, type));
}
