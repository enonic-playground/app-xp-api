exports.getInstalledApplications = function () {
    const bean = __.newBean('com.enonic.app.api.ApplicationHandler');
    return __.toNativeObject(bean.getInstalledApplicationKeys());
}
