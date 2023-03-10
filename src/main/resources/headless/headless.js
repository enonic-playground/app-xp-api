const mustache = require('/lib/mustache');
const portalLib = require('/lib/xp/portal');
const urlLib = require('/lib/urls');

exports.get = function (req) {
    const view = resolve('headless.html');
    const assetsUrl = urlLib.assetUrl("/api", "server");
    // const assetsUrl = portalLib.assetUrl({path: "/"});
    const baseUrl = '/api';
    const wsUrl = portalLib.url({
        path: baseUrl,
        type: 'websocket',
    });
    const handlerUrl = portalLib.url({
        path: baseUrl,
    });

    const params = {
        assetsUrl: assetsUrl,
        wsUrl: wsUrl,
        handlerUrl: handlerUrl,
    };

    return {
        contentType: 'text/html',
        body: mustache.render(view, params),
    };
}

