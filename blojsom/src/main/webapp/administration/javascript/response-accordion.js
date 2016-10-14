(function(Y) {
    var E = Y.util.Event,
        Dom = Y.util.Dom,
        S = Y.util.Selector,
        Anim = Y.util.Anim;
        
        
        var pageInit = function() {
            response_metas = S.query("blojsom-accordion-content-meta::before");
            E.on(response_metas, 'click', function() {
                console.log("click!");
            });
            
            console.log(response_metas);
        };
        
        E.onDOMReady(pageInit);

        
})(YAHOO);